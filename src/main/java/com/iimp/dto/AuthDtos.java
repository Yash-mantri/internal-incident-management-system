package com.iimp.dto;

import com.iimp.entity.Otp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private String email;
        private String name;
        private String role;
        private String department;
        private Long expiresIn;
        private boolean isFirstLogin;

        public LoginResponse(String accessToken, String refreshToken, String email,
                             String name, String role, String department,
                             Long expiresIn, boolean isFirstLogin) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.email = email;
            this.name = name;
            this.role = role;
            this.department = department;
            this.expiresIn = expiresIn;
            this.isFirstLogin = isFirstLogin;
        }
        public LoginResponse(String email,boolean isFirstLogin) {
        	this.email=email;
        	this.isFirstLogin=isFirstLogin;
        }
    }
    
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OtpSendRequest {
		@NotBlank
		@Email
		public String email;
		@NotNull
		public Otp.OtpPurpose purpose;

	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OtpVerifyRequest {
		@NotBlank
		@Email
		public String email;
		@NotBlank
		@Size(min = 6, max = 6)
		public String otpCode;
		@NotNull
		public Otp.OtpPurpose purpose;
	}
	
    @Data
    public static class ChangePasswordRequest {
    
    	@NotBlank
    	@Email
    	private String email;
        @NotBlank
        private String oldPassword;
        @NotBlank @Size(min = 6)
        private String newPassword;
        @NotBlank
        private String otpCode;
        @NotNull
		public Otp.OtpPurpose purpose;
       
    }
    
    @Data
    public static class ForgotPasswordRequest {
    	@NotBlank
    	@Email
    	private String email;
        @NotBlank @Size(min = 6)
        private String newPassword;
        @NotBlank
        private String otpCode;
        @NotNull
		public Otp.OtpPurpose purpose;
       
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }
    
    @Data
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class ApiResponse<T> {
		public boolean success;
		public String message;
		public T data;

		public static <T> ApiResponse<T> ok(String msg, T data) {
			return new ApiResponse<>(true, msg, data);
		}

		public static <T> ApiResponse<T> ok(String msg) {
			return new ApiResponse<>(true, msg, null);
		}

		public static <T> ApiResponse<T> error(String msg) {
			return new ApiResponse<>(false, msg, null);
		}
	}
}
