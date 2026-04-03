package com.iimp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iimp.entity.ResolutionNote;

@Repository
public interface ResolutionNoteRepository extends JpaRepository<ResolutionNote, Long> {

    List<ResolutionNote> findByIncident_IncidentKey(String incidentKey);
    
    Optional<ResolutionNote> findTopByIncident_IncidentKeyOrderByCreatedAtDesc(String incidentKey);
}
