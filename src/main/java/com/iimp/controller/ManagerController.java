package com.iimp.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.ManagerDtos;
import com.iimp.service.ManagerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins="https://iimp-3gso.onrender.com",allowCredentials = "true")
public class ManagerController {


    private final ManagerService managerService;

    @GetMapping("/incidents")
    public ResponseEntity<List<ManagerDtos.IncidentDetail>> getAllIncidents(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                managerService.getAllIncidentsByDepartment(userDetails.getUsername()));
    }

    
    @GetMapping("/statistics")
    public ResponseEntity<ManagerDtos.DepartmentStats> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                managerService.getStatsByDepartment(userDetails.getUsername()));
    }

   
    @PostMapping("/incidents")
    public ResponseEntity<ManagerDtos.IncidentDetail> createIncident(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody IncidentDtos.CreateIncidentRequest req) {
        return ResponseEntity.ok(
                managerService.createIncident(userDetails.getUsername(), req));
    }

   
    @PutMapping("/incidents/{incidentKey}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable String incidentKey,
            @RequestBody ManagerDtos.UpdateStatusRequest req) {
        managerService.updateStatus(incidentKey, req);
        return ResponseEntity.ok().build();
    }

    
    @PatchMapping("/incidents/{incidentKey}/priority")
    public ResponseEntity<Void> updatePriority(
            @PathVariable String incidentKey,
            @RequestBody ManagerDtos.UpdatePriorityRequest req) {
        managerService.updatePriority(incidentKey, req);
        return ResponseEntity.ok().build();
    }

    
    @PutMapping("/incidents/{incidentKey}/assign")
    public ResponseEntity<Void> assignIncident(
            @PathVariable String incidentKey,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ManagerDtos.AssignRequest req) {
        managerService.assignIncident(userDetails.getUsername(), incidentKey, req);
        return ResponseEntity.ok().build();
    }

    
    @PutMapping("/incidents/{incidentKey}/category")
    public ResponseEntity<Void> recategorize(
            @PathVariable String incidentKey,
            @RequestBody ManagerDtos.RecategorizeRequest req) {
        managerService.recategorize(incidentKey, req);
        return ResponseEntity.ok().build();
    }

    
    @PostMapping("/incidents/{incidentKey}/comments")
    public ResponseEntity<CommentDtos.CommentResponse> addComment(
            @PathVariable String incidentKey,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CommentDtos.AddCommentRequest req) {
        return ResponseEntity.ok(managerService.addComment(incidentKey, userDetails.getUsername(), req));
    }

    
    @GetMapping("/reports/summary")
    public ResponseEntity<ManagerDtos.ReportSummary> getReportSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                managerService.getReportSummary(userDetails.getUsername()));
    }

    
    @GetMapping("/reports/volume")
    public ResponseEntity<List<ManagerDtos.VolumeEntry>> getTicketVolume(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "week") String range) {
        return ResponseEntity.ok(
                managerService.getTicketVolume(userDetails.getUsername(), range));
    }

    
    @GetMapping("/reports/category")
    public ResponseEntity<List<ManagerDtos.CategoryEntry>> getCategoryBreakdown(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                managerService.getCategoryBreakdown(userDetails.getUsername()));
    }

    
    @GetMapping("/support-staff")
    public ResponseEntity<List<ManagerDtos.StaffSummary>> getSupportStaff(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                managerService.getSupportStaff(userDetails.getUsername()));
    }
}
