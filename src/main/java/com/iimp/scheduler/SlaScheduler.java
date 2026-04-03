//
//package com.iimp.scheduler;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.iimp.entity.Incident;
//import com.iimp.entity.User;
//import com.iimp.enums.Role;
//import com.iimp.repository.IncidentRepository;
//import com.iimp.repository.UserRepository;
//import com.iimp.service.AuditService;
//import com.iimp.service.NotificationService;
//
//import lombok.RequiredArgsConstructor;
//
//@Component
//@EnableScheduling
//@RequiredArgsConstructor
//public class SlaScheduler {
//
//	private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);
//
//	private final IncidentRepository incidentRepository;
//	private final UserRepository userRepository;
//	private final NotificationService notificationService;
//	private final AuditService auditService;
//
//	// 🔥 SLA BREACH CHECK
//	@Scheduled(fixedRate = 300_000)
//	@Transactional
//	public void checkSlaBreaches() {
//
//		LocalDateTime now = LocalDateTime.now();
//		List<Incident> breached = incidentRepository.findBreachedIncidents(now);
//
//		if (breached.isEmpty())
//			return;
//
//		List<User> admins = userRepository.findByRole(Role.ADMIN);
//
//		for (Incident incident : breached) {
//
//			incident.setSlaBreached(true);
//
//			String department = incident.getCategory().getDepartmentName();
//			List<User> managers = userRepository.findByDepartmentAndRole(department, Role.MANAGER);
//
//			boolean isOtherCategory = "OTHER".equalsIgnoreCase(department);
//			User systemUser = admins.isEmpty() ? null : admins.get(0);
//
//			// 🔥 FIRST TIME BREACH
//			if (incident.getNotifiedTo() == null) {
//
//				if (isOtherCategory) {
//
//					notificationService.sendToAll(admins, incident, "BREACH");
//					incident.setNotifiedTo(Role.ADMIN);
//					incident.setEscalationLevel(1);
//
//					// ✅ AUDIT
//					if (systemUser != null) {
//						auditService.log(incident, systemUser, "SLA_BREACHED", "WITHIN_SLA", "ADMIN_NOTIFIED");
//					}
//
//					log.warn("[SLA] Sent directly to ADMIN: {}", incident.getIncidentKey());
//
//				} else {
//
//					notificationService.sendToAll(managers, incident, "BREACH");
//					incident.setNotifiedTo(Role.MANAGER);
//					incident.setEscalationLevel(1);
//
//					// ✅ AUDIT
//					if (systemUser != null) {
//						auditService.log(incident, systemUser, "SLA_BREACHED", "WITHIN_SLA", "MANAGER_NOTIFIED");
//					}
//
//					log.warn("[SLA] Sent to MANAGER: {}", incident.getIncidentKey());
//				}
//
//				incident.setLastNotificationSentAt(now);
//			}
//
//			// 🔥 ESCALATE TO ADMIN
//			else if (incident.getNotifiedTo() == Role.MANAGER
//					&& incident.getLastNotificationSentAt().isBefore(now.minusMinutes(30))) {
//
//				notificationService.sendToAll(admins, incident, "ESCALATION");
//
//				incident.setNotifiedTo(Role.ADMIN);
//				incident.setEscalationLevel(2);
//				incident.setLastNotificationSentAt(now);
//
//				// ✅ AUDIT
//				if (systemUser != null) {
//					auditService.log(incident, systemUser, "ESCALATED", "MANAGER", "ADMIN");
//				}
//
//				log.warn("[SLA] Escalated to ADMIN: {}", incident.getIncidentKey());
//			}
//
//			// 🔥 ADMIN REMINDER
//			else if (incident.getNotifiedTo() == Role.ADMIN
//					&& incident.getLastNotificationSentAt().isBefore(now.minusMinutes(60))) {
//
//				notificationService.sendToAll(admins, incident, "REMINDER");
//
//				incident.setLastNotificationSentAt(now);
//
//				// ✅ AUDIT
//				if (systemUser != null) {
//					auditService.log(incident, systemUser, "REMINDER_SENT", "ADMIN", "ADMIN");
//				}
//
//				log.warn("[SLA] Admin Reminder: {}", incident.getIncidentKey());
//			}
//
//			incidentRepository.save(incident);
//		}
//	}
//
//	// 🔥 CRITICAL ESCALATION CHECK
//	@Scheduled(fixedRate = 300_000)
//	@Transactional
//	public void checkCriticalEscalation() {
//
//		LocalDateTime now = LocalDateTime.now();
//
//		List<Incident> criticals = incidentRepository.findCriticalUnassigned(now.minusHours(1));
//		if (criticals.isEmpty())
//			return;
//
//		List<User> admins = userRepository.findByRole(Role.ADMIN);
//		List<User> managers = userRepository.findByRole(Role.MANAGER);
//
//		for (Incident incident : criticals) {
//
//			long hoursUnassigned = java.time.Duration.between(incident.getCreatedAt(), now).toHours();
//
//			User systemUser = admins.isEmpty() ? null : admins.get(0);
//
//			// 🔥 Level 1 → Manager
//			if (hoursUnassigned >= 1 && incident.getEscalationLevel() == null) {
//
//				notificationService.sendToAll(managers, incident, "ESCALATION");
//
//				incident.setEscalationLevel(1);
//				incident.setLastNotificationSentAt(now);
//
//				// ✅ AUDIT
//				if (systemUser != null) {
//					auditService.log(incident, systemUser, "CRITICAL_ESCALATION_L1", "UNASSIGNED", "MANAGER_NOTIFIED");
//				}
//
//				log.warn("[ESCALATION L1] {}", incident.getIncidentKey());
//			}
//
//			// 🔥 Level 2 → Admin
//			else if (hoursUnassigned >= 2 && incident.getEscalationLevel() < 1) {
//
//				notificationService.sendToAll(admins, incident, "ESCALATION");
//
//				incident.setEscalationLevel(2);
//				incident.setLastNotificationSentAt(now);
//
//				// ✅ AUDIT
//				if (systemUser != null) {
//					auditService.log(incident, systemUser, "CRITICAL_ESCALATION_L2", "MANAGER", "ADMIN_NOTIFIED");
//				}
//
//				log.warn("[ESCALATION L2] {}", incident.getIncidentKey());
//			}
//
//			// 🔥 Reminder
//			else if (hoursUnassigned >= 4 && (incident.getLastNotificationSentAt() == null
//					|| incident.getLastNotificationSentAt().isBefore(now.minusHours(1)))) {
//
//				notificationService.sendToAll(admins, incident, "REMINDER");
//
//				incident.setLastNotificationSentAt(now);
//
//				// ✅ AUDIT
//				if (systemUser != null) {
//					auditService.log(incident, systemUser, "CRITICAL_REMINDER", "ADMIN", "ADMIN");
//				}
//
//				log.warn("[ESCALATION REMINDER] {}", incident.getIncidentKey());
//			}
//
//			incidentRepository.save(incident);
//		}
//	}
//}


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
import com.iimp.util.BusinessHoursCalculator;

import lombok.RequiredArgsConstructor;


@Component
@EnableScheduling
@RequiredArgsConstructor
public class SlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);

    private final IncidentRepository       incidentRepository;
    private final UserRepository           userRepository;
    private final NotificationService      notificationService;
    private final AuditService             auditService;
    private final BusinessHoursCalculator  businessHoursCalc;

    
    
    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void checkSlaBreaches() {

        LocalDateTime now = LocalDateTime.now();

       
        List<Incident> activeIncidents = incidentRepository.findActiveIncidents();

        if (activeIncidents.isEmpty()) return;

        List<User> admins = userRepository.findByRole(Role.ADMIN);

        for (Incident incident : activeIncidents) {


            User assignee = incident.getAssignedTo(); 

           
            double elapsedBusinessHours = businessHoursCalc.calcElapsedBusinessHours(
                incident.getCreatedAt(), now, assignee
            );

            double slaHours = incident.getSlaConfig() != null
                ? incident.getSlaConfig().getResolutionTimeHours()
                : getDefaultSlaHours(incident);

            boolean breachedNow = elapsedBusinessHours >= slaHours;

          
            if (incident.getSlaDueAt() == null) {
                incident.setSlaDueAt(businessHoursCalc.calcDeadline(
                    incident.getCreatedAt(), slaHours, assignee
                ));
            }

            if (!breachedNow) continue; 

            
            incident.setSlaBreached(true);

            String department = incident.getCategory() != null
                ? incident.getCategory().getDepartmentName() : "OTHER";

            List<User> managers = userRepository.findByDepartmentAndRole(department, Role.MANAGER);
            boolean isOtherCategory = "OTHER".equalsIgnoreCase(department);

            User systemUser = admins.isEmpty() ? null : admins.get(0);

            
            if (incident.getNotifiedTo() == null) {

                if (isOtherCategory) {
                    notificationService.sendToAll(admins, incident, "BREACH");
                    incident.setNotifiedTo(Role.ADMIN);
                    incident.setEscalationLevel(1);
                    incident.setLastNotificationSentAt(now);

                    if (systemUser != null) {
                        auditService.log(incident, systemUser, "SLA_BREACHED", "WITHIN_SLA", "ADMIN_NOTIFIED");
                    }

                    log.warn("[SLA] Breach → ADMIN (OTHER category): incidentKey={}, elapsed={}h, sla={}h",
                        incident.getIncidentKey(), String.format("%.1f", elapsedBusinessHours), slaHours);

                } else {
                    notificationService.sendToAll(managers, incident, "BREACH");
                    incident.setNotifiedTo(Role.MANAGER);
                    incident.setEscalationLevel(1);
                    incident.setLastNotificationSentAt(now);

                    if (systemUser != null) {
                        auditService.log(incident, systemUser, "SLA_BREACHED", "WITHIN_SLA", "MANAGER_NOTIFIED");
                    }

                    log.warn("[SLA] Breach → MANAGER: incidentKey={}, elapsed={}h, sla={}h",
                        incident.getIncidentKey(), String.format("%.1f", elapsedBusinessHours), slaHours);
                }

            }

           
            else if (incident.getNotifiedTo() == Role.MANAGER
                && incident.getLastNotificationSentAt().isBefore(now.minusMinutes(30))) {

                notificationService.sendToAll(admins, incident, "ESCALATION");
                incident.setNotifiedTo(Role.ADMIN);
                incident.setEscalationLevel(2);
                incident.setLastNotificationSentAt(now);

                if (systemUser != null) {
                    auditService.log(incident, systemUser, "ESCALATED", "MANAGER", "ADMIN");
                }

                log.warn("[SLA] Escalated → ADMIN: incidentKey={}", incident.getIncidentKey());

            }

           
            else if (incident.getNotifiedTo() == Role.ADMIN
                && incident.getLastNotificationSentAt().isBefore(now.minusMinutes(60))) {

                notificationService.sendToAll(admins, incident, "REMINDER");
                incident.setLastNotificationSentAt(now);

                if (systemUser != null) {
                    auditService.log(incident, systemUser, "REMINDER_SENT", "ADMIN", "ADMIN");
                }

                log.warn("[SLA] Admin reminder: incidentKey={}", incident.getIncidentKey());
            }

            incidentRepository.save(incident);
        }

        log.debug("[SLA] Breach check complete. Checked {} active incidents.", activeIncidents.size());
    }

    @Scheduled(fixedRate = 300_000) // 5 minutes
    @Transactional
    public void checkCriticalEscalation() {

        LocalDateTime now = LocalDateTime.now();

        
        List<Incident> criticals = incidentRepository.findCriticalUnassigned(now.minusHours(1));

        if (criticals.isEmpty()) return;

        List<User> admins    = userRepository.findByRole(Role.ADMIN);
        List<User> managers  = userRepository.findByRole(Role.MANAGER);
        User systemUser      = admins.isEmpty() ? null : admins.get(0);

        for (Incident incident : criticals) {

            // Use business hours elapsed — not raw wall-clock hours
            double businessHoursUnassigned = businessHoursCalc.calcElapsedBusinessHours(
                incident.getCreatedAt(), now, null // no assignee yet
            );

           
            if (businessHoursUnassigned >= 1.0
                    && (incident.getEscalationLevel() == null || incident.getEscalationLevel() < 1)) {

                notificationService.sendToAll(managers, incident, "ESCALATION");
                incident.setEscalationLevel(1);
                incident.setLastNotificationSentAt(now);

                if (systemUser != null) {
                    auditService.log(incident, systemUser,
                        "CRITICAL_ESCALATION_L1", "UNASSIGNED", "MANAGER_NOTIFIED");
                }

                log.warn("[CRITICAL L1] Managers notified: incidentKey={}, businessHrsUnassigned={}",
                    incident.getIncidentKey(), String.format("%.1f", businessHoursUnassigned));

            }

            
            else if (businessHoursUnassigned >= 2.0
                    && incident.getEscalationLevel() != null
                    && incident.getEscalationLevel() < 2) {

                notificationService.sendToAll(admins, incident, "ESCALATION");
                incident.setEscalationLevel(2);
                incident.setLastNotificationSentAt(now);

                if (systemUser != null) {
                    auditService.log(incident, systemUser,
                        "CRITICAL_ESCALATION_L2", "MANAGER", "ADMIN_NOTIFIED");
                }

                log.warn("[CRITICAL L2] Admins notified: incidentKey={}, businessHrsUnassigned={}",
                    incident.getIncidentKey(), String.format("%.1f", businessHoursUnassigned));

            }

            
            else if (businessHoursUnassigned >= 4.0
                    && incident.getEscalationLevel() != null
                    && incident.getEscalationLevel() >= 2) {

                
                boolean timeForReminder = incident.getLastNotificationSentAt() == null
                    || businessHoursCalc.calcElapsedBusinessHours(
                        incident.getLastNotificationSentAt(), now, null) >= 1.0;

                if (timeForReminder) {
                    notificationService.sendToAll(admins, incident, "REMINDER");
                    incident.setLastNotificationSentAt(now);

                    if (systemUser != null) {
                        auditService.log(incident, systemUser,
                            "CRITICAL_REMINDER", "ADMIN", "ADMIN");
                    }

                    log.warn("[CRITICAL REMINDER] incidentKey={}, businessHrsUnassigned={}",
                        incident.getIncidentKey(), String.format("%.1f", businessHoursUnassigned));
                }
            }

            incidentRepository.save(incident);
        }

        log.debug("[CRITICAL] Escalation check complete. Checked {} critical unassigned incidents.",
            criticals.size());
    }

    
    private double getDefaultSlaHours(Incident incident) {
        if (incident.getPriority() == null) return 24.0;
        return switch (incident.getPriority()) {
            case CRITICAL -> 6.0;
            case HIGH     -> 12.0;
            case MEDIUM   -> 24.0;
            case LOW      -> 48.0;
        };
    }
}