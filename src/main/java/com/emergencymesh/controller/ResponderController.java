package com.emergencymesh.controller;

import com.emergencymesh.dto.ResponderDto;
import com.emergencymesh.model.Responder;
import com.emergencymesh.service.ResponderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/responders")
@RequiredArgsConstructor
public class ResponderController {

    private final ResponderService responderService;

    @PostMapping
    public ResponseEntity<ResponderDto.Response> createResponder(@RequestBody ResponderDto.Request request) {
        return ResponseEntity.ok(responderService.createResponder(request));
    }

    @GetMapping
    public ResponseEntity<List<ResponderDto.Response>> getAllResponders() {
        return ResponseEntity.ok(responderService.getAllResponders());
    }

    @PutMapping("/{id}/location")
    public ResponseEntity<ResponderDto.Response> updateLocation(
            @PathVariable UUID id, 
            @RequestParam Double lat, 
            @RequestParam Double lon) {
        return ResponseEntity.ok(responderService.updateLocation(id, lat, lon));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ResponderDto.Response> updateStatus(
            @PathVariable UUID id, 
            @RequestParam Responder.ResponderStatus status) {
        return ResponseEntity.ok(responderService.updateStatus(id, status));
    }
}
