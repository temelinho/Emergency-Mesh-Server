package com.emergencymesh.dto;

import com.emergencymesh.model.Location;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class LocationDto {

    @Data
    public static class Request {
        @NotNull(message = "User ID is required")
        private UUID userId;

        @NotNull(message = "Latitude is required")
        private Double latitude;

        @NotNull(message = "Longitude is required")
        private Double longitude;
    }

    @Data
    public static class Response {
        private UUID userId;
        private Double latitude;
        private Double longitude;
        private LocalDateTime timestamp;

        public static Response fromEntity(Location location) {
            Response response = new Response();
            response.setUserId(location.getUser().getId());
            response.setLatitude(location.getLatitude());
            response.setLongitude(location.getLongitude());
            response.setTimestamp(location.getTimestamp());
            return response;
        }
    }
}
