package com.iimp.dto;

import com.iimp.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ══════════════════════════════════════════════════════════
// USER DTOs
// ══════════════════════════════════════════════════════════
public class UserDtos {

    @Data @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String department;
        private String role;
    }

    @Data @Builder
    public static class UserDetail {
        private Long id;
        private String name;
        private String email;
        private String department;
        private Role role;
        private boolean active;
        private boolean firstLogin;
        private LocalDateTime createdAt;
    }
    
    @Data @Builder
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String department;
        private Role role;
        private boolean active;
        
    }
    
    

    @Data
    public static class CreateUserRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
        @NotNull  private Role role;
        private String department;
    }

    @Data
    public static class UpdateUserRequest {
        private String name;
        private Role role;
        private String department;
    }
}
