package com.emergencymesh.controller;

import com.emergencymesh.dto.MessageDto;
import com.emergencymesh.service.MessageService;
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
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<MessageDto.Response> sendMessage(@Valid @RequestBody MessageDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.sendMessage(request));
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> sendMessages(@RequestBody java.util.List<MessageDto.Request> requests) {
        System.out.println("[CONTROLLER] Received batch request with " + (requests != null ? requests.size() : 0) + " items");
        if (requests != null) {
            for (MessageDto.Request req : requests) {
                System.out.println("[CONTROLLER] Message Item: Sender=" + req.getSenderId() + ", Content=" + req.getContent());
            }
        }
        messageService.sendMessages(requests);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MessageDto.Response>> getMessagesForUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(messageService.getMessagesForUser(userId, pageable));
    }

}
