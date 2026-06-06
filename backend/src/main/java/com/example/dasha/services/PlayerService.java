package com.example.dasha.services;

import com.example.dasha.models.Song;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Comparator;
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
    @Getter
    private volatile Song currentSong;
    private volatile long streamStartMillis = 0;

    public PlayerService(MinioService minioService) {
        this.minioService = minioService;
    }


    public void startPlaylist() {
        if (streaming.getAndSet(true)) {
            stopStream();
        }

        streamThread = Thread.ofVirtual().start(() -> {
            String lastPlayedKey = null;
            while (streaming.get() && !Thread.currentThread().isInterrupted()) {
                List<Song> songs = minioService.listSongs();
                songs.sort(Comparator.comparing(Song::getFileKey));
                if (songs.isEmpty()) {
                    log.warning("Playlist is empty, stopping.");
                    break;
                }

                int nextIndex = 0;
                if (lastPlayedKey != null) {
                    for (int i = 0; i < songs.size(); i++) {
                        if (songs.get(i).getFileKey().equals(lastPlayedKey)) {
                            nextIndex = (i + 1) % songs.size();
                            break;
                        }
                    }
                }

                Song song = songs.get(nextIndex);
                lastPlayedKey = song.getFileKey();

                if (!streaming.get() || Thread.currentThread().isInterrupted()) break;
                log.info("Playing next: " + song.getFileKey());
                currentSong = song;
                streamStartMillis = System.currentTimeMillis();
                try {
                    InputStream songStream = minioService.getSongStream(song.getFileKey());
                    pushToIcecast(songStream, song.getFileKey());
                } catch (RuntimeException e) {
                    log.warning("Skipping unavailable track: " + song.getFileKey());
                }
            }
            streaming.set(false);
        });
    }

    public void stopStream() {
        streaming.set(false);
        currentSong = null;
        if (streamThread != null) {
            streamThread.interrupt();
        }
    }

    public boolean isStreaming() {
        return streaming.get();
    }

    public double getPositionSeconds() {
        if (!streaming.get() || streamStartMillis == 0) return 0;
        return (System.currentTimeMillis() - streamStartMillis) / 1000.0;
    }

    private void pushToIcecast(InputStream songStream, String fileKey) {
        Process ffmpeg = null;
        java.net.Socket socket = null;

        try {
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

            // raw socket — HttpURLConnection буферизует и не подходит для стриминга
            socket = new Socket(icecastHost, icecastPort);
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
            ffmpeg.getErrorStream();

            Thread feeder = Thread.ofVirtual().start(() -> pipe(songStream, ffmpegIn, true));
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
