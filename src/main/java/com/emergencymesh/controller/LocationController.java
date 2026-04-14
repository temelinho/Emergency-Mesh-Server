package com.emergencymesh.controller;

import com.emergencymesh.dto.LocationDto;
import com.emergencymesh.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ResponseEntity<LocationDto.Response> updateLocation(@Valid @RequestBody LocationDto.Request request) {
        return ResponseEntity.ok(locationService.updateLocation(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<LocationDto.Response> getLocation(@PathVariable UUID userId) {
        return ResponseEntity.ok(locationService.getLocation(userId));
    }
}
