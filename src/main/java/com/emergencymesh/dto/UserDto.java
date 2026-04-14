package com.emergencymesh.dto;

import com.emergencymesh.model.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserDto {

    @Data
    public static class Request {
        @NotBlank(message = "Username is required")
        @jakarta.validation.constraints.Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Phone number is required")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(05|5)\\d{9}$|^\\+905\\d{9}$",
            message = "Invalid Turkish phone number format. Use 05XXXXXXXXX, 5XXXXXXXXX or +905XXXXXXXXX"
        )
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        @jakarta.validation.constraints.Size(min = 8, message = "Password must be at least 8 characters long")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number"
        )
        private String password;

        private String firstName;
        private String lastName;
        private String city;
        private String district;
    }

    @Data
    public static class UpdateRequest {
        @NotBlank(message = "Username is required")
        @jakarta.validation.constraints.Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Phone number is required")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(05|5)\\d{9}$|^\\+905\\d{9}$",
            message = "Invalid Turkish phone number format. Use 05XXXXXXXXX, 5XXXXXXXXX or +905XXXXXXXXX"
        )
        private String phoneNumber;

        private String firstName;
        private String lastName;
        private String city;
        private String district;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Phone number is required")
        private String phoneNumber;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class Response {
        private UUID id;
        private String username;
        private String phoneNumber;
        private String firstName;
        private String lastName;
        private String city;
        private String district;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response fromEntity(User user) {
            Response response = new Response();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setPhoneNumber(user.getPhoneNumber());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setCity(user.getCity());
            response.setDistrict(user.getDistrict());
            response.setCreatedAt(user.getCreatedAt());
            response.setUpdatedAt(user.getUpdatedAt());
            return response;
        }
    }
}
