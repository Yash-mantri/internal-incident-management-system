package com.iimp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.AuthDtos;
import com.iimp.dto.AuthDtos.ApiResponse;
import com.iimp.dto.AuthDtos.OtpSendRequest;
import com.iimp.dto.AuthDtos.OtpVerifyRequest;
import com.iimp.service.AuthService;
import com.iimp.service.OtpService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins="http://192.168.0.166:5173",allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.LoginResponse> refresh(
            @Valid @RequestBody AuthDtos.RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

   
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword( @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword( req);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> changePassword( @Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword( req);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/send-otp")
	public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody OtpSendRequest req) {
		String code = otpService.generateAndSend(req.email, req.purpose);
		return ResponseEntity.ok(ApiResponse.ok("OTP sent to " + req.email,
				Map.of("otpCode", code, "note", "Remove otpCode field in production")));
	}
    
    @PostMapping("/verify-otp")
	public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
		otpService.verify(req.email, req.otpCode, req.purpose);
		return ResponseEntity.ok(ApiResponse.ok("OTP verified", Map.of("verified", true)));
	}

}
