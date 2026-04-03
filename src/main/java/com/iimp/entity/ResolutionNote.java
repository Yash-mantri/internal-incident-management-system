package com.iimp.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resolution_notes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResolutionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String note;

    private LocalDateTime createdAt;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_key", referencedColumnName = "incident_key", nullable = false)
    private Incident incident;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}