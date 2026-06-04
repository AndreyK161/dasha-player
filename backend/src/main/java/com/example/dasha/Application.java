package com.example.dasha;

import com.example.dasha.services.PlayerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class Application {

    private final PlayerService playerService;

    public Application(PlayerService playerService) {
        this.playerService = playerService;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStart() {
        playerService.startPlaylist();
    }

}
