package com.iimp.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.dto.AuthDtos;
import com.iimp.entity.Otp;
import com.iimp.entity.User;
import com.iimp.repository.UserRepository;
import com.iimp.security.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final OtpService otpService;
    private final NotificationService notificationService;
    @Value("${app.jwt.expiration-ms}")
    private Long expirationMs;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is active
        if (!user.isActive()) {
            throw new DisabledException("Your account is inactive. Contact Admin.");
        }

        // Check if account is locked (FR-03)
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new LockedException("Account locked. Try again after " + user.getLockedUntil());
        }
//        if(user.isFirstLogin()) {
//        	System.out.println("Aaayaa");
//        	return  new AuthDtos.LoginResponse(user.getEmail(), user.isFirstLogin());
//        }
        

        // Validate password
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                throw new LockedException("Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts. Try again in " + LOCK_MINUTES + " minutes.");
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        // Reset failed attempts on success
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken  = jwtUtils.generateToken(userDetails, user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(userDetails);

        return new AuthDtos.LoginResponse(
                accessToken, refreshToken,
                user.getEmail(), user.getName(),
                user.getRole().name(), user.getDepartment(),
                expirationMs / 1000, user.isFirstLogin());
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }
        if(req.getOldPassword().equals(req.getNewPassword())) {
        	 throw new BadCredentialsException("New Password is not same as the old password !");
        }
    	otpService.verify(user.getEmail(), req.getOtpCode(), Otp.OtpPurpose.NEW_LOGIN);
    	
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setFirstLogin(false);
        userRepository.save(user);
        
        notificationService.send(user,
                "Welcome to IIMP! Your  password is changed If not u can to emergency immediately .");
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse refreshToken(AuthDtos.RefreshTokenRequest req) {
        String token = req.getRefreshToken();
        if (!jwtUtils.validateToken(token)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        String email = jwtUtils.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String newAccess  = jwtUtils.generateToken(userDetails, user.getRole().name());
        String newRefresh = jwtUtils.generateRefreshToken(userDetails);
        return new AuthDtos.LoginResponse(newAccess, newRefresh,
                user.getEmail(), user.getName(), user.getRole().name(),
                user.getDepartment(), expirationMs / 1000, user.isFirstLogin());
    }

	public void forgotPassword(@Valid AuthDtos.ForgotPasswordRequest req) {
		User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

    
    	otpService.verify(user.getEmail(), req.getOtpCode(), Otp.OtpPurpose.FORGOT_PASSWORD);
    	if(user.getPassword()!=req.getNewPassword()) {
    		user.setPassword(passwordEncoder.encode(req.getNewPassword()));
            user.setFirstLogin(false);
            userRepository.save(user);
            
            notificationService.send(user,
                    "Welcome to IIMP! Your  password is changed If not u can to emergency immediately .");
    		
    	}
    	else throw new BadCredentialsException("New Password is Not Same as the Old password");
        
	}
}
