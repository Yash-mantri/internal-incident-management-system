package com.iimp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class DashboardDtos {

    @Data @Builder
    public static class EmployeeDashboard {
        private long myOpenCount;
        private long myInProgressCount;
        private long myResolvedCount;
        private long myClosedCount;
        private long totalMyTickets;
        private List<IncidentDtos.IncidentSummary> recentTickets;
        private List<NotificationDtos.NotificationResponse> unreadNotifications;
    }

    @Data @Builder
    public static class SupportDashboard {
        private long assignedOpenCount;
        private long assignedInProgressCount;
        private long assignedResolvedCount;
        private List<IncidentDtos.IncidentSummary> assignedTickets;
        private List<NotificationDtos.NotificationResponse> unreadNotifications;
    }

    @Data @Builder
    public static class ManagerDashboard {
        // Team stats
        private long deptTotalTickets;
        private long deptOpenCount;
        private long deptInProgressCount;
        private long deptResolvedCount;
        private long deptClosedCount;
        private long deptSlaBreachedCount;
        // Who is requesting (created_by employee → count)
        private List<RequesterStat> topRequesters;
        // Team workload (assigned_to staff → count)
        private List<TeamMemberStat> teamWorkload;
        private List<IncidentDtos.IncidentSummary> recentTickets;
        private List<NotificationDtos.NotificationResponse> unreadNotifications;
    }

    @Data @Builder
    public static class RequesterStat {
        private Long userId;
        private String name;
        private long totalRequests;
        private long openRequests;
        private long resolvedRequests;
    }

    @Data @Builder
    public static class TeamMemberStat {
        private Long userId;
        private String name;
        private String email;
        private long assignedCount;
        private long resolvedCount;
    }

    @Data @Builder
    public static class AdminDashboard {
        private long totalUsers;
        private long activeUsers;
        private long totalTicketsToday;
        private long totalTickets;
        private long openCount;
        private long inProgressCount;
        private long resolvedCount;
        private long closedCount;
        private long slaBreachedCount;
        private double slaCompliancePercent;
        private Map<String, Long> ticketsByCategory;
        private Map<String, Long> ticketsByPriority;
        private List<UserDtos.UserDetail> allUsers;
        private List<IncidentDtos.IncidentSummary> recentTickets;
        private List<NotificationDtos.NotificationResponse> unreadNotifications;
    }

    @Data
    public static class UpdateSlaRequest {
        @NotNull private Long id;
        @Min(1)  private int resolutionTimeHours;
    }

    @Data @Builder
    public static class ErrorResponse {
        private int status;
        private String message;
        private long timestamp;

        public static ErrorResponse of(int status, String message) {
            return ErrorResponse.builder()
                    .status(status).message(message)
                    .timestamp(System.currentTimeMillis()).build();
        }
    }
}
