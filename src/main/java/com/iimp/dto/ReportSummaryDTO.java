package com.iimp.dto;

import com.iimp.entity.Category;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSummaryDTO {

    /** Number of tickets created today */
    private long totalToday;

    /** Total currently open tickets */
    private long openCount;

    /** Tickets breached (open past SLA + flagged breached) */
    private long breachedCount;

    /**
     * SLA compliance percentage (0.0 – 100.0)
     * Formula: (resolvedOnTime / totalResolvedWithSla) * 100
     * Returns 100.0 if no tickets have been resolved yet.
     */
    private double slaCompliance;

    @Data
	public static class CategoryBreakdown {
		private String label;
		private Integer count;
	}
}
