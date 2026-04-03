package com.iimp.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class ResolutionNoteDtos {
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ResolutionNote{
	    private String incidentKey;
	    private String note;
	}
	
	@Data@AllArgsConstructor@NoArgsConstructor@Builder
	public static class ResolutionNoteResponse{
		    private String note;
		    private LocalDateTime createdAt;
	}
	
}
