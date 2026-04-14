package com.emergencymesh.controller;

import com.emergencymesh.dto.EmergencyRequestDto;
import com.emergencymesh.model.EmergencyRequest;
import com.emergencymesh.service.EmergencyRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/emergency")
@RequiredArgsConstructor
public class EmergencyRequestController {

    private final EmergencyRequestService service;

    @PostMapping
    public ResponseEntity<EmergencyRequestDto.Response> createRequest(
            @Valid @RequestBody EmergencyRequestDto.Create request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(request));
    }

    @GetMapping
    public ResponseEntity<Page<EmergencyRequestDto.Response>> getAllRequests(
            @RequestParam(required = false) EmergencyRequest.RequestStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(service.getRequestsByStatus(status, pageable));
        }
        return ResponseEntity.ok(service.getAllRequests(pageable));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<EmergencyRequestDto.Response> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EmergencyRequestDto.UpdateStatus statusDto) {
        return ResponseEntity.ok(service.updateStatus(id, statusDto.getStatus()));
    }

    @PutMapping("/{id}/assign/{responderId}")
    public ResponseEntity<EmergencyRequestDto.Response> assignResponder(
            @PathVariable UUID id,
            @PathVariable UUID responderId) {
        return ResponseEntity.ok(service.assignResponder(id, responderId));
    }
}
