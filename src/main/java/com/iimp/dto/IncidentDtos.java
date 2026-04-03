package com.iimp.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

public class IncidentDtos {
	
	@Data
	public static class AddCategoryRequest {
		private String categoryName;
		private String departmentName;
	}

	/* ── Create ── */
	@Data
	public static class CreateIncidentRequest {
		@NotBlank
		@Size(min = 3, max = 150)
		private String title;

		@NotBlank
		@Size(min = 20)
		private String description;

		@NotBlank
		private String category; // category name e.g. "IT"

		@NotNull
		private Priority priority;
	}
	
	@Data	//CHANGES
	public static class Recategorize {
		private Long categoryId;
	}

	/* ── Assign ── */
	@Data
	public static class AssignIncidentRequest {
		@NotNull
		private Long assignedToUserId;
		@NotNull
		private String category;
	}

	@Data
	@Builder
	public static class IncidentStat {
		@NotNull
		private long todayTotal;
		@NotNull
		private long totalAll;
		@NotNull
		private long open;
		@NotNull
		private long inProgress;
		@NotNull
		private long resolved;
		@NotNull
		private long closed;
		long slaBreached;

	}

	/* ── Status Update ── */
	@Data
	public static class UpdateStatusRequest {
		@NotNull
		private IncidentStatus newStatus;
		private String note;
	}

	/* ── Summary (list row) ── */
	@Data
	@Builder
	public static class IncidentSummary {
		private Long id;
		private String incidentKey;
		private String title;
		private String category;
		private String department;
		private Priority priority;
		private String description;
		private IncidentStatus status;
		private String createdByName;
		private String assignedToName;
		private LocalDateTime createdAt;
		private LocalDateTime slaDueAt;
		private boolean slaBreached;
		private List<AttachmentDtos.AttachmentResponse> attachments;
	}

	@Data@Builder
	public static class Status {
		private String id;
		private IncidentStatus newStatus;
//    	private IncidentStatus status;

	}

	/* ── Detail (full ticket) ── */
	@Data
	@Builder
	public static class IncidentDetail {
		private Long id;
		private String incidentKey;
		private String title;
		private String description;
		private String category;
		private Priority priority;
		private IncidentStatus status;
		private UserDtos.UserSummary createdBy;
		private UserDtos.UserSummary assignedTo;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;
		private LocalDateTime resolvedAt;
		private LocalDateTime closedAt;
		private LocalDateTime slaDueAt;
		private boolean slaBreached;
		private List<CommentDtos.CommentResponse> comments;
		private List<AttachmentDtos.AttachmentResponse> attachments;
		private List<AuditDtos.AuditResponse> auditHistory;
	}

	@Data
	@Builder
	public static class IncidentPriorityUpdate {
		private Long id;
		private Priority priority;

	}

	/* ── Page wrapper ── */
	@Data
	@Builder
	public static class PagedIncidents {
		private List<IncidentSummary> content;
		private int page;
		private int size;
		private long totalElements;
		private int totalPages;
	}
}
