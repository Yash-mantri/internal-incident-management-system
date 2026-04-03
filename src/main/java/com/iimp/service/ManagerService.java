package com.iimp.service;

import java.util.List;

import com.iimp.dto.CommentDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.ManagerDtos;
import com.iimp.entity.User;

public interface ManagerService {

	
	List<ManagerDtos.IncidentDetail> getAllIncidentsByDepartment(String username);

	
	ManagerDtos.DepartmentStats getStatsByDepartment(String username);

	
	ManagerDtos.IncidentDetail createIncident(String username, IncidentDtos.CreateIncidentRequest req);

	
	void updateStatus(String incidentKey, ManagerDtos.UpdateStatusRequest req);

	
	void updatePriority(String incidentKey, ManagerDtos.UpdatePriorityRequest req);

	
	void assignIncident(String managerEmail, String incidentKey, ManagerDtos.AssignRequest req);

	
	void recategorize(String incidentKey, ManagerDtos.RecategorizeRequest req);

	
	CommentDtos.CommentResponse addComment(String incidentKey, String username, CommentDtos.AddCommentRequest req);

	
	ManagerDtos.ReportSummary getReportSummary(String username);

	
	List<ManagerDtos.VolumeEntry> getTicketVolume(String username, String range);

	
	List<ManagerDtos.CategoryEntry> getCategoryBreakdown(String username);

	
	List<ManagerDtos.StaffSummary> getSupportStaff(String username);

	User findUserByEmail(String email);

}