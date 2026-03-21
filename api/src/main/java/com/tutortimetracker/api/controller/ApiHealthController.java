package com.tutortimetracker.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight operational endpoint for local smoke checks.
 */
@RestController
@RequestMapping("/api")
public class ApiHealthController {

    /**
     * @return service status and current mode
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "mode", "prototype"
        );
    }
}
