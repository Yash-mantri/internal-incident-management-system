package com.iimp.service;

import java.util.List;

import com.iimp.dto.NotificationDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.User;
import com.iimp.enums.EventType;

public interface NotificationService {

	void send(User user, Incident incident, EventType event);

	void sendToAll(List<User> users, Incident incident, String eventType);

	void sendMsg(User user, Incident incident, String eventType);

	List<NotificationDtos.NotificationResponse> getUnread(Long userId);

	List<NotificationDtos.NotificationResponse> getAll(Long userId);

	void markRead(Long notificationId, Long userId);

	void markAllRead(Long userId);

	long countUnread(Long userId);

	List<NotificationDtos.NotificationResponse> getUnreadAll(Long userId);

	void sendUserCreated(User user, String tempPassword, EventType userCreated);

	void notifyall(List<User> users, String string);

	void notify(User createdBy, String message);

}