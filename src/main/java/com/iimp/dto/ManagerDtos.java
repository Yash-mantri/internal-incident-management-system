package com.iimp.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ManagerDtos {

    /* ══════════════════════════════════════════════════════════
       Response DTOs
    ══════════════════════════════════════════════════════════ */

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IncidentDetail {
        private Long id;
        private String incidentKey;
        private String title;
        private String description;
        private String priority;
        private String status;
        private CategoryRef category;
        private UserRef createdBy;
        private UserRef assignedTo;
        private LocalDateTime slaDueAt;
        private Boolean isSlaBreached;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime closedAt;
        private List<CommentDetail> comments;
        private List<AttachmentDetail> attachments;
    }

    /** Frontend reads: totalAll, open, inProgress, resolved, closed, slaBreached */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepartmentStats {
        private long totalAll;
        private long todayTotal;
        private long open;
        private long inProgress;
        private long resolved;
        private long closed;
        private long slaBreached;
    }

    /** Frontend reads: slaCompliance, totalToday */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReportSummary {
    	private long total;
        private long totalToday;
        private long openCount;
        private long breachedCount;
        private double slaCompliance;   // 0–100
    }

    /** Frontend reads: label, count  (ticket volume bar chart) */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VolumeEntry {
        private String label;
        private long count;
    }

    /** Frontend reads: label, count  (category breakdown) */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryEntry {
        private String label;
        private long count;
    }

    /** Frontend reads: id, name, fullName, department */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StaffSummary {
        private Long id;
        private String name;
        private String department;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentDetail {
        private Long id;
        private String commentText;
        private boolean isInternal;
        private LocalDateTime createdAt;
        private UserRef user;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttachmentDetail {
        private Long id;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String contentType;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CategoryRef {
        private Long id;
        private String categoryName;
        private String departmentName;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserRef {
        private Long id;
        private String name;
    }

    /* ══════════════════════════════════════════════════════════
       Request DTOs
    ══════════════════════════════════════════════════════════ */

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdateStatusRequest {
        private String newStatus;   // "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED"
        private String note;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UpdatePriorityRequest {
        private String priority;    // "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AssignRequest {
        private Long assignedToUserId;
        private String category;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RecategorizeRequest {
        private Long categoryId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AddCommentRequest {
        private String commentText;
        private boolean isInternal;
    }
}
