package com.iimp.repository;

import com.iimp.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
	List<Attachment> findByIncidentId(Long incidentId);

	long countByIncident_IncidentKey(String incidentId);
}
