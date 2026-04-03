package com.iimp.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Priority;
import com.iimp.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "incidents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   
    @Column(name = "incident_key", nullable = false, unique = true)
    private String incidentKey;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    /** Reporter */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /** Assigned support staff */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
   

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_id")
    private SlaConfig slaConfig;

    @Column(name = "sla_due_at")
    private LocalDateTime slaDueAt;

    @Column(name = "is_sla_breached", nullable = false)
    @Builder.Default
    private boolean isSlaBreached = false;
    
    @Column(name = "notified_to")
    @Enumerated(EnumType.STRING)
    private Role notifiedTo; 

    @Column(name = "escalation_level")
    @Builder.Default
    private Integer escalationLevel = 0;

    @Column(name = "last_notification_sent_at")
    private LocalDateTime lastNotificationSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL)
    private List<ResolutionNote> notes;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (escalationLevel == null) {
            escalationLevel = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
