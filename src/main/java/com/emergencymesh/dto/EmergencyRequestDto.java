package com.emergencymesh.dto;

import com.emergencymesh.model.EmergencyRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class EmergencyRequestDto {

    @Data
    public static class Create {
        @NotNull(message = "User ID is required")
        private UUID userId;

        @NotNull(message = "Type is required")
        private EmergencyRequest.EmergencyType type;

        private String description;

        private Double latitude;

        private Double longitude;

        private String priority;
    }

    @Data
    public static class UpdateStatus {
        @NotNull(message = "Status is required")
        private EmergencyRequest.RequestStatus status;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID userId;
        private String userFirstName;
        private String userLastName;
        private String userPhoneNumber;
        private EmergencyRequest.EmergencyType type;
        private String description;
        private EmergencyRequest.RequestStatus status;
        private Double latitude;
        private Double longitude;
        private String priority;
        private UUID assignedResponderId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response fromEntity(EmergencyRequest request) {
            Response response = new Response();
            response.setId(request.getId());
            
            if (request.getUser() != null) {
                response.setUserId(request.getUser().getId());
                response.setUserFirstName(request.getUser().getFirstName());
                response.setUserLastName(request.getUser().getLastName());
                response.setUserPhoneNumber(request.getUser().getPhoneNumber());
            }
            
            response.setType(request.getType());
            response.setDescription(request.getDescription());
            response.setStatus(request.getStatus());
            response.setLatitude(request.getLatitude());
            response.setLongitude(request.getLongitude());
            response.setPriority(request.getPriority() != null ? request.getPriority().name() : null);
            response.setAssignedResponderId(request.getAssignedResponder() != null ? request.getAssignedResponder().getId() : null);
            response.setCreatedAt(request.getCreatedAt());
            response.setUpdatedAt(request.getUpdatedAt());
            return response;
        }
    }
}
