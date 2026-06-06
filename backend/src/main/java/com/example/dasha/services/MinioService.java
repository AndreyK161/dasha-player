package com.example.dasha.services;

import com.example.dasha.models.Song;
import io.minio.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MinioService {

    private static final Logger log = Logger.getLogger(MinioService.class.getName());
    private static final String SONGS_PREFIX = "songs/";

    private final MinioClient minioClient;
    private final String bucketName;
    private final ConcurrentHashMap<String, Double> durationCache = new ConcurrentHashMap<>();

    public MinioService(MinioClient minioClient,
                        @Value("${minio.bucket.name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public List<Song> listSongs() {
        List<Song> songs = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(SONGS_PREFIX)
                        .recursive(false)
                        .build()
        );

        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                String objectName = item.objectName();

                if (!objectName.endsWith(".mp3")) {
                    continue;
                }

                String filename = objectName.substring(SONGS_PREFIX.length());
                filename = filename.replace(".mp3", "");

                String[] parts = filename.split(" – ", 2);
                String title = parts[0].trim();
                String artist = parts.length > 1 ? parts[1].trim() : "Unknown";

                Double duration = durationCache.computeIfAbsent(objectName, this::fetchDuration);

                songs.add(new Song(title, artist, duration, objectName));
            } catch (Exception e) {
                log.log(Level.WARNING, "Skipping song due to error", e);
            }
        }

        return songs;
    }

    private Double fetchDuration(String fileKey) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("song-", ".mp3");
            try (InputStream stream = getSongStream(fileKey)) {
                Files.copy(stream, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    "-i", tmp.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            return (output.isEmpty() || output.equals("N/A")) ? null : Double.parseDouble(output);
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not fetch duration for: " + fileKey, e);
            return null;
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    private boolean isMp3(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[3];
            if (is.read(header) < 3) return false;
            // ID3 tag
            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') return true;
            // MPEG sync word (0xFF 0xFx)
            return (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0;
        } catch (Exception e) {
            return false;
        }
    }

    public String uploadSong(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Only .mp3 files are allowed");
        }
        if (!isMp3(file)) {
            throw new IllegalArgumentException("File content is not a valid MP3");
        }
        String objectName = SONGS_PREFIX + originalName;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1L)
                            .contentType("audio/mpeg")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload song to MinIO", e);
        }
        return objectName;
    }

    public void deleteSong(String filename) {
        String objectName = SONGS_PREFIX + filename;
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new java.util.NoSuchElementException("Song not found: " + filename);
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
            durationCache.remove(objectName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete song from MinIO", e);
        }
    }

    public InputStream getSongStream(String fileKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch song: " + fileKey, e);
        }
    }
}
