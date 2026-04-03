package com.iimp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.ReportSummaryDTO;
import com.iimp.dto.TicketVolumeDto;
import com.iimp.service.IncidentService;
import com.iimp.service.ReportService;

import lombok.RequiredArgsConstructor;

@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
@RestController
@CrossOrigin(origins = "https://iimp-3gso.onrender.com", allowCredentials = "true")
public class StatsController {
	private final IncidentService incidentService;
	private final ReportService reportService;

	@GetMapping("/summary")
	public ResponseEntity<ReportSummaryDTO> getSummary() {
		return new ResponseEntity<ReportSummaryDTO>(incidentService.getSummary(), HttpStatus.ACCEPTED);
	}

	@GetMapping("/categories")
	public ResponseEntity<List<ReportSummaryDTO.CategoryBreakdown>> getCategoryBreakdown() {
		return new ResponseEntity<List<ReportSummaryDTO.CategoryBreakdown>>(incidentService.getCatgeoryBreakdown(),
				HttpStatus.ACCEPTED);
	}

	@GetMapping("/volume")
	public ResponseEntity<List<TicketVolumeDto>> getTicketVolume(@RequestParam String range) {
		return ResponseEntity.ok(reportService.getTicketVolume(range));
	}

}
