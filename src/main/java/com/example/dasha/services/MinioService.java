package com.example.dasha.services;

import com.example.dasha.models.Song;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MinioService {

    private static final String SONGS_PREFIX = "songs/";

    private final MinioClient minioClient;
    private final String bucketName;

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

                Song song = new Song(title, artist, null, objectName);
                song.setGuid(UUID.randomUUID());
                songs.add(song);
            } catch (Exception e) {
                throw new RuntimeException("Failed to list songs from MinIO", e);
            }
        }

        return songs;
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
