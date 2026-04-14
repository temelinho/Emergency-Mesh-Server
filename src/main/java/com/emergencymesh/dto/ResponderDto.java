package com.emergencymesh.dto;

import com.emergencymesh.model.Responder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResponderDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String name;
        private Responder.ResponderType type;
        private Double latitude;
        private Double longitude;
        private String city;
        private String district;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String name;
        private Responder.ResponderType type;
        private Responder.ResponderStatus status;
        private Double latitude;
        private Double longitude;
        private String city;
        private String district;
        private LocalDateTime updatedAt;

        public static Response fromEntity(Responder responder) {
            return Response.builder()
                    .id(responder.getId())
                    .name(responder.getName())
                    .type(responder.getType())
                    .status(responder.getStatus())
                    .latitude(responder.getLatitude())
                    .longitude(responder.getLongitude())
                    .city(responder.getCity())
                    .district(responder.getDistrict())
                    .updatedAt(responder.getUpdatedAt())
                    .build();
        }
    }
}
