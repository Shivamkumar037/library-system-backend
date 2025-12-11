package com.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

public class LibraryDtos {

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String mobile;
        private String secretKey; // For Admin creation
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    @Builder
    public static class LoginResponse {
        private String token;
        private String username;
        private String role;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class UpdateUserRequest {
        private String mobile;
        // Username updates handled separately if logic requires uniqueness checks
    }

    @Data
    public static class BookUploadRequest {
        private String name;
        private String description;
        // File comes via MultipartFile, not DTO
    }

    @Data
    @Builder
    public static class BookResponse {
        private Long id;
        private String name;
        private String description;
        private Long ownerId;
        private String viewUrl;
        private String downloadUrl;
        private List<String> previews;
        private int downloads;
        private LocalDateTime createdAt;
        private long sizeBytes;
    }

    @Data
    @Builder
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String mobile;
        private String role;
        private boolean active;
        private int uploadsCount;
        private LocalDateTime lastLogin;
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;
    }
}