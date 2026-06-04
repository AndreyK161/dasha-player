package com.example.dasha.models;

import lombok.Data;

import java.util.UUID;

@Data
public class Song {

    private UUID guid;
    private String songName;
    private String artist;
    private Double duration;
    private String fileKey;

    public Song(String songName, String artist, Double duration, String fileKey) {
        this.songName = songName;
        this.artist = artist;
        this.duration = duration;
        this.fileKey = fileKey;
    }

    public Song() {
    }
}
