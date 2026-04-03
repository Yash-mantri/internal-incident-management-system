package com.iimp.service;

import java.util.List;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.iimp.dto.NotificationDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.Notification;
import com.iimp.entity.User;
import com.iimp.enums.EventType;
import com.iimp.exception.BadRequestException;
import com.iimp.repository.NotificationRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	@Override
	@Transactional
	public void sendUserCreated(User user,String password,EventType event) {
		try {
			Context context = new Context();
			context.setVariable("email", user.getEmail());
			context.setVariable("password", password);

			String htmlContent = templateEngine.process("account-created", context);

			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true);

			helper.setTo(user.getEmail());
			helper.setSubject("Your Account Created");
			helper.setText(htmlContent, true); // TRUE = HTML

			mailSender.send(msg);
			log.info("📧 Email sent → {}", user.getEmail());
		} catch (Exception e) {

			log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
		}
		Notification n = Notification.builder().user(user).message("User created").build();
		notificationRepository.save(n);
	}
	
	@Override
	@Transactional
	public void send(User user, Incident incident, EventType event)  {

		switch (event) {
		
		
		case FORGOT_PASSWORD:
			try {
				Context context = new Context();

				String htmlContent = templateEngine.process("forgot-password", context);

				MimeMessage msg = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(msg, true);

				helper.setTo(user.getEmail());
    	        helper.setSubject("Password Changed Sucessfully !");
				helper.setText(htmlContent, true); // TRUE = HTML

				mailSender.send(msg);
				log.info("📧 Email sent → {}", user.getEmail());
			} catch (Exception e) {

				log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
			}
			Notification n1 = Notification.builder().user(user).message("User created ").build();
			notificationRepository.save(n1);
			break;

		case CHANGE_PASSWORD:
			try {
				Context context = new Context();
                context.setVariable("username", user.getName());
				String htmlContent = templateEngine.process("change-password", context);

				MimeMessage msg = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(msg, true);

				helper.setTo(user.getEmail());
   	            helper.setSubject("Password Changed Sucessfully !");
				helper.setText(htmlContent, true); // TRUE = HTML

				mailSender.send(msg);
				log.info("📧 Email sent → {}", user.getEmail());
			} catch (Exception e) {

				log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
			}
			Notification n2 = Notification.builder().user(user).message("User created ").build();
			notificationRepository.save(n2);
			break;

		case INCIDENT_STATUS_CHANGED:
			try {
				Context context = new Context();
				context.setVariable("Incident ID", incident.getIncidentKey());
				context.setVariable("Old Status", incident.getStatus());
				context.setVariable("New Status", incident.getStatus());

				String htmlContent = templateEngine.process("incident-status-changed", context);

				MimeMessage msg = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(msg, true);

				helper.setTo(user.getEmail());
   	            helper.setSubject("Incident Status Changed !");
				helper.setText(htmlContent, true); // TRUE = HTML

				mailSender.send(msg);
				log.info("📧 Email sent → {}", user.getEmail());
			} catch (Exception e) {

				log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
			}
			Notification n3 = Notification.builder().user(user).message("User created ").build();
			notificationRepository.save(n3);
			break;

		case INCIDENT_ASSIGNED:
			try {
				Context context = new Context();
				context.setVariable("Incident ID", incident.getIncidentKey());
				context.setVariable("Title", incident.getTitle());
				context.setVariable("Priority", incident.getPriority());

				String htmlContent = templateEngine.process("incident-assign", context);

				MimeMessage msg = mailSender.createMimeMessage();
				MimeMessageHelper helper = new MimeMessageHelper(msg, true);
				helper.setSubject("Incident Assigned ");
				helper.setTo(user.getEmail());

				helper.setText(htmlContent, true); // TRUE = HTML

				mailSender.send(msg);
				log.info("📧 Email sent → {}", user.getEmail());
			} catch (Exception e) {

				log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
			}
			Notification n4 = Notification.builder().user(user).message("User created ").build();
			notificationRepository.save(n4);
			break;
		default:
			throw new BadRequestException("Illegal type event ");
			
		}

	}

	@Override
	@Transactional
	public void sendToAll(List<User> users, Incident incident, String eventType) {
		users.forEach(u -> sendMsg(u, incident, eventType));
	}
	
	@Override
	@Transactional
	public void sendMsg(User user, Incident incident, String eventType) {
		 try {
		        Context context = new Context();

		        context.setVariable("recipientName", user.getName());
		        context.setVariable("incidentKey", incident.getIncidentKey());
		        context.setVariable("title", incident.getTitle());
		        context.setVariable("priority", incident.getPriority());
		        context.setVariable("status", incident.getStatus());
		        context.setVariable("level", incident.getEscalationLevel());
		        context.setVariable("dashboardUrl", "http://localhost:3000/incidents/" + incident.getId());

		        // 🔥 choose template dynamically
		        String template = "sla-notification";

		        String subject = "";

		        if ("BREACH".equals(eventType)) {
		            subject = "⚠ SLA Breached - Immediate Attention Required";
		        } else if ("ESCALATION".equals(eventType)) {
		            subject = "🚨 Incident Escalated - Action Required";
		        } else if ("REMINDER".equals(eventType)) {
		            subject = "⏰ Reminder - SLA Still Breached";
		        }

		        String htmlContent = templateEngine.process(template, context);

		        MimeMessage msg = mailSender.createMimeMessage();
		        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

		        helper.setTo(user.getEmail());
		        helper.setSubject(subject);
		        helper.setText(htmlContent, true); // ✅ HTML enabled

		        mailSender.send(msg);

		        log.info("📧 Email sent → {}", user.getEmail());

		    } catch (Exception e) {
		        log.warn("📧 Email failed for {}: {}", user.getEmail(), e.getMessage());
		    }

		    Notification n = Notification.builder()
		            .user(user)
		            .message(eventType + " → " + incident.getIncidentKey())
		            .build();

		    notificationRepository.save(n);
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationDtos.NotificationResponse> getUnread(Long userId) {
		return notificationRepository.findByUserIdAndIsReadFalse(userId).stream().map(this::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationDtos.NotificationResponse> getAll(Long userId) {
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
	}

	@Override
	@Transactional
	public void markRead(Long notificationId, Long userId) {
		notificationRepository.findById(notificationId).ifPresent(n -> {
			if (n.getUser().getId().equals(userId)) {
				n.setRead(true);
				notificationRepository.save(n);
			}
		});
	}

	@Override
	@Transactional
	public void markAllRead(Long userId) {
		notificationRepository.findByUserIdAndIsReadFalse(userId).forEach(n -> {
			n.setRead(true);
			notificationRepository.save(n);
		});
	}

	@Override
	public long countUnread(Long userId) {
		return notificationRepository.countByUserIdAndIsReadFalse(userId);
	}

	private NotificationDtos.NotificationResponse toDto(Notification n) {
		return NotificationDtos.NotificationResponse.builder().id(n.getId()).message(n.getMessage()).read(n.isRead())
				.createdAt(n.getCreatedAt()).build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<NotificationDtos.NotificationResponse> getUnreadAll(Long userId) {
		return notificationRepository.findByUserIdAndIsReadFalse(userId).stream().map(this::toDto).toList();
	}

	@Override
	public void notifyall(List<User> users, String msg) {
		users.forEach(u -> notify(u, msg));
	}
	
	@Transactional
	@Override
	public void notify(User user,String msg) {
		  Notification n = Notification.builder()
		            .user(user)
		            .message(msg)
		            .build();

		    notificationRepository.save(n);
	}

	

}
