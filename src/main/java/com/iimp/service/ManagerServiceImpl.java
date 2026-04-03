package com.iimp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.ManagerDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.User;
import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Priority;
import com.iimp.enums.Role;
import com.iimp.exception.ResourceNotFoundException;
import com.iimp.repository.AttachmentRepository;
import com.iimp.repository.CategoryRepository;
import com.iimp.repository.IncidentCommentRepository;
import com.iimp.repository.IncidentRepository;
import com.iimp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final IncidentRepository incidentRepository;
    private final UserRepository     userRepository;
    private final IncidentCommentRepository  commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final CategoryRepository categoryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final IncidentService incidentService;

  
    private User getManager(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

  
    @Override
	public List<ManagerDtos.IncidentDetail> getAllIncidentsByDepartment(String username) {
        String dept = getManager(username).getDepartment();
        return incidentRepository
                .findByCategoryDepartmentNameOrderByCreatedAtDesc(dept)
                .stream().map(this::toIncidentDetail).collect(Collectors.toList());
    }

    
    @Override
	public ManagerDtos.DepartmentStats getStatsByDepartment(String username) {
        String dept = getManager(username).getDepartment();
        LocalDateTime startOfDay = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        return ManagerDtos.DepartmentStats.builder()
                .totalAll(incidentRepository.countAllByDepartment(dept))
                .todayTotal(incidentRepository.countCreatedSinceByDepartment(dept, startOfDay))
                .open(incidentRepository.countByDepartmentAndStatus(dept, IncidentStatus.OPEN))
                .inProgress(incidentRepository.countByDepartmentAndStatus(dept, IncidentStatus.IN_PROGRESS))
                .resolved(incidentRepository.countByDepartmentAndStatus(dept, IncidentStatus.RESOLVED))
                .closed(incidentRepository.countByDepartmentAndStatus(dept, IncidentStatus.CLOSED))
                .slaBreached(incidentRepository.countSlaBreachedByDepartment(dept))
                .build();
    }

   
    @Override
	public ManagerDtos.IncidentDetail createIncident(String username, IncidentDtos.CreateIncidentRequest req) {
       
        throw new UnsupportedOperationException(
                "Wire to your existing IncidentService.createIncident() or replicate logic here.");
    }

    
    @Override
	public void updateStatus(String incidentKey, ManagerDtos.UpdateStatusRequest req) {
        Incident incident = incidentRepository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
        incident.setStatus(IncidentStatus.valueOf(req.getNewStatus()));
        LocalDateTime now = LocalDateTime.now();
        incident.setUpdatedAt(now);
        if (IncidentStatus.RESOLVED.name().equals(req.getNewStatus())) incident.setResolvedAt(now);
        if (IncidentStatus.CLOSED.name().equals(req.getNewStatus()))   incident.setClosedAt(now);
        incidentRepository.save(incident);
    }

    
    @Override
	public void updatePriority(String incidentKey, ManagerDtos.UpdatePriorityRequest req) {
        Incident incident = incidentRepository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
        incident.setPriority(Priority.valueOf(req.getPriority()));
        incidentRepository.save(incident);
    }

    
    @Override
	public void assignIncident(String managerEmail, String incidentKey, ManagerDtos.AssignRequest req) {
    	
    	User manager = findUserByEmail(managerEmail);
        Incident incident = incidentRepository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
        User staff = userRepository.findById(req.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + req.getAssignedToUserId()));
        incident.setAssignedTo(staff);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setUpdatedAt(LocalDateTime.now());
        incidentRepository.save(incident);
        
        String oldAssignee = incident.getAssignedTo() != null
                ? incident.getAssignedTo().getName() : "UNASSIGNED";
        
        auditService.log(incident, manager, "ASSIGNED", oldAssignee, staff.getName());
        auditService.log(incident, manager, "STATUS_CHANGED",
                IncidentStatus.OPEN.name(), IncidentStatus.IN_PROGRESS.name());

     
        notificationService.notify(staff,
                "You have been assigned to incident " + incident.getIncidentKey()
                        + " - " + incident.getTitle());
        notificationService.notify(incident.getCreatedBy(),
                "Your incident " + incident.getIncidentKey()
                        + " has been assigned to " + staff.getName());
        
    }

    
    @Override
	public void recategorize(String incidentKey, ManagerDtos.RecategorizeRequest req) {
        Incident incident = incidentRepository.findByIncidentKey(incidentKey)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentKey));
        var category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
        incident.setCategory(category);
        incident.setUpdatedAt(LocalDateTime.now());
        incidentRepository.save(incident);
    }

    
    @Override
	public CommentDtos.CommentResponse addComment(String incidentKey, String username,
                                                CommentDtos.AddCommentRequest req) {
       
    	return incidentService.addComment(username, incidentKey, req);
    }

    
    @Override
	public ManagerDtos.ReportSummary getReportSummary(String username) {
        String dept = getManager(username).getDepartment();
        LocalDateTime startOfDay = LocalDateTime.now()
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        long total    = incidentRepository.countAllByDepartment(dept);
        long today    = incidentRepository.countCreatedSinceByDepartment(dept, startOfDay);
        long breached = incidentRepository.countSlaBreachedByDepartment(dept);
        double compliance = total == 0 ? 100.0
                : Math.round(((double)(total - breached) / total) * 10000.0) / 100.0;

        return ManagerDtos.ReportSummary.builder()
        		.total(total)
                .totalToday(today)
                .openCount(incidentRepository.countByDepartmentAndStatus(dept, IncidentStatus.OPEN))
                .breachedCount(breached)
                .slaCompliance(compliance)
                .build();
    }

   
    @Override
	public List<ManagerDtos.VolumeEntry> getTicketVolume(String username, String range) {
        String dept = getManager(username).getDepartment();
        LocalDateTime now = LocalDateTime.now();
        List<ManagerDtos.VolumeEntry> result = new ArrayList<>();

        switch (range) {
            case "week" -> {
                for (int i = 6; i >= 0; i--) {
                    LocalDateTime day = now.minusDays(i);
                    LocalDateTime from = day.withHour(0).withMinute(0).withSecond(0).withNano(0);
                    LocalDateTime to   = from.plusDays(1);
                    long count = incidentRepository.countByDepartmentAndCreatedBetween(dept, from, to);
                    result.add(new ManagerDtos.VolumeEntry(day.format(DateTimeFormatter.ofPattern("EEE")), count));
                }
            }
            case "month" -> {
                for (int i = 3; i >= 0; i--) {
                    LocalDateTime weekStart = now.minusWeeks(i).with(java.time.DayOfWeek.MONDAY)
                            .withHour(0).withMinute(0).withSecond(0).withNano(0);
                    LocalDateTime weekEnd = weekStart.plusWeeks(1);
                    long count = incidentRepository.countByDepartmentAndCreatedBetween(dept, weekStart, weekEnd);
                    result.add(new ManagerDtos.VolumeEntry("Wk " + (4 - i), count));
                }
            }
            case "year" -> {
                for (int i = 11; i >= 0; i--) {
                    LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1)
                            .withHour(0).withMinute(0).withSecond(0).withNano(0);
                    LocalDateTime monthEnd = monthStart.plusMonths(1);
                    long count = incidentRepository.countByDepartmentAndCreatedBetween(dept, monthStart, monthEnd);
                    result.add(new ManagerDtos.VolumeEntry(
                            monthStart.format(DateTimeFormatter.ofPattern("MMM")), count));
                }
            }
        }
        return result;
    }

    /* ══════════════════════════════════════════════════════════
       GET /api/manager/reports/category-breakdown
    ══════════════════════════════════════════════════════════ */
    @Override
	public List<ManagerDtos.CategoryEntry> getCategoryBreakdown(String username) {
        String dept = getManager(username).getDepartment();
        return incidentRepository.countGroupByCategoryInDepartment(dept);
    }

   
    @Override
	public List<ManagerDtos.StaffSummary> getSupportStaff(String username) {
        String dept = getManager(username).getDepartment();
        return userRepository.findByRoleAndIsActive(Role.SUPPORT_STAFF,true)
                .stream()
                .map(u -> ManagerDtos.StaffSummary.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .department(u.getDepartment())
                        .build())
                .collect(Collectors.toList());
    }

  
    private ManagerDtos.IncidentDetail toIncidentDetail(Incident i) {
        return ManagerDtos.IncidentDetail.builder()
                .id(i.getId())
                .incidentKey(i.getIncidentKey())
                .title(i.getTitle())
                .description(i.getDescription())
                .priority(i.getPriority().name())
                .status(i.getStatus().name())
                .slaDueAt(i.getSlaDueAt())
                .isSlaBreached(i.isSlaBreached())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .category(i.getCategory() == null ? null :
                        ManagerDtos.CategoryRef.builder()
                                .id(i.getCategory().getId())
                                .categoryName(i.getCategory().getCategoryName())
                                .departmentName(i.getCategory().getDepartmentName())
                                .build())
                .createdBy(i.getCreatedBy() == null ? null :
                        ManagerDtos.UserRef.builder()
                                .id(i.getCreatedBy().getId())
                                .name(i.getCreatedBy().getName())
                                .build())
                .assignedTo(i.getAssignedTo() == null ? null :
                        ManagerDtos.UserRef.builder()
                                .id(i.getAssignedTo().getId())
                                .name(i.getAssignedTo().getName())
                                .build())
                .comments(commentRepository.findByIncident_IncidentKey(i.getIncidentKey()).stream()
                        .map(c -> ManagerDtos.CommentDetail.builder()
                                .id(c.getId())
                                .commentText(c.getCommentText())
                                .isInternal(c.isInternal())
                                .createdAt(c.getCreatedAt())
                                .user(c.getUser() == null ? null :
                                        ManagerDtos.UserRef.builder()
                                                .id(c.getUser().getId())
                                                .name(c.getUser().getName())
                                                .build())
                                .build())
                        .collect(Collectors.toList()))
                .attachments(attachmentRepository.findByIncidentId(i.getId()).stream()
                        .map(a -> ManagerDtos.AttachmentDetail.builder()
                                .id(a.getId())
                                .fileName(a.getFileName())
                                .fileUrl(a.getFileUrl())
                                .fileSize(a.getFileSize())
                                .contentType(a.getContentType())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
    
    @Override
	public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}