package com.emergencymesh.controller;

import com.emergencymesh.dto.MessageDto;
import com.emergencymesh.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // EKSİK OLAN IMPORT BUYDU
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

@Slf4j // Loglama yapabilmenizi sağlar
@Controller
@RequiredArgsConstructor // messageService için constructor injection yapar
public class WebSocketController {

    private final com.emergencymesh.service.EmergencyRequestService emergencyRequestService;

    @MessageMapping("/emergency.send")
    public void sendEmergency(@Valid @Payload com.emergencymesh.dto.EmergencyRequestDto.Create request) {
        log.info("Received WebSocket emergency report from user: {}", request.getUserId());
        emergencyRequestService.createRequest(request);
    }

    @MessageExceptionHandler
    public void handleException(MethodArgumentNotValidException ex) {
        log.error("WebSocket payload validation failed: {}", ex.getMessage());
    }
}