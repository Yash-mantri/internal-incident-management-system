package com.iimp.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.ResolutionNoteDtos;
import com.iimp.service.ResolutionNoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin(origins="https://iimp-3gso.onrender.com",allowCredentials = "true")
public class ResolutionNoteController {

    private final ResolutionNoteService resolutionNoteService;

    
    @PostMapping
    public void addNote(@RequestBody ResolutionNoteDtos.ResolutionNote req) {
    	System.out.println(req.getIncidentKey());
    	resolutionNoteService.saveNote(
                req.getIncidentKey(),
                req.getNote()
        );
    }

    
    @GetMapping("/{incidentKey}")
    public ResolutionNoteDtos.ResolutionNoteResponse getNotes(@PathVariable String incidentKey) {
        return resolutionNoteService.getNotes(incidentKey);
    }
}