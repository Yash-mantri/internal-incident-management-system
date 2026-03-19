package com.iimp.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.dto.DashboardDtos;
import com.iimp.dto.UserDtos;
import com.iimp.entity.User;
import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Role;
import com.iimp.repository.IncidentRepository;
import com.iimp.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentService incidentService;
    private final NotificationService notificationService;
    private final UserService userService;

    // ══════════════════════════════════════════════════════
    //  EMPLOYEE DASHBOARD
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardDtos.EmployeeDashboard getEmployeeDashboard(String email) {
        User user = incidentService.findUserByEmail(email);

        long open       = incidentRepository.countByCreatedByAndStatus(user, IncidentStatus.OPEN);
        long inProgress = incidentRepository.countByCreatedByAndStatus(user, IncidentStatus.IN_PROGRESS);
        long resolved   = incidentRepository.countByCreatedByAndStatus(user, IncidentStatus.RESOLVED);
        long closed     = incidentRepository.countByCreatedByAndStatus(user, IncidentStatus.CLOSED);

        var recent = incidentRepository
                .findByCreatedBy(user, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(incidentService::toSummary).toList();

        var notifications = notificationService.getUnread(user.getId());

        return DashboardDtos.EmployeeDashboard.builder()
                .myOpenCount(open)
                .myInProgressCount(inProgress)
                .myResolvedCount(resolved)
                .myClosedCount(closed)
                .totalMyTickets(open + inProgress + resolved + closed)
                .recentTickets(recent)
                .unreadNotifications(notifications)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  SUPPORT STAFF DASHBOARD
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardDtos.SupportDashboard getSupportDashboard(String email) {
        User user = incidentService.findUserByEmail(email);

        long open       = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.IN_PROGRESS);
        long inProgress = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.IN_PROGRESS);
        long resolved   = incidentRepository.countByAssignedToAndStatus(user, IncidentStatus.RESOLVED);

        var assigned = incidentRepository
                .findByAssignedTo(user, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(incidentService::toSummary).toList();

        var notifications = notificationService.getUnread(user.getId());

        return DashboardDtos.SupportDashboard.builder()
                .assignedOpenCount(open)
                .assignedInProgressCount(inProgress)
                .assignedResolvedCount(resolved)
                .assignedTickets(assigned)
                .unreadNotifications(notifications)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  MANAGER DASHBOARD
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardDtos.ManagerDashboard getManagerDashboard(String email) {
        User manager = incidentService.findUserByEmail(email);
        String dept = manager.getDepartment();

        long total      = incidentRepository.countByDept(dept);
        long open       = incidentRepository.countByDeptAndStatus(dept, IncidentStatus.OPEN);
        long inProgress = incidentRepository.countByDeptAndStatus(dept, IncidentStatus.IN_PROGRESS);
        long resolved   = incidentRepository.countByDeptAndStatus(dept, IncidentStatus.RESOLVED);
        long closed     = incidentRepository.countByDeptAndStatus(dept, IncidentStatus.CLOSED);
        long slaBreached = incidentRepository.countSlaBreachedByDept(dept);

        // Team workload — assigned_to counts per staff member in dept
        List<Object[]> workloadRaw = incidentRepository.teamWorkloadByDept(dept);
        Map<Long, Long> workloadMap = new HashMap<>();
        for (Object[] row : workloadRaw) {
            workloadMap.put((Long) row[0], (Long) row[1]);
        }

        List<DashboardDtos.TeamMemberStat> teamWorkload =
                userRepository.findByDepartmentAndRole(dept, Role.SUPPORT_STAFF).stream()
                        .map(u -> DashboardDtos.TeamMemberStat.builder()
                                .userId(u.getId())
                                .name(u.getName())
                                .email(u.getEmail())
                                .assignedCount(workloadMap.getOrDefault(u.getId(), 0L))
                                .resolvedCount(incidentRepository.countByAssignedToAndStatus(u, IncidentStatus.RESOLVED))
                                .build())
                        .toList();

        // Top requesters in this dept
        List<DashboardDtos.RequesterStat> topRequesters =
                userRepository.findByDepartmentAndRole(dept, Role.EMPLOYEE).stream()
                        .map(u -> DashboardDtos.RequesterStat.builder()
                                .userId(u.getId())
                                .name(u.getName())
                                .totalRequests(incidentRepository.countByCreatedBy(u))
                                .openRequests(incidentRepository.countByCreatedByAndStatus(u, IncidentStatus.OPEN))
                                .resolvedRequests(incidentRepository.countByCreatedByAndStatus(u, IncidentStatus.RESOLVED))
                                .build())
                        .filter(s -> s.getTotalRequests() > 0)
                        .sorted(Comparator.comparingLong(DashboardDtos.RequesterStat::getTotalRequests).reversed())
                        .limit(10)
                        .toList();

        var recent = incidentRepository
                .findByDepartment(dept, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(incidentService::toSummary).toList();

        var notifications = notificationService.getUnread(manager.getId());

        return DashboardDtos.ManagerDashboard.builder()
                .deptTotalTickets(total)
                .deptOpenCount(open)
                .deptInProgressCount(inProgress)
                .deptResolvedCount(resolved)
                .deptClosedCount(closed)
                .deptSlaBreachedCount(slaBreached)
                .teamWorkload(teamWorkload)
                .topRequesters(topRequesters)
                .recentTickets(recent)
                .unreadNotifications(notifications)
                .build();
    }

    // ══════════════════════════════════════════════════════
    //  ADMIN DASHBOARD
    // ══════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public DashboardDtos.AdminDashboard getAdminDashboard(String email) {
        User admin = incidentService.findUserByEmail(email);

//        long totalUsers  = userRepository.count();
//        long activeUsers = userRepository.findByIsActiveTrue().size();

        long todayTotal  = incidentRepository.countCreatedSince(LocalDateTime.now().withHour(0).withMinute(0));
        long totalAll    = incidentRepository.count();
        long open        = incidentRepository.countByStatus(IncidentStatus.OPEN);
        long inProgress  = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
        long resolved    = incidentRepository.countByStatus(IncidentStatus.RESOLVED);
        long closed      = incidentRepository.countByStatus(IncidentStatus.CLOSED);
        long slaBreached = incidentRepository.countSlaBreached();

        double slaCompliance = totalAll > 0
                ? Math.round((1.0 - (double) slaBreached / totalAll) * 10000.0) / 100.0
                : 100.0;

        // Tickets by category
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : incidentRepository.countByCategory()) {
            byCategory.put((String) row[0], (Long) row[1]);
        }

        // Tickets by priority
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : incidentRepository.countByPriority()) {
            byPriority.put(row[0].toString(), (Long) row[1]);
        }

//        // All users for admin panel
//        List<UserDtos.UserDetail> allUsers = userRepository.findAll()
//                .stream().map(userService::toDetail).toList();

        var recent = incidentRepository.findTop10ByOrderByCreatedAtDesc()
                .stream().map(incidentService::toSummary).toList();

        var notifications = notificationService.getUnread(admin.getId());

        return DashboardDtos.AdminDashboard.builder()
//                .totalUsers(totalUsers)
//                .activeUsers(activeUsers)
                .totalTicketsToday(todayTotal)
                .totalTickets(totalAll)
                .openCount(open)
                .inProgressCount(inProgress)
                .resolvedCount(resolved)
                .closedCount(closed)
                .slaBreachedCount(slaBreached)
                .slaCompliancePercent(slaCompliance)
                .ticketsByCategory(byCategory)
                .ticketsByPriority(byPriority)
//                .allUsers(allUsers)
                .recentTickets(recent)
//                .unreadNotifications(notifications)
                .build();
    }
}
