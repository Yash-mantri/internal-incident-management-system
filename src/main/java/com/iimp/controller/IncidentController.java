package com.iimp.controller;

import java.io.IOException;
import org.springframework.data.domain.Page;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.iimp.dto.AttachmentDtos;
import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.entity.User;
import com.iimp.service.IncidentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    

    @PostMapping("/createIncident")
    public ResponseEntity<IncidentDtos.IncidentDetail> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody IncidentDtos.CreateIncidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.createIncident(userDetails.getUsername(), req));
    }

   
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDtos.IncidentDetail> getById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getIncident(userDetails.getUsername(), id));
    }


    @GetMapping("/getAllIncident")
    public ResponseEntity<Page<IncidentDtos.IncidentSummary>> list(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
    	User user=incidentService.findUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(incidentService.listIncidents(user.getEmail(), page, size));
    }
    
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<IncidentDtos.IncidentStat> stats(
            @AuthenticationPrincipal UserDetails userDetails) {
    	User user=incidentService.findUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(incidentService.incidentStat(user.getEmail()));
    }
   

    @PutMapping("/assign/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<IncidentDtos.IncidentDetail> assign(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody IncidentDtos.AssignIncidentRequest req) {
        return ResponseEntity.ok(incidentService.assignIncident(userDetails.getUsername(), id, req));
    }

   

    @PutMapping("/{id}/status")
    public ResponseEntity<IncidentDtos.IncidentDetail> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody IncidentDtos.UpdateStatusRequest req) {
        return ResponseEntity.ok(incidentService.updateStatus(userDetails.getUsername(), id, req));
    }

   

    @PostMapping("/comments/{id}")
    public ResponseEntity<CommentDtos.CommentResponse> addComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CommentDtos.AddCommentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.addComment(userDetails.getUsername(), id, req));
    }


    @PostMapping("/attachments/{id}")
    public ResponseEntity<AttachmentDtos.AttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(incidentService.uploadAttachment(userDetails.getUsername(), id, file, uploadDir));
    }
    

}
