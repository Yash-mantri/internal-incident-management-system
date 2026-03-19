package com.iimp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.iimp.dto.AttachmentDtos;
import com.iimp.dto.AuditDtos;
import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.UserDtos;
import com.iimp.entity.Attachment;
import com.iimp.entity.Category;
import com.iimp.entity.Incident;
import com.iimp.entity.IncidentComment;
import com.iimp.entity.SlaConfig;
import com.iimp.entity.User;
import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Role;
import com.iimp.exception.AccessDeniedException;
import com.iimp.exception.BadRequestException;
import com.iimp.exception.ResourceNotFoundException;
import com.iimp.repository.AttachmentRepository;
import com.iimp.repository.CategoryRepository;
import com.iimp.repository.IncidentAuditRepository;
import com.iimp.repository.IncidentCommentRepository;
import com.iimp.repository.IncidentRepository;
import com.iimp.repository.SlaConfigRepository;
import com.iimp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SlaConfigRepository slaConfigRepository;
    private final IncidentCommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final IncidentAuditRepository auditRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final int  MAX_FILES     = 5;

    // ── Create Incident (FR-07 to FR-11) ──────────────────────────────────────

    @Transactional
    public IncidentDtos.IncidentDetail createIncident(String creatorEmail,
                                                      IncidentDtos.CreateIncidentRequest req) {
        User creator = findUserByEmail(creatorEmail);

        Category category = categoryRepository.findByCategoryName(req.getCategory().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategory()));

        SlaConfig sla = slaConfigRepository.findByPriority(req.getPriority())
                .orElseThrow(() -> new ResourceNotFoundException("SLA config not found for: " + req.getPriority()));

        // Auto-generate incident key IIMP-YYYY-NNNNNN (FR-09)
        String incidentKey = generateIncidentKey();

        Incident incident = Incident.builder()
                .incidentKey(incidentKey)
                .title(req.getTitle())
                .description(req.getDescription())
                .status(IncidentStatus.OPEN)
                .priority(req.getPriority())
                .createdBy(creator)
                .category(category)
                .slaConfig(sla)
                .slaDueAt(LocalDateTime.now().plusHours(sla.getResolutionTimeHours()))
                .build();

        incidentRepository.save(incident);
        auditService.log(incident, creator, "CREATED", null, IncidentStatus.OPEN.name());

       
        if ("OTHERS".equalsIgnoreCase(category.getCategoryName())) {
            List<User> admins = userRepository.findByRole(Role.ADMIN);

            notificationService.sendToAll(admins,
                    "New OTHER incident " + incidentKey + " - " + req.getTitle());

        } else {
            List<User> manager = userRepository.findByDepartmentAndRole(
                    category.getDepartmentName(), Role.MANAGER);

            notificationService.sendToAll(manager,
                    "New incident " + incidentKey + " - [" + req.getPriority() + "] " + req.getTitle());
        }
        return toDetail(incident);
    }

    // ── Get Single Incident ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public IncidentDtos.IncidentDetail getIncident(String email, Long id) {
        User user = findUserByEmail(email);
        Incident incident = findIncidentById(id);
        assertCanView(user, incident);
        return toDetail(incident);
    }

    // ── List Incidents (role-scoped, paginated, filtered) (FR-12, FR-13) ─────

    @Transactional(readOnly = true)
    public  Page<IncidentDtos.IncidentSummary> listIncidents(String email,int page,int size) {
    	
        User user = findUserByEmail(email);
     
        if(user.getRole()!=Role.ADMIN) {
        	throw new  BadRequestException("Access Denied for user !!!");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        

//        Page<Incident> result = switch (user.getRole()) {
//            case EMPLOYEE -> incidentRepository.findFiltered(
//                    user.getId(), null, status, priority, category, from, to, pageable);
//            case SUPPORT_STAFF -> incidentRepository.findFiltered(
//                    null, user.getId(), status, priority, category, from, to, pageable);
//            case MANAGER -> incidentRepository.findFiltered(
//                    null, null, status, priority, category, from, to, pageable); // dept filtered below
//            case ADMIN -> incidentRepository.findFiltered(
//                    null, null, status, priority, category, from, to, pageable);
//        };
        return incidentRepository.findAll(pageable) .map(this::toSummary);
    }

    // ── Assign Incident (FR-14, FR-15) ────────────────────────────────────────

    @Transactional
    public IncidentDtos.IncidentDetail assignIncident(String managerEmail,
                                                      Long incidentId,
                                                      IncidentDtos.AssignIncidentRequest req) {
        User manager = findUserByEmail(managerEmail);
        Incident incident = findIncidentById(incidentId);

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Cannot assign a closed incident");
        }

        if ("OTHERS".equalsIgnoreCase(incident.getCategory().getCategoryName())
                && manager.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can assign OTHER incidents");
        }

        
        User staff = userRepository.findById(req.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getAssignedToUserId()));

        if (staff.getRole() != Role.SUPPORT_STAFF) {
            throw new BadRequestException("Can only assign to SUPPORT_STAFF users");
        }

        if (req.getCategory() != null) {
            Category newCategory = categoryRepository
                    .findByCategoryName(req.getCategory().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

            incident.setCategory(newCategory); 
        }
        
        String oldAssignee = incident.getAssignedTo() != null
                ? incident.getAssignedTo().getName() : "UNASSIGNED";

        incident.setAssignedTo(staff);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(incident);

        auditService.log(incident, manager, "ASSIGNED", oldAssignee, staff.getName());
        auditService.log(incident, manager, "STATUS_CHANGED",
                IncidentStatus.OPEN.name(), IncidentStatus.IN_PROGRESS.name());

        // Notify assignee and reporter (FR-26)
        notificationService.send(staff,
                "You have been assigned to incident " + incident.getIncidentKey()
                        + " - " + incident.getTitle());
        notificationService.send(incident.getCreatedBy(),
                "Your incident " + incident.getIncidentKey()
                        + " has been assigned to " + staff.getName());

        return toDetail(incident);
    }

    // ── Update Status (FR-16, FR-17) ──────────────────────────────────────────

    @Transactional
    public IncidentDtos.IncidentDetail updateStatus(String userEmail,
                                                    Long incidentId,
                                                    IncidentDtos.UpdateStatusRequest req) {
        User user = findUserByEmail(userEmail);
        Incident incident = findIncidentById(incidentId);

        validateStatusTransition(user, incident, req.getNewStatus());

        IncidentStatus oldStatus = incident.getStatus();
        incident.setStatus(req.getNewStatus());

        if (req.getNewStatus() == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        } else if (req.getNewStatus() == IncidentStatus.CLOSED) {
            incident.setClosedAt(LocalDateTime.now());
        }

        incidentRepository.save(incident);
        auditService.log(incident, user, "STATUS_CHANGED", oldStatus.name(), req.getNewStatus().name());

        if (req.getNote() != null && !req.getNote().isBlank()) {
            IncidentComment note = IncidentComment.builder()
                    .incident(incident)
                    .user(user)
                    .commentText("[Status Update] " + req.getNote())
                    .isInternal(false)
                    .build();
            commentRepository.save(note);
        }

        // Notify reporter (FR-26)
        notificationService.send(incident.getCreatedBy(),
                "Incident " + incident.getIncidentKey() + " status changed to " + req.getNewStatus());

        return toDetail(incident);
    }

    // ── Add Comment (FR-19, FR-20) ────────────────────────────────────────────

    @Transactional
    public CommentDtos.CommentResponse addComment(String email,
                                                  Long incidentId,
                                                  CommentDtos.AddCommentRequest req) {
        User user = findUserByEmail(email);
        Incident incident = findIncidentById(incidentId);
        assertCanView(user, incident);

        // Only staff/manager/admin can post internal notes
        if (req.isInternal() && user.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Employees cannot post internal comments");
        }

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .user(user)
                .commentText(req.getCommentText())
                .isInternal(req.isInternal())
                .build();
        commentRepository.save(comment);
        auditService.log(incident, user, "COMMENT_ADDED");

        // Notify all participants (FR-26)
        notifyParticipants(incident, user,
                "New comment on " + incident.getIncidentKey() + " by " + user.getName());

        return toCommentDto(comment);
    }

    // ── Upload Attachment (FR-21) ─────────────────────────────────────────────

    @Transactional
    public AttachmentDtos.AttachmentResponse uploadAttachment(String email,
                                                              Long incidentId,
                                                              MultipartFile file,
                                                              String uploadDir) throws IOException {
        User user = findUserByEmail(email);
        Incident incident = findIncidentById(incidentId);
        assertCanView(user, incident);

        // Validate file count
        if (attachmentRepository.countByIncidentId(incidentId) >= MAX_FILES) {
            throw new BadRequestException("Maximum " + MAX_FILES + " attachments per ticket");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File exceeds 10 MB limit");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Unsupported file type. Allowed: PDF, JPG, PNG, DOCX, XLSX");
        }

        // Save file to disk
        Path uploadPath = Paths.get(uploadDir, incidentId.toString());
        Files.createDirectories(uploadPath);
        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Attachment attachment = Attachment.builder()
                .incident(incident)
                .fileName(file.getOriginalFilename())
                .fileUrl("/api/incidents/" + incidentId + "/attachments/" + storedName)
                .fileSize(file.getSize())
                .contentType(contentType)
                .uploadedBy(user)
                .build();
        attachmentRepository.save(attachment);
        auditService.log(incident, user, "ATTACHMENT_ADDED", null, file.getOriginalFilename());

        return AttachmentDtos.AttachmentResponse.builder()
                .id(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getFileUrl())
                .fileSize(attachment.getFileSize())
                .contentType(attachment.getContentType())
                .uploadedByName(user.getName())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    // ── Helper: Status Transition Validation (FR-16) ──────────────────────────

    private void validateStatusTransition(User user, Incident incident, IncidentStatus newStatus) {
    	 if ("OTHERS".equalsIgnoreCase(incident.getCategory().getCategoryName())
    	            && user.getRole() != Role.ADMIN) {
    	        throw new AccessDeniedException("Only ADMIN can update OTHER incidents");
    	    }
    	 
        IncidentStatus current = incident.getStatus();
        
        boolean allowed = switch (user.getRole()) {
            case SUPPORT_STAFF -> current == IncidentStatus.IN_PROGRESS
                    && newStatus == IncidentStatus.RESOLVED;
            case MANAGER -> (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.CLOSED)
                    || (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.IN_PROGRESS);
            case ADMIN -> current != IncidentStatus.CLOSED; // Admin can do any except re-open closed
            case EMPLOYEE -> false;
		default -> throw new IllegalArgumentException("Unexpected value: " + user.getRole());
        };
        
        if (!allowed) {
            throw new AccessDeniedException(
                    "Role " + user.getRole() + " cannot transition from " + current + " to " + newStatus);
        }
    }

    private void assertCanView(User user, Incident incident) {
    	 if ("OTHERS".equalsIgnoreCase(incident.getCategory().getCategoryName())) {
    	        if (user.getRole() != Role.ADMIN) {
    	            throw new AccessDeniedException("Only ADMIN can view OTHER incidents");
    	        }
    	    }
        boolean canView = switch (user.getRole()) {
            case ADMIN -> true;
            case MANAGER -> incident.getCategory().getDepartmentName()
                    .equals(user.getDepartment());
            case SUPPORT_STAFF -> incident.getAssignedTo() != null
                    && incident.getAssignedTo().getId().equals(user.getId());
            case EMPLOYEE -> incident.getCreatedBy().getId().equals(user.getId());
//		case OTHERS -> throw new UnsupportedOperationException("Unimplemented case: " + user.getRole());
		default -> throw new IllegalArgumentException("Unexpected value: " + user.getRole());
        };
        if (!canView) throw new AccessDeniedException("You do not have access to this incident");
    }

    private void notifyParticipants(Incident incident, User actor, String message) {
        List<User> participants = new java.util.ArrayList<>();
        if (!incident.getCreatedBy().getId().equals(actor.getId()))
            participants.add(incident.getCreatedBy());
        if (incident.getAssignedTo() != null
                && !incident.getAssignedTo().getId().equals(actor.getId()))
            participants.add(incident.getAssignedTo());
        notificationService.sendToAll(participants, message);
    }

    // ── Key Generator ─────────────────────────────────────────────────────────

    private String generateIncidentKey() {
        long count = incidentRepository.count() + 1;
        return String.format("IIMP-%d-%06d", Year.now().getValue(), count);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    public Incident findIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    public IncidentDtos.IncidentSummary toSummary(Incident i) {
        return IncidentDtos.IncidentSummary.builder()
                .id(i.getId())
                .incidentKey(i.getIncidentKey())
                .title(i.getTitle())
                .category(i.getCategory().getCategoryName())
                .priority(i.getPriority())
                .status(i.getStatus())
                .createdByName(i.getCreatedBy().getName())
                .assignedToName(i.getAssignedTo() != null ? i.getAssignedTo().getName() : null)
                .createdAt(i.getCreatedAt())
                .slaDueAt(i.getSlaDueAt())
                .slaBreached(i.isSlaBreached())
                .build();
    }

    public IncidentDtos.IncidentDetail toDetail(Incident i) {
        List<CommentDtos.CommentResponse> comments =
                commentRepository.findByIncidentIdOrderByCreatedAtAsc(i.getId())
                        .stream().map(this::toCommentDto).toList();

        List<AttachmentDtos.AttachmentResponse> attachments =
                attachmentRepository.findByIncidentId(i.getId())
                        .stream().map(a -> AttachmentDtos.AttachmentResponse.builder()
                                .id(a.getId())
                                .fileName(a.getFileName())
                                .fileUrl(a.getFileUrl())
                                .fileSize(a.getFileSize())
                                .contentType(a.getContentType())
                                .uploadedByName(a.getUploadedBy().getName())
                                .createdAt(a.getCreatedAt())
                                .build()).toList();

        List<AuditDtos.AuditResponse> audit =
                auditRepository.findByIncidentIdOrderByCreatedAtAsc(i.getId())
                        .stream().map(a -> AuditDtos.AuditResponse.builder()
                                .id(a.getId())
                                .action(a.getAction())
                                .oldValue(a.getOldValue())
                                .newValue(a.getNewValue())
                                .changedByName(a.getChangedBy().getName())
                                .createdAt(a.getCreatedAt())
                                .build()).toList();

        return IncidentDtos.IncidentDetail.builder()
                .id(i.getId())
                .incidentKey(i.getIncidentKey())
                .title(i.getTitle())
                .description(i.getDescription())
                .category(i.getCategory().getCategoryName())
                .priority(i.getPriority())
                .status(i.getStatus())
                .createdBy(toUserSummary(i.getCreatedBy()))
                .assignedTo(i.getAssignedTo() != null ? toUserSummary(i.getAssignedTo()) : null)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .resolvedAt(i.getResolvedAt())
                .closedAt(i.getClosedAt())
                .slaDueAt(i.getSlaDueAt())
                .slaBreached(i.isSlaBreached())
                .comments(comments)
                .attachments(attachments)
                .auditHistory(audit)
                .build();
    }

    private CommentDtos.CommentResponse toCommentDto(IncidentComment c) {
        return CommentDtos.CommentResponse.builder()
                .id(c.getId())
                .commentText(c.getCommentText())
                .authorName(c.getUser().getName())
                .authorRole(c.getUser().getRole().name())
                .internal(c.isInternal())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private UserDtos.UserSummary toUserSummary(User u) {
        return UserDtos.UserSummary.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .department(u.getDepartment())
                .role(u.getRole().name())
                .build();
    }

	public IncidentDtos.IncidentStat incidentStat(String email) {
		 long todayTotal  = incidentRepository.countCreatedSince(LocalDateTime.now().withHour(0).withMinute(0));
	        long totalAll    = incidentRepository.count();
	        long open        = incidentRepository.countByStatus(IncidentStatus.OPEN);
	        long inProgress  = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
	        long resolved    = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
	        long closed      = incidentRepository.countByStatus(IncidentStatus.CLOSED);
	        long slaBreached = incidentRepository.countSlaBreached();
  

	        return IncidentDtos.IncidentStat.builder()
	                .todayTotal(todayTotal)
	                .totalAll(totalAll)
	                .open(open)
	                .inProgress(inProgress)
	                .resolved(resolved)
	                .closed(closed)
	                .slaBreached(slaBreached)
	                .build();
	}


}
