package com.iimp.service;



import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.dto.NotificationDtos;
import com.iimp.entity.Notification;
import com.iimp.entity.User;
import com.iimp.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void send(User user, String message) {
    	
    	 try {
             SimpleMailMessage msg = new SimpleMailMessage();
             msg.setTo(user.getEmail());
             msg.setSubject("[Account created  ");
             msg.setText(message);
             mailSender.send(msg);
             log.info("📧 Email sent → {}", user.getEmail());
         } catch (Exception e) {
            
             log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
         }
        Notification n = Notification.builder()
                .user(user)
                .message(message)
                .build();
        notificationRepository.save(n);
    }

    @Transactional
    public void sendToAll(List<User> users, String message) {
        users.forEach(u -> send(u, message));
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> getUnread(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> getAll(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalse(userId)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationDtos.NotificationResponse toDto(Notification n) {
        return NotificationDtos.NotificationResponse.builder()
                .id(n.getId())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

	@Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> getUnreadAll(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
                .stream().map(this::toDto).toList();
    }

}
