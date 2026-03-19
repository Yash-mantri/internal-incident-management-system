package com.iimp.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.entity.Incident;
import com.iimp.entity.User;
import com.iimp.enums.Role;
import com.iimp.repository.IncidentRepository;
import com.iimp.repository.UserRepository;
import com.iimp.service.AuditService;
import com.iimp.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    /**
     * FR-23: Run every 5 minutes and flag breached tickets.
     * FR-24: Notify manager and admin on breach.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void checkSlaBreaches() {
        log.info("[SLA Scheduler] Checking SLA breaches at {}", LocalDateTime.now());

        List<Incident> breached = incidentRepository.findBreachedIncidents(LocalDateTime.now());
        if (breached.isEmpty()) return;

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        // Use a system user for audit — pick first admin or fallback
        User systemUser = admins.isEmpty() ? null : admins.get(0);

        for (Incident incident : breached) {
            incident.setSlaBreached(true);
            incidentRepository.save(incident);

            if (systemUser != null) {
                auditService.log(incident, systemUser, "SLA_BREACHED",
                        "WITHIN_SLA", "BREACHED");
            }

            String msg = "⚠ SLA BREACHED: " + incident.getIncidentKey()
                    + " [" + incident.getPriority() + "] " + incident.getTitle();

            // Notify admins
            notificationService.sendToAll(admins, msg);

            // Notify manager of that dept
            List<User> managers = userRepository.findByDepartmentAndRole(
                    incident.getCategory().getDepartmentName(), Role.MANAGER);
            notificationService.sendToAll(managers, msg);

            log.warn("[SLA Scheduler] Breached: {}", incident.getIncidentKey());
        }

        log.info("[SLA Scheduler] Flagged {} breached incidents", breached.size());
    }

    /**
     * FR-25: Critical tickets unassigned for > 1 hour — escalate to admins.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void checkCriticalEscalation() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<Incident> criticals = incidentRepository.findCriticalUnassigned(threshold);

        if (criticals.isEmpty()) return;

        List<User> admins = userRepository.findByRole(Role.ADMIN);
        User systemUser = admins.isEmpty() ? null : admins.get(0);

        for (Incident incident : criticals) {
            String msg = "🚨 ESCALATION: CRITICAL incident " + incident.getIncidentKey()
                    + " has been unassigned for over 1 hour — " + incident.getTitle();

            notificationService.sendToAll(admins, msg);

            if (systemUser != null) {
                auditService.log(incident, systemUser, "AUTO_ESCALATED_CRITICAL");
            }

            log.warn("[SLA Scheduler] Escalated critical unassigned: {}", incident.getIncidentKey());
        }
    }
}
