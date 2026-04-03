package com.iimp.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

public class SupportStaffDtos {

	@Data @Builder
    public static class SupportStaffStats {
        private long assignedOpenCount;
        private long assignedInProgressCount;
        private long assignedResolvedCount;
    }
	
	@Data @Builder
	public static class SupportStaffAssignedTickets{
		private List<IncidentDtos.IncidentSummary> assignedTickets;
	}
	
	@Data@Builder
	public static class SupportStaffUnreadNotifications{
		private List<NotificationDtos.NotificationResponse> unreadNotifications;
	}
    
}
