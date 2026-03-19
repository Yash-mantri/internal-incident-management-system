package com.iimp.service;

import com.iimp.entity.Incident;
import com.iimp.entity.IncidentAudit;
import com.iimp.entity.User;
import com.iimp.repository.IncidentAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final IncidentAuditRepository auditRepository;

    @Transactional
    public void log(Incident incident, User changedBy, String action, String oldValue, String newValue) {
        IncidentAudit audit = IncidentAudit.builder()
                .incident(incident)
                .changedBy(changedBy)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        auditRepository.save(audit);
    }

    @Transactional
    public void log(Incident incident, User changedBy, String action) {
        log(incident, changedBy, action, null, null);
    }
}
