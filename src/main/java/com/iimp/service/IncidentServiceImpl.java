package com.iimp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.iimp.dto.AttachmentDtos;
import com.iimp.dto.AuditDtos;
import com.iimp.dto.AuditDtos.AuditResponse;
import com.iimp.dto.CommentDtos;
import com.iimp.dto.CommentDtos.CommentUserDTO;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.NotificationDtos;
import com.iimp.dto.ReportSummaryDTO;
import com.iimp.dto.ReportSummaryDTO.CategoryBreakdown;
import com.iimp.dto.SupportStaffDtos;
import com.iimp.dto.UserDtos;
import com.iimp.entity.Attachment;
import com.iimp.entity.Category;
import com.iimp.entity.Incident;
import com.iimp.entity.IncidentAudit;
import com.iimp.entity.IncidentComment;
import com.iimp.entity.SlaConfig;
import com.iimp.entity.User;
import com.iimp.enums.EventType;
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
import com.iimp.util.BusinessHoursCalculator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

	private final IncidentRepository incidentRepository;
	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final SlaConfigRepository slaConfigRepository;
	private final IncidentCommentRepository commentRepository;
	private final AttachmentRepository attachmentRepository;
	private final IncidentAuditRepository auditRepository;
	private final NotificationService notificationService;
	private final AuditService auditService;
	private final BusinessHoursCalculator businessHoursCalc;
	private static final List<String> ALLOWED_TYPES = List.of("application/pdf", "image/jpeg", "image/png",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
	private static final int MAX_FILES = 5;

	@Override
	@Transactional
	public IncidentDtos.IncidentDetail createIncident(String creatorEmail, IncidentDtos.CreateIncidentRequest req) {
		User creator = findUserByEmail(creatorEmail);

		Category category = categoryRepository.findByCategoryName(req.getCategory())
				.orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategory()));
		System.out.println(category.getCategoryName());
		SlaConfig sla = slaConfigRepository.findByPriority(req.getPriority())
				.orElseThrow(() -> new ResourceNotFoundException("SLA config not found for: " + req.getPriority()));

		String incidentKey = generateIncidentKey(category.getDepartmentName());
        
		LocalDateTime slaDueAt = businessHoursCalc.calcDeadline(
				LocalDateTime.now(),
				sla.getResolutionTimeHours(),
				null  // assignee unknown at creation — no leave skip yet
		);
		
		Incident incident = Incident.builder().incidentKey(incidentKey).title(req.getTitle())
				.description(req.getDescription()).status(IncidentStatus.OPEN).priority(req.getPriority())
				.createdBy(creator).category(category).slaConfig(sla)
				.slaDueAt(slaDueAt).build();

		incidentRepository.save(incident);
		auditService.log(incident, creator, "CREATED", null, IncidentStatus.OPEN.name());

		if ("OTHERS".equalsIgnoreCase(category.getCategoryName())) {
			List<User> admins = userRepository.findByRole(Role.ADMIN);

			notificationService.notifyall(admins, "New OTHER incident " + incidentKey + " - " + req.getTitle());

		} else {
			List<User> manager = userRepository.findByDepartmentAndRole(category.getDepartmentName(), Role.MANAGER);

			notificationService.notifyall(manager,
					"New incident " + incidentKey + " - [" + req.getPriority() + "] " + req.getTitle());
		}
		return toDetail(incident);
	}

	@Override
	@Transactional(readOnly = true)
	public IncidentDtos.IncidentDetail getIncident(String email, String id) {
		User user = findUserByEmail(email);
		Incident incident = findIncidentById(id);
		assertCanView(user, incident);
		return toDetail(incident);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<IncidentDtos.IncidentSummary> listIncidents(String email, int page, int size) {

		User user = findUserByEmail(email);

		if (user.getRole() != Role.ADMIN) {
			throw new BadRequestException("Access Denied for user !!!");
		}

		Pageable pageable = PageRequest.of(page, size);

		return incidentRepository.findAll(pageable).map(this::toSummary);
	}

	@Override
	@Transactional
	public IncidentDtos.IncidentDetail assignIncident(String managerEmail, String incidentId,
			IncidentDtos.AssignIncidentRequest req) {
		User manager = findUserByEmail(managerEmail);
		Incident incident = findIncidentById(incidentId);

		if (incident.getStatus() == IncidentStatus.CLOSED) {
			throw new BadRequestException("Cannot assign a closed incident");
		}

		if ("OTHERS".equalsIgnoreCase(incident.getCategory().getCategoryName()) && manager.getRole() != Role.ADMIN) {
			throw new AccessDeniedException("Only ADMIN can assign OTHER incidents");
		}

		User staff = userRepository.findById(req.getAssignedToUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getAssignedToUserId()));

		if (staff.getRole() != Role.SUPPORT_STAFF) {
			throw new BadRequestException("Can only assign to SUPPORT_STAFF users");
		}

		if (req.getCategory() != null) {
			Category newCategory = categoryRepository.findByCategoryName(req.getCategory())
					.orElseThrow(() -> new ResourceNotFoundException("Category not found"));

			incident.setCategory(newCategory);
		}

		String oldAssignee = incident.getAssignedTo() != null ? incident.getAssignedTo().getName() : "UNASSIGNED";

		incident.setAssignedTo(staff);
		incident.setStatus(IncidentStatus.IN_PROGRESS);
		
		double slaHours = incident.getSlaConfig() != null
				? incident.getSlaConfig().getResolutionTimeHours()
				: 24.0;
 
		LocalDateTime updatedSlaDueAt = businessHoursCalc.calcDeadline(
				incident.getCreatedAt(), 
				slaHours,
				staff                     
		);
 
		incident.setSlaDueAt(updatedSlaDueAt);
		
		incidentRepository.save(incident);

		auditService.log(incident, manager, "ASSIGNED", oldAssignee, staff.getName());
		auditService.log(incident, manager, "STATUS_CHANGED", IncidentStatus.OPEN.name(),
				IncidentStatus.IN_PROGRESS.name());

		notificationService.send(staff, incident, EventType.INCIDENT_ASSIGNED);

		return toDetail(incident);
	}

	private Incident findIncidentByIncidentKey(String id) {

		return incidentRepository.findByIncidentKey(id).get();
	}

	@Override
	@Transactional
	public IncidentDtos.Status updateStatus(String userEmail, String incidentId, IncidentDtos.UpdateStatusRequest req) {
		User user = findUserByEmail(userEmail);
		Incident incident = findIncidentByIncidentKey(incidentId);

		validateStatusTransition(user, incident, req.getNewStatus());

		IncidentStatus oldStatus = incident.getStatus();
		incident.setStatus(req.getNewStatus());

		if (req.getNewStatus() == IncidentStatus.RESOLVED) {

			incident.setResolvedAt(LocalDateTime.now());
			incident.setStatus(IncidentStatus.RESOLVED);
		} else if (req.getNewStatus() == IncidentStatus.CLOSED) {
			incident.setClosedAt(LocalDateTime.now());
			incident.setStatus(IncidentStatus.CLOSED);
		}

		incidentRepository.save(incident);
		auditService.log(incident, user, "STATUS_CHANGED", oldStatus.name(), req.getNewStatus().name());

		if (req.getNote() != null && !req.getNote().isBlank()) {
			IncidentComment note = IncidentComment.builder().incident(incident).user(user)
					.commentText("[Status Update] " + req.getNote()).isInternal(false).build();
			commentRepository.save(note);
		}

		notificationService.send(user, incident, EventType.INCIDENT_STATUS_CHANGED);

		return IncidentDtos.Status.builder().id(incident.getIncidentKey()).newStatus(incident.getStatus()).build();
	}

	@Override
	@Transactional
	public CommentDtos.CommentResponse addComment(String email, String incidentId, CommentDtos.AddCommentRequest req) {
		User user = findUserByEmail(email);
		Incident incident = findIncidentById(incidentId);
		assertCanView(user, incident);

		if (req.isInternal() && user.getRole() == Role.EMPLOYEE) {
			throw new AccessDeniedException("Employees cannot post internal comments");
		}

		IncidentComment comment = IncidentComment.builder().incident(incident).user(user)
				.commentText(req.getCommentText()).isInternal(req.isInternal()).build();
		commentRepository.save(comment);
		auditService.log(incident, user, "COMMENT_ADDED");

		notifyParticipants(incident, user, "New comment on " + incident.getIncidentKey() + " by " + user.getName());

		return toCommentDto(comment);
	}

	private final String uploadDir = System.getProperty("user.dir") + "/uploads";

	@Override
	@Transactional
	public AttachmentDtos.AttachmentResponse uploadAttachment(String username, String incidentKey, MultipartFile file)
			throws IOException {

		if (file.isEmpty()) {
			throw new RuntimeException("File is empty");
		}

		Incident incident = incidentRepository.findByIncidentKey(incidentKey)
				.orElseThrow(() -> new RuntimeException("Incident not found"));

		User user = userRepository.findByEmail(username).orElseThrow(() -> new RuntimeException("User not found"));

		String contentType = file.getContentType();
		if (contentType == null || (!contentType.equals("application/pdf") && !contentType.equals("image/png")
				&& !contentType.equals("image/jpeg"))) {
			throw new RuntimeException("Invalid file type");
		}

		String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

		Path uploadPath = Paths.get(uploadDir);
		Path filePath = uploadPath.resolve(fileName);

		Files.createDirectories(uploadPath);

		file.transferTo(filePath.toFile());

		String fileUrl = "/uploads/" + fileName;

		Attachment attachment = Attachment.builder().incident(incident).fileName(fileName).fileUrl(fileUrl)
				.fileSize(file.getSize()).contentType(file.getContentType()).uploadedBy(user).build();

		attachmentRepository.save(attachment);

		return AttachmentDtos.AttachmentResponse.builder().fileName(fileName).fileUrl(fileUrl).build();
	}
	
	@Transactional
	public void recalcSlaDeadlinesForStaff(User staff) {
		List<Incident> openIncidents = incidentRepository.findOpenIncidentsByAssignee(staff);
 
		if (openIncidents.isEmpty()) return;
 
		for (Incident incident : openIncidents) {
			double slaHours = incident.getSlaConfig() != null
					? incident.getSlaConfig().getResolutionTimeHours()
					: 24.0;
 
			
			LocalDateTime newDeadline = businessHoursCalc.calcDeadline(
					incident.getCreatedAt(),
					slaHours,
					staff
			);
 
			incident.setSlaDueAt(newDeadline);
		}
 
		incidentRepository.saveAll(openIncidents);
	}

	private void validateStatusTransition(User user, Incident incident, IncidentStatus newStatus) {
		if ("OTHERS".equalsIgnoreCase(incident.getCategory().getCategoryName()) && user.getRole() != Role.ADMIN) {
			throw new AccessDeniedException("Only ADMIN can update OTHER incidents");
		}

		IncidentStatus current = incident.getStatus();

		boolean allowed = switch (user.getRole()) {
		case SUPPORT_STAFF -> current == IncidentStatus.IN_PROGRESS && newStatus == IncidentStatus.RESOLVED;
		case MANAGER -> (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.CLOSED)
				|| (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.IN_PROGRESS);
		case ADMIN -> current != IncidentStatus.CLOSED; // Admin can do any except re-open closed
		case EMPLOYEE -> (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.CLOSED)
				|| (current == IncidentStatus.RESOLVED && newStatus == IncidentStatus.IN_PROGRESS);
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
		case MANAGER -> incident.getCategory().getDepartmentName().equals(user.getDepartment());
		case SUPPORT_STAFF -> incident.getAssignedTo() != null && incident.getAssignedTo().getId().equals(user.getId());
		case EMPLOYEE -> incident.getCreatedBy().getId().equals(user.getId());

		default -> throw new IllegalArgumentException("Unexpected value: " + user.getRole());
		};
		if (!canView)
			throw new AccessDeniedException("You do not have access to this incident");
	}

	private void notifyParticipants(Incident incident, User actor, String message) {
		List<User> participants = new java.util.ArrayList<>();
		if (!incident.getCreatedBy().getId().equals(actor.getId()))
			participants.add(incident.getCreatedBy());
		if (incident.getAssignedTo() != null && !incident.getAssignedTo().getId().equals(actor.getId()))
			participants.add(incident.getAssignedTo());
		notificationService.notifyall(participants, message);
	}

	private String generateIncidentKey(String department) {
		long count = incidentRepository.count() + 1;
		return String.format("TKT-%s-%06d", department, count);
	}

	@Override
	public User findUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
	}

	@Override
	public Incident findIncidentById(Long id) {
		return incidentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
	}

	@Override
	public IncidentDtos.IncidentSummary toSummary(Incident i) {
		List<AttachmentDtos.AttachmentResponse> attachments = attachmentRepository.findByIncidentId(i.getId()).stream()
				.map(a -> AttachmentDtos.AttachmentResponse.builder().id(a.getId()).fileName(a.getFileName())
						.fileUrl(a.getFileUrl()).fileSize(a.getFileSize()).contentType(a.getContentType())
						.uploadedByName(a.getUploadedBy().getName()).createdAt(a.getCreatedAt()).build())
				.toList();

		return IncidentDtos.IncidentSummary.builder().id(i.getId()).incidentKey(i.getIncidentKey()).title(i.getTitle())
				.category(i.getCategory().getCategoryName()).department(i.getCategory().getDepartmentName())
				.priority(i.getPriority()).description(i.getDescription()).status(i.getStatus())
				.createdByName(i.getCreatedBy().getName())
				.assignedToName(i.getAssignedTo() != null ? i.getAssignedTo().getName() : null)
				.createdAt(i.getCreatedAt()).attachments(attachments).slaDueAt(i.getSlaDueAt())
				.slaBreached(i.isSlaBreached()).build();
	}

	@Override
	public IncidentDtos.IncidentDetail toDetail(Incident i) {
		List<CommentDtos.CommentResponse> comments = commentRepository.findByIncidentIdOrderByCreatedAtAsc(i.getId())
				.stream().map(this::toCommentDto).toList();

		List<AttachmentDtos.AttachmentResponse> attachments = attachmentRepository.findByIncidentId(i.getId()).stream()
				.map(a -> AttachmentDtos.AttachmentResponse.builder().id(a.getId()).fileName(a.getFileName())
						.fileUrl(a.getFileUrl()).fileSize(a.getFileSize()).contentType(a.getContentType())
						.uploadedByName(a.getUploadedBy().getName()).createdAt(a.getCreatedAt()).build())
				.toList();

		List<AuditDtos.AuditResponse> audit = auditRepository.findByIncidentIdOrderByCreatedAtAsc(i.getId()).stream()
				.map(a -> AuditDtos.AuditResponse.builder().id(a.getId()).action(a.getAction())
						.oldValue(a.getOldValue()).newValue(a.getNewValue()).changedByName(a.getChangedBy().getName())
						.createdAt(a.getCreatedAt()).build())
				.toList();

		return IncidentDtos.IncidentDetail.builder().id(i.getId()).incidentKey(i.getIncidentKey()).title(i.getTitle())
				.description(i.getDescription()).category(i.getCategory().getCategoryName()).priority(i.getPriority())
				.status(i.getStatus()).createdBy(toUserSummary(i.getCreatedBy()))
				.assignedTo(i.getAssignedTo() != null ? toUserSummary(i.getAssignedTo()) : null)
				.createdAt(i.getCreatedAt()).updatedAt(i.getUpdatedAt()).resolvedAt(i.getResolvedAt())
				.closedAt(i.getClosedAt()).slaDueAt(i.getSlaDueAt()).slaBreached(i.isSlaBreached()).comments(comments)
				.attachments(attachments).auditHistory(audit).build();
	}

	private CommentDtos.CommentResponse toCommentDto(IncidentComment c) {
		return CommentDtos.CommentResponse.builder().id(c.getId()).commentText(c.getCommentText())
				.authorName(c.getUser().getName()).authorRole(c.getUser().getRole().name()).internal(c.isInternal())
				.createdAt(c.getCreatedAt()).build();
	}

	private UserDtos.UserSummary toUserSummary(User u) {
		return UserDtos.UserSummary.builder().id(u.getId()).name(u.getName()).email(u.getEmail())
				.department(u.getDepartment()).role(u.getRole().name()).build();
	}

	@Override
	public IncidentDtos.IncidentStat incidentStat(String email) {
		long todayTotal = incidentRepository.countCreatedSince(LocalDateTime.now().withHour(0).withMinute(0));
		long totalAll = incidentRepository.count();
		long open = incidentRepository.countByStatus(IncidentStatus.OPEN);
		long inProgress = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
		long resolved = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
		long closed = incidentRepository.countByStatus(IncidentStatus.CLOSED);
		long slaBreached = incidentRepository.countSlaBreached();

		return IncidentDtos.IncidentStat.builder().todayTotal(todayTotal).totalAll(totalAll).open(open)
				.inProgress(inProgress).resolved(resolved).closed(closed).slaBreached(slaBreached).build();
	}

	@Override
	public IncidentDtos.IncidentPriorityUpdate updatePriority(String username, String id,
			@Valid IncidentDtos.IncidentPriorityUpdate req) {
		Incident incident = findIncidentById(id);
		incident.setPriority(req.getPriority());
		return IncidentDtos.IncidentPriorityUpdate.builder().id(incident.getId()).priority(incident.getPriority())
				.build();
	}

	@Override
	public Incident findIncidentById(String id) {
		return incidentRepository.findByIncidentKey(id)
				.orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
	}

	@Override
	@Transactional
	public boolean recategorizeIncident(String username, String id, @Valid IncidentDtos.Recategorize req) {
		// TODO Auto-generated method stub
		Incident incident = findIncidentById(id);
		Category category = categoryRepository.findById(req.getCategoryId()).get();
		incident.setCategory(category);
		return true;

	}

	@Override
	public ReportSummaryDTO getSummary() { // CHANGES
		long onTime = incidentRepository.countResolvedOnTime();
		long total = incidentRepository.countResolvedWithSla();
		long breached = incidentRepository.countBreached(LocalDateTime.now());

		double compliance = total > 0 ? Math.round((onTime * 100.0 / total) * 10.0) / 10.0 : 100.0;

		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = startOfDay.plusDays(1);
		long totalToday = incidentRepository.countCreatedToday(startOfDay, endOfDay);

		return ReportSummaryDTO.builder().totalToday(totalToday)
				.openCount(incidentRepository.countByStatus(IncidentStatus.OPEN)).breachedCount(breached)
				.slaCompliance(compliance).build();
	}

	@Override
	public List<CategoryBreakdown> getCatgeoryBreakdown() { // CHANGES
		return incidentRepository.countGroupedByDepartment().stream().map(row -> {
			CategoryBreakdown cat = new CategoryBreakdown();
			cat.setLabel((String) row[0]);
			cat.setCount(((Number) row[1]).intValue());
			return cat;
		}).collect(Collectors.toList());

	}

	@Override
	public List<AuditDtos.AuditResponse> getAuditLog(String email, String id) {
		// TODO Auto-generated method stub
		User user = findUserByEmail(email);
		List<IncidentAudit> auditLog = auditRepository.findByIncident_IncidentKey(id);
		return auditLog.stream()
				.map(a -> AuditResponse.builder().id(a.getId()).action(a.getAction()).oldValue(a.getOldValue())
						.newValue(a.getNewValue()).changedBy(user.getId()) // user id
						.changedByName(user.getName()) // user name
						.createdAt(a.getCreatedAt()).build())
				.toList();
	}

	@Override
	public List<CommentDtos.CommentDTO> getComments(String email, String incidentKey) {
		User user = findUserByEmail(email); // 🔥 ADD THIS

		List<IncidentComment> comments = commentRepository.findByIncident_IncidentKey(incidentKey);

		return comments.stream()
				// 🔥 IMPORTANT FILTER
				.filter(c -> {
					// public comment → everyone can see
					if (!c.isInternal())
						return true;

					// internal comment → only admin/manager/support
					return user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER
							|| user.getRole() == Role.SUPPORT_STAFF;
				})
				.map(c -> CommentDtos.CommentDTO.builder().id(c.getId()).commentText(c.getCommentText())
						.isInternal(c.isInternal()).createdAt(c.getCreatedAt())
						.user(new CommentUserDTO(c.getUser().getName())).build())
				.toList();
	}

	@Override
	public List<IncidentDtos.IncidentSummary> getIncidentByUser(String email) {
		return incidentRepository.findByCreatedBy_EmailOrderByCreatedAtDesc(email).stream().map(this::toSummary).toList();

	}

	@Override
	public IncidentDtos.IncidentStat incidentUserStat(String email) {
		long totalAll = incidentRepository.countByCreatedBy_Email(email);
		long open = incidentRepository.countByCreatedBy_EmailAndStatus(email, IncidentStatus.OPEN);
		long inProgress = incidentRepository.countByCreatedBy_EmailAndStatus(email, IncidentStatus.IN_PROGRESS);
		long resolved = incidentRepository.countByCreatedBy_EmailAndStatus(email, IncidentStatus.RESOLVED);
		long closed = incidentRepository.countByCreatedBy_EmailAndStatus(email, IncidentStatus.CLOSED);
//		long breached = incidentRepository.countCreatedBy_EmailAndBreached(email,LocalDateTime.now());
		return IncidentDtos.IncidentStat.builder().totalAll(totalAll).open(open).inProgress(inProgress)
				.resolved(resolved).closed(closed).build();
	}

	@Override
	public SupportStaffDtos.SupportStaffStats getSupportStaffStats(String email) {
		User user = findUserByEmail(email);
		long open = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.IN_PROGRESS);
		long inProgress = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.IN_PROGRESS);
		long resolved = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.RESOLVED);

		return SupportStaffDtos.SupportStaffStats.builder().assignedOpenCount(open).assignedInProgressCount(inProgress)
				.assignedResolvedCount(resolved).build();
	}

	@Override
	public SupportStaffDtos.SupportStaffAssignedTickets getAssignedTickets(String email) {
		User user = findUserByEmail(email);
		var assigned = incidentRepository
				.findByAssignedTo(user, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent()
				.stream().map(this::toSummary).toList();

		return SupportStaffDtos.SupportStaffAssignedTickets.builder().assignedTickets(assigned).build();
	}

	@Override
	public SupportStaffDtos.SupportStaffUnreadNotifications getSupportStaffUnreadNotifications(String email) {
		// TODO Auto-generated method stub
		User user = findUserByEmail(email);
		List<NotificationDtos.NotificationResponse> notifications = notificationService.getUnread(user.getId());

		return SupportStaffDtos.SupportStaffUnreadNotifications.builder().unreadNotifications(notifications).build();
	}

}
