package com.emergencymesh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "durum", "Sunucu Aktif",
            "mesaj", "Emergency Mesh Server başarıyla cevap veriyor!",
            "zaman", LocalDateTime.now()
        );
    }
}