package com.iimp.service;

import java.security.Key;
import java.util.Date;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


import com.iimp.enums.EventType;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class  OtpServiceImpl implements OtpService  {

   
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

   
   
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); 
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
	@Override
	@Transactional
    public String generateAndSend(String email, EventType purpose) {
        
       
		String code = String.format("%06d", new Random().nextInt(999999));

	    Date now = new Date();
	    Date expiry = new Date(now.getTime() + 60 * 2000); // 1 min

	    String token = Jwts.builder()
	            .setSubject(email)
	            .claim("otp", code)
	            .claim("purpose", purpose.name())
	            .setIssuedAt(now)
	            .setExpiration(expiry)
	            .signWith(getSigningKey()) 
	            .compact();

	    sendOtpEmail(email, code, purpose.name());

	    return token;
    }

  
	@Override
	@Transactional
    public void verify(String token,String email, String code, EventType purpose) {
		 try {
		        Claims claims = Jwts.parserBuilder()   
		                .setSigningKey(getSigningKey())
		                .build()
		                .parseClaimsJws(token)
		                .getBody();

		        String otp = claims.get("otp", String.class);
		        String tokenPurpose = claims.get("purpose", String.class);
		        String tokenEmail = claims.getSubject();

		        
		        if (!tokenEmail.equals(email)) {
		            throw new RuntimeException("Invalid email");
		        }

		        if (!tokenPurpose.equals(purpose.name())) {
		            throw new RuntimeException("Invalid purpose");
		        }

		        if (!otp.equals(code)) {
		            throw new RuntimeException("Invalid OTP");
		        }

		    } catch (ExpiredJwtException e) {
		        throw new RuntimeException("OTP expired");
		    } catch (Exception e) {
		        throw new RuntimeException("Invalid token");
		    }
    }

    private void sendOtpEmail(String to, String code, String purpose) {
        try {
        	 Context context = new Context();
             context.setVariable("otp", code);

            String htmlContent = templateEngine.process("otp-email", context);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setTo(to);
            helper.setSubject("[Incident MAnagement Portal] Your OTP for " + purpose);
            helper.setText(htmlContent, true);
            mailSender.send(msg);
            log.info("📧 OTP email sent to {}", to);
        } catch (Exception e) {
            log.warn("📧 Email failed (check SMTP config): {}", e.getMessage());
        }
    }
}
