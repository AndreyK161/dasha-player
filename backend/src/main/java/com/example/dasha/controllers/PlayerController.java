package com.example.dasha.controllers;

import com.example.dasha.models.Song;
import com.example.dasha.services.MinioService;
import com.example.dasha.services.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final MinioService minioService;
    private final PlayerService playerService;

    public PlayerController(MinioService minioService, PlayerService playerService) {
        this.minioService = minioService;
        this.playerService = playerService;
    }

    @GetMapping("/songs")
    public ResponseEntity<List<Song>> getSongs() {
        return ResponseEntity.ok(minioService.listSongs());
    }

    @PostMapping("/songs/upload")
    public ResponseEntity<Map<String, String>> uploadSong(@RequestParam("file") MultipartFile file) {
        String objectName = minioService.uploadSong(file);
        return ResponseEntity.ok(Map.of("status", "uploaded", "objectName", objectName));
    }

    @DeleteMapping("/songs")
    public ResponseEntity<Map<String, String>> deleteSong(@RequestParam String objectName) {
        var current = playerService.getCurrentSong();
        if (current != null && current.getFileKey().equals("songs/" + objectName)) {
            return ResponseEntity.status(409).body(Map.of("error", "Трек сейчас играет в эфире"));
        }
        minioService.deleteSong(objectName);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @PostMapping("/stream/play-all")
    public ResponseEntity<Map<String, String>> playAll() {
        playerService.startPlaylist();
        return ResponseEntity.ok(Map.of("status", "streaming", "mode", "playlist"));
    }

    @PostMapping("/stream/stop")
    public ResponseEntity<Map<String, String>> stop() {
        playerService.stopStream();
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

@GetMapping("/stream/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> map = new HashMap<>();
        map.put("streaming", playerService.isStreaming());
        map.put("currentSong", playerService.getCurrentSong());
        map.put("positionSeconds", playerService.getPositionSeconds());
        return ResponseEntity.ok(map);
    }

    @GetMapping("/stream/current")
    public ResponseEntity<Song> currentSong() {
        Song song = playerService.getCurrentSong();
        return song != null ? ResponseEntity.ok(song) : ResponseEntity.noContent().build();
    }
}
