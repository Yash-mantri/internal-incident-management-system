package com.iimp.service;

import com.iimp.dto.ResolutionNoteDtos;

public interface ResolutionNoteService {

	
	void saveNote(String incidentKey, String noteText);

	ResolutionNoteDtos.ResolutionNoteResponse getNotes(String incidentKey);

}