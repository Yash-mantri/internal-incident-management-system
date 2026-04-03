package com.iimp.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.iimp.dto.AttachmentDtos;
import com.iimp.dto.AuditDtos;
import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.ReportSummaryDTO;
import com.iimp.dto.ReportSummaryDTO.CategoryBreakdown;
import com.iimp.dto.SupportStaffDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.User;

public interface IncidentService {

	IncidentDtos.IncidentDetail createIncident(String creatorEmail, IncidentDtos.CreateIncidentRequest req);

	IncidentDtos.IncidentDetail getIncident(String email, String id);

	Page<IncidentDtos.IncidentSummary> listIncidents(String email, int page, int size);

	IncidentDtos.IncidentDetail assignIncident(String managerEmail, String incidentId,
			IncidentDtos.AssignIncidentRequest req);

	IncidentDtos.Status updateStatus(String userEmail, String incidentId, IncidentDtos.UpdateStatusRequest req);

	CommentDtos.CommentResponse addComment(String email, String incidentId, CommentDtos.AddCommentRequest req);

	AttachmentDtos.AttachmentResponse uploadAttachment(String username, String incidentKey, MultipartFile file) throws IOException;

	User findUserByEmail(String email);

	Incident findIncidentById(Long id);

	IncidentDtos.IncidentSummary toSummary(Incident i);

	IncidentDtos.IncidentDetail toDetail(Incident i);

	IncidentDtos.IncidentStat incidentStat(String email);

	IncidentDtos.IncidentPriorityUpdate updatePriority(String username, String id,
			IncidentDtos.IncidentPriorityUpdate req);

	Incident findIncidentById(String id);

	boolean recategorizeIncident(String username, String id, IncidentDtos.Recategorize req);

	ReportSummaryDTO getSummary();

	List<CategoryBreakdown> getCatgeoryBreakdown();

	List<AuditDtos.AuditResponse> getAuditLog(String email, String id);

	List<CommentDtos.CommentDTO> getComments(String email, String incidentKey);

	List<IncidentDtos.IncidentSummary> getIncidentByUser(String email);

	IncidentDtos.IncidentStat incidentUserStat(String email);

	SupportStaffDtos.SupportStaffStats getSupportStaffStats(String email);

	SupportStaffDtos.SupportStaffAssignedTickets getAssignedTickets(String email);

	SupportStaffDtos.SupportStaffUnreadNotifications getSupportStaffUnreadNotifications(String email);

//	 List<AttachmentDtos.AttachmentResponse> getAttachmentsByIncident(Long id);

}