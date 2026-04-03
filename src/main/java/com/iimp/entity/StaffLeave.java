package com.iimp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_leave", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "leave_date" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLeave {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "leave_date", nullable = false)
	private LocalDate leaveDate;

	@Column(nullable = false)
	private boolean approved;

	@Column(length = 255)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private User approvedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@PrePersist
	void prePersist() {
		createdAt = LocalDateTime.now();
	}
}