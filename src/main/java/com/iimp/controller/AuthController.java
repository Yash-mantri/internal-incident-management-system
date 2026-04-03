package com.iimp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
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
@CrossOrigin(origins="https://iimp-3gso.onrender.com",allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    
    @PostMapping("/sessions")
    public ResponseEntity<AuthDtos.LoginResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    
    @PostMapping("/sessions/refresh")
    public ResponseEntity<AuthDtos.LoginResponse> refresh(
            @Valid @RequestBody AuthDtos.RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refreshToken(req));
    }

   
    @PostMapping("/me/password")
    public ResponseEntity<String> changePassword( @Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword( req);
        return ResponseEntity.ok().body("Password Updated Successfully !");
    }
    
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> changePassword( @Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword( req);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/otp")
	public ResponseEntity<ApiResponse<Map<String, String>>> sendOtp(@Valid @RequestBody OtpSendRequest req) {
    	 String token = otpService.generateAndSend(req.email, req.purpose);
		 return ResponseEntity.ok(
		            ApiResponse.ok("OTP sent",
		                    Map.of("token", token)) 
		    );
	}
    
    @PostMapping("/otp/verification")
	public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
		
		otpService.verify(req.token, req.email,req.otpCode, req.purpose);

	    return ResponseEntity.ok(
	            ApiResponse.ok("OTP verified", Map.of("verified", true))
	    );
	}

}
