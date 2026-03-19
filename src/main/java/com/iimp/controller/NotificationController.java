package com.iimp.controller;

import com.iimp.dto.NotificationDtos;
import com.iimp.service.IncidentService;
import com.iimp.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final IncidentService incidentService;

   
    @GetMapping("/getAllNotifications")
    public ResponseEntity<List<NotificationDtos.NotificationResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(notificationService.getAll(userId));
    }

   @GetMapping("/getAllUnreadNotifications")
   public ResponseEntity<List<NotificationDtos.NotificationResponse>> getUnreadAll( @AuthenticationPrincipal UserDetails userDetails){
	   Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
       return ResponseEntity.ok(notificationService.getUnreadAll(userId));
   }
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDtos.NotificationResponse>> getUnread(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(notificationService.getUnread(userId));
    }

   
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
        return ResponseEntity.ok(Map.of("unread", notificationService.countUnread(userId)));
    }

   
    @PatchMapping("/read/{id}")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
        notificationService.markRead(id, userId);
        return ResponseEntity.ok().build();
    }

   
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = incidentService.findUserByEmail(userDetails.getUsername()).getId();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }
}
