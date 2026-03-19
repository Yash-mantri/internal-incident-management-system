package com.iimp.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.entity.Otp;
import com.iimp.repository.OtpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class  OtpService {

    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    @Value("${otp.expiry.minutes:5}")
    private int expiryMinutes;

    
	@Transactional
    public String generateAndSend(String email, Otp.OtpPurpose purpose) {
        
        otpRepository
            .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
            .ifPresent(old -> { old.setUsed(true); otpRepository.save(old); });

        String code = String.format("%06d", new Random().nextInt(999999));

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();
        otpRepository.save(otp);

        sendOtpEmail(email, code, purpose.name());
        log.info("🔢 OTP generated for {} [{}]", email, purpose);
        return code;   // ← remove from response in production
    }

  
	@Transactional
    public void verify(String email, String code, Otp.OtpPurpose purpose) {
        Otp otp = otpRepository
                .findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new RuntimeException("OTP not found. Request a new one."));

        if (otp.isExpired())
            throw new RuntimeException("OTP expired. Request a new one.");
        if (!otp.getOtpCode().equals(code))
            throw new RuntimeException("Invalid OTP. Try again.");

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    private void sendOtpEmail(String to, String code, String purpose) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("[Incident MAnagement Portal] Your OTP for " + purpose);
            msg.setText(
                "Your OTP is: " + code + "\n\n" +
                "Valid for " + expiryMinutes + " minutes.\n" +
                "Do NOT share this OTP with anyone.\n\n" +
                "Incident Management Portal"
            );
            mailSender.send(msg);
            log.info("📧 OTP email sent to {}", to);
        } catch (Exception e) {
            log.warn("📧 Email failed (check SMTP config): {}", e.getMessage());
        }
    }
}
