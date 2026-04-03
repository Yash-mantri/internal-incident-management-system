package com.iimp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.DashboardDtos;
import com.iimp.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins="https://iimp-3gso.onrender.com",allowCredentials = "true")
public class DashboardController {
	
    private final DashboardService dashboardService;

   
    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<DashboardDtos.EmployeeDashboard> employeeDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getEmployeeDashboard(userDetails.getUsername()));
    }

   
    @GetMapping("/support")
    @PreAuthorize("hasAnyRole('SUPPORT_STAFF','ADMIN')")
    public ResponseEntity<DashboardDtos.SupportDashboard> supportDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getSupportDashboard(userDetails.getUsername()));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<DashboardDtos.ManagerDashboard> managerDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getManagerDashboard(userDetails.getUsername()));
    }

  
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDtos.AdminDashboard> adminDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                dashboardService.getAdminDashboard(userDetails.getUsername()));
    }
}
