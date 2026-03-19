package com.iimp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "comment_text", columnDefinition = "TEXT", nullable = false)
    private String commentText;

    /** Internal notes visible only to Staff/Manager/Admin */
    @Column(name = "is_internal", nullable = false)
    @Builder.Default
    private boolean isInternal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
