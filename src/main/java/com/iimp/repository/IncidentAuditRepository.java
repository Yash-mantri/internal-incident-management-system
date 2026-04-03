package com.iimp.repository;

import com.iimp.entity.IncidentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentAuditRepository extends JpaRepository<IncidentAudit, Long> {
	List<IncidentAudit> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

	List<IncidentAudit> findByIncident_IncidentKey(String id);
}
