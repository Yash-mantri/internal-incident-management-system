package com.iimp.entity;

import com.iimp.enums.Priority;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private Priority priority;

    @Column(name = "resolution_time_hours", nullable = false)
    private int resolutionTimeHours;
}
