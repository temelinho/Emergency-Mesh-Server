package com.emergencymesh.dto;

import com.emergencymesh.model.Message;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageDto {

    @Data
    public static class Request {
        private UUID id; // Optional (if provided by mesh node)
        private Long originalCreatedAt; // Optional (original timestamp from offline node)

        @NotNull(message = "Sender ID is required")
        private UUID senderId;

        @NotBlank(message = "Content cannot be empty")
        private String content;

        private Double latitude;
        private Double longitude;
        private String priority;
        /** Opsiyonel: Uygulama doğrudan acil durum tipi gönderebilir (örn. "FIRE", "MEDICAL") */
        private String emergencyType;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID senderId;
        private String content;
        private Double latitude;
        private Double longitude;
        private String priority;
        private LocalDateTime timestamp;
        private boolean delivered;

        public static Response fromEntity(Message message) {
            Response response = new Response();
            response.setId(message.getId());
            response.setSenderId(message.getSender().getId());
            response.setContent(message.getContent());
            response.setLatitude(message.getLatitude());
            response.setLongitude(message.getLongitude());
            response.setPriority(message.getPriority() != null ? message.getPriority().name() : null);
            response.setTimestamp(message.getTimestamp());
            response.setDelivered(message.isDelivered());
            return response;
        }
    }
}
