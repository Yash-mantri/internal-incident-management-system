package com.iimp.controller;

import java.io.IOException;
import org.springframework.http.MediaType;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;

import com.iimp.dto.AttachmentDtos;
import com.iimp.dto.AuditDtos;
import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.entity.User;
import com.iimp.service.IncidentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://iimp-3gso.onrender.com", allowCredentials = "true")
public class IncidentController {

	private final IncidentService incidentService;

	@Value("${app.upload.dir:./uploads}")
	private String uploadDir;

	@PostMapping("/")
	public ResponseEntity<IncidentDtos.IncidentDetail> create(@AuthenticationPrincipal UserDetails userDetails,
			@Valid @RequestBody IncidentDtos.CreateIncidentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(incidentService.createIncident(userDetails.getUsername(), req));
	}

	@GetMapping("/")
	public ResponseEntity<Page<IncidentDtos.IncidentSummary>> list(@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
		User user = incidentService.findUserByEmail(userDetails.getUsername());
		return ResponseEntity.ok(incidentService.listIncidents(user.getEmail(), page, size));
	}

	@GetMapping("/me")
	public ResponseEntity<List<IncidentDtos.IncidentSummary>> getAllIncidentByUser(
			@AuthenticationPrincipal UserDetails userDetails) {
		User user = incidentService.findUserByEmail(userDetails.getUsername());
		return ResponseEntity.status(HttpStatus.OK).body(incidentService.getIncidentByUser(user.getEmail()));
	}

	@GetMapping("/{incidentKey}")
	public ResponseEntity<IncidentDtos.IncidentDetail> getById(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String incidentKey) {
		return ResponseEntity.ok(incidentService.getIncident(userDetails.getUsername(), incidentKey));
	}

	@PutMapping("/{id}/assignee")
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public ResponseEntity<IncidentDtos.IncidentDetail> assign(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String id, @Valid @RequestBody IncidentDtos.AssignIncidentRequest req) {
		return ResponseEntity.ok(incidentService.assignIncident(userDetails.getUsername(), id, req));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<IncidentDtos.Status> updateStatus(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String id, @Valid @RequestBody IncidentDtos.UpdateStatusRequest req) {
		return ResponseEntity.ok(incidentService.updateStatus(userDetails.getUsername(), id, req));
	}

	@PatchMapping("/{id}/priority")
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public ResponseEntity<IncidentDtos.IncidentPriorityUpdate> updatePriority(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id,
			@Valid @RequestBody IncidentDtos.IncidentPriorityUpdate req) {
		return ResponseEntity.ok(incidentService.updatePriority(userDetails.getUsername(), id, req));
	}

	@PutMapping("/{id}/category")
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public ResponseEntity<Boolean> recategorize(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String id, @Valid @RequestBody IncidentDtos.Recategorize req) {
		return ResponseEntity.ok(incidentService.recategorizeIncident(userDetails.getUsername(), id, req));
	}

	@PostMapping("/{id}/comments")
	public ResponseEntity<CommentDtos.CommentResponse> addComment(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String id, @Valid @RequestBody CommentDtos.AddCommentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(incidentService.addComment(userDetails.getUsername(), id, req));
	}

	@GetMapping("/{id}/comments")
	public ResponseEntity<List<CommentDtos.CommentDTO>> getCommentByIncidentId(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
		return ResponseEntity.status(HttpStatus.OK).body(incidentService.getComments(userDetails.getUsername(), id));
	}

	@PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AttachmentDtos.AttachmentResponse> uploadAttachment(
			@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id,
			@RequestParam("file") MultipartFile file) throws IOException {

		// 🔥 DEBUG
		System.out.println("Uploading file: " + file.getOriginalFilename());

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(incidentService.uploadAttachment(userDetails.getUsername(), id, file));
	}

	@GetMapping("/{id}/audit")
	public ResponseEntity<List<AuditDtos.AuditResponse>> getAuditLog(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable String id) {
		return ResponseEntity.ok(incidentService.getAuditLog(userDetails.getUsername(), id));
	}

	@GetMapping("/statistics")
	@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
	public ResponseEntity<IncidentDtos.IncidentStat> stats(@AuthenticationPrincipal UserDetails userDetails) {
		User user = incidentService.findUserByEmail(userDetails.getUsername());
		return ResponseEntity.ok(incidentService.incidentStat(user.getEmail()));
	}

	@GetMapping("/me/statistics")
	public ResponseEntity<IncidentDtos.IncidentStat> userStats(@AuthenticationPrincipal UserDetails userDetails) {
		User user = incidentService.findUserByEmail(userDetails.getUsername());
		return ResponseEntity.ok(incidentService.incidentUserStat(user.getEmail()));
	}

}
