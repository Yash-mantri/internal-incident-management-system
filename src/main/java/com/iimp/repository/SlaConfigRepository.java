package com.iimp.repository;

import com.iimp.entity.SlaConfig;
import com.iimp.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long> {
	Optional<SlaConfig> findByPriority(Priority priority);
}
