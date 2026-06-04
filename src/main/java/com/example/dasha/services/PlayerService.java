package com.example.dasha.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PlayerService {

    private static final Logger log = Logger.getLogger(PlayerService.class.getName());

    private static final int BUFFER_SIZE = 4096;

    private final MinioService minioService;

    @Value("${icecast.host}")
    private String icecastHost;

    @Value("${icecast.port}")
    private int icecastPort;

    @Value("${icecast.mount}")
    private String icecastMount;

    @Value("${icecast.source.password}")
    private String icecastPassword;

    private final AtomicBoolean streaming = new AtomicBoolean(false);
    private Thread streamThread;

    public PlayerService(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * Запускает стрим песни из MinIO через ffmpeg в Icecast.
     *
     * @param fileKey путь к файлу в MinIO (например: songs/Title – Artist.mp3)
     */
    public void startStream(String fileKey) {
        if (streaming.getAndSet(true)) {
            stopStream();
        }

        streamThread = Thread.ofVirtual().start(() -> {
            try {
                InputStream songStream = minioService.getSongStream(fileKey);
                pushToIcecast(songStream, fileKey);
            } finally {
                streaming.set(false);
            }
        });
    }

    /**
     * Запускает непрерывный плейлист: все песни из MinIO по кругу.
     */
    public void startPlaylist() {
        if (streaming.getAndSet(true)) {
            stopStream();
        }

        streamThread = Thread.ofVirtual().start(() -> {
            while (streaming.get() && !Thread.currentThread().isInterrupted()) {
                List<com.example.dasha.models.Song> songs = minioService.listSongs();
                if (songs.isEmpty()) {
                    log.warning("Playlist is empty, stopping.");
                    break;
                }
                for (com.example.dasha.models.Song song : songs) {
                    if (!streaming.get() || Thread.currentThread().isInterrupted()) break;
                    log.info("Playing next: " + song.getFileKey());
                    InputStream songStream = minioService.getSongStream(song.getFileKey());
                    pushToIcecast(songStream, song.getFileKey());
                }
            }
            streaming.set(false);
        });
    }

    public void stopStream() {
        streaming.set(false);
        if (streamThread != null) {
            streamThread.interrupt();
        }
    }

    public boolean isStreaming() {
        return streaming.get();
    }

    // -------------------------------------------------------------------------

    private void pushToIcecast(InputStream songStream, String fileKey) {
        Process ffmpeg = null;
        java.net.Socket socket = null;

        try {
            // 1. Запускаем ffmpeg
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-re",
                    "-i", "pipe:0",
                    "-vn",
                    "-acodec", "libmp3lame",
                    "-ab", "192k",
                    "-ar", "44100",
                    "-f", "mp3",
                    "pipe:1"
            );
            pb.redirectErrorStream(false);
            ffmpeg = pb.start();

            // 2. Raw socket к Icecast — HttpURLConnection буферизует и не подходит для стриминга
            socket = new java.net.Socket(icecastHost, icecastPort);
            socket.setSoTimeout(0);
            OutputStream socketOut = socket.getOutputStream();

            String headers = "PUT /" + icecastMount + " HTTP/1.0\r\n"
                    + "Authorization: " + basicAuth("source", icecastPassword) + "\r\n"
                    + "Content-Type: audio/mpeg\r\n"
                    + "Ice-Name: " + fileKey + "\r\n"
                    + "\r\n";
            socketOut.write(headers.getBytes());
            socketOut.flush();

            OutputStream ffmpegIn  = ffmpeg.getOutputStream();
            InputStream  ffmpegOut = ffmpeg.getInputStream();
            InputStream  ffmpegErr = ffmpeg.getErrorStream();

            // 4. MinIO → ffmpeg stdin
            Thread feeder = Thread.ofVirtual().start(() -> pipe(songStream, ffmpegIn, true));

            // 5. ffmpeg stdout → Icecast socket
            pipe(ffmpegOut, socketOut, false);

            feeder.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Stream interrupted for: " + fileKey);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Streaming error for: " + fileKey, e);
        } finally {
            if (ffmpeg != null) ffmpeg.destroyForcibly();
            if (socket != null) try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void pipe(InputStream in, OutputStream out, boolean closeOut) {
        byte[] buf = new byte[BUFFER_SIZE];
        try {
            int len;
            while (!Thread.currentThread().isInterrupted() && (len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                out.flush();
            }
        } catch (IOException e) {
            // поток закрыт — нормальное завершение
        } finally {
            if (closeOut) {
                try { out.close(); } catch (IOException ignored) {}
            }
        }
    }

    private String basicAuth(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes());
    }
}
