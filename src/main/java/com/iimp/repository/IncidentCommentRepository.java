package com.iimp.repository;

import com.iimp.entity.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {
    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
