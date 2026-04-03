package com.iimp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import com.iimp.dto.SupportStaffDtos;
import com.iimp.service.IncidentService;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@CrossOrigin(origins="https://iimp-3gso.onrender.com",allowCredentials = "true")
public class SupportController {
 
	private final IncidentService incidentService;
	
	@GetMapping("/statistics")
	public ResponseEntity<SupportStaffDtos.SupportStaffStats> getStats(@AuthenticationPrincipal UserDetails userDetails){
		return ResponseEntity.status(HttpStatus.OK).body(incidentService.getSupportStaffStats(userDetails.getUsername()));
	}
	
	@GetMapping("/tickets/assigned")
	public ResponseEntity<SupportStaffDtos.SupportStaffAssignedTickets> getAssignedTickets(@AuthenticationPrincipal UserDetails userDetails){
		return ResponseEntity.status(HttpStatus.OK).body(incidentService.getAssignedTickets(userDetails.getUsername()));
	}
	
	@GetMapping("/unreadnotifications")
	public ResponseEntity<SupportStaffDtos.SupportStaffUnreadNotifications> getUnreadNotification(@AuthenticationPrincipal UserDetails userDetails){
		return ResponseEntity.status(HttpStatus.OK).body(incidentService.getSupportStaffUnreadNotifications(userDetails.getUsername()));
	}
}
