package com.emergencymesh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> sayHello() {
        return Map.of(
            "status", "UP",
            "project", "Emergency Mesh Server",
            "message", "Sunucu canavar gibi calisiyor!"
        );
    }
}