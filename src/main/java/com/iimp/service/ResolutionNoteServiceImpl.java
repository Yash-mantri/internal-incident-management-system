package com.iimp.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.dto.ResolutionNoteDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.ResolutionNote;
import com.iimp.enums.IncidentStatus;
import com.iimp.repository.IncidentRepository;
import com.iimp.repository.ResolutionNoteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResolutionNoteServiceImpl implements ResolutionNoteService {
	  private final ResolutionNoteRepository resolutionNoteRepository;
	    private final IncidentRepository incidentRepository;

	   
	    @Override
	    @Transactional
		public void saveNote(String incidentKey, String noteText) {

	        Incident incident = incidentRepository.findByIncidentKey(incidentKey)
	                .orElseThrow(() -> new RuntimeException("Incident not found"));

	        ResolutionNote note = ResolutionNote.builder()
	                .note(noteText)
	                .incident(incident)
	                .build();
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(LocalDateTime.now());
            incidentRepository.save(incident);
	        resolutionNoteRepository.save(note);
	    }
	    
	    @Override
		public ResolutionNoteDtos.ResolutionNoteResponse getNotes(String incidentKey) {
	    	return resolutionNoteRepository
	                .findTopByIncident_IncidentKeyOrderByCreatedAtDesc(incidentKey)
	                .map(note -> ResolutionNoteDtos.ResolutionNoteResponse.builder()
	                        .note(note.getNote())
	                        .createdAt(note.getCreatedAt())
	                        .build())
	                .orElse(null);
	    }
}
