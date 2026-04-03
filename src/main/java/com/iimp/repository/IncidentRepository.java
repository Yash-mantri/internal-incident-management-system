package com.iimp.repository;

import com.iimp.dto.ManagerDtos;
import com.iimp.entity.Incident;
import com.iimp.entity.User;
import com.iimp.enums.IncidentStatus;
import com.iimp.enums.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

	Optional<Incident> findByIncidentKey(String incidentKey);

	Page<Incident> findByCreatedBy(User user, Pageable pageable);

	Page<Incident> findByAssignedTo(User user, Pageable pageable);

	List<Incident> findByCategoryDepartmentNameOrderByCreatedAtDesc(String departmentName);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :departmentName")
	long countAllByDepartment(@Param("departmentName") String dept);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :departmentName AND i.createdAt >= :since")
	long countCreatedSinceByDepartment(@Param("departmentName") String dept, @Param("since") LocalDateTime since);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :departmentName AND i.status = :status")
	long countByDepartmentAndStatus(@Param("departmentName") String dept, @Param("status") IncidentStatus status);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :departmentName AND i.isSlaBreached = true")
	long countSlaBreachedByDepartment(@Param("departmentName") String dept);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :departmentName AND i.createdAt >= :from AND i.createdAt < :to")
	long countByDepartmentAndCreatedBetween(@Param("departmentName") String dept, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	@Query("SELECT new com.iimp.dto.ManagerDtos$CategoryEntry(i.category.categoryName, COUNT(i)) "
			+ "FROM Incident i WHERE i.category.departmentName = :departmentName GROUP BY i.category.categoryName")
	List<ManagerDtos.CategoryEntry> countGroupByCategoryInDepartment(@Param("departmentName") String dept);

	@Query("SELECT i FROM Incident i WHERE i.category.departmentName = :dept")
	Page<Incident> findByDepartment(@Param("dept") String dept, Pageable pageable);

	@Query("""
			    SELECT i FROM Incident i
			    WHERE (:userId IS NULL OR i.createdBy.id = :userId)
			      AND (:assignedId IS NULL OR i.assignedTo.id = :assignedId)
			      AND (:status IS NULL OR i.status = :status)
			      AND (:priority IS NULL OR i.priority = :priority)
			      AND (:category IS NULL OR i.category.categoryName = :category)
			      AND (:from IS NULL OR i.createdAt >= :from)
			      AND (:to IS NULL OR i.createdAt <= :to)
			""")
	Page<Incident> findFiltered(@Param("userId") Long userId, @Param("assignedId") Long assignedId,
			@Param("status") IncidentStatus status, @Param("priority") Priority priority,
			@Param("category") String category, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
			Pageable pageable);

	@Query("""
			    SELECT i FROM Incident i
			    WHERE i.status IN ('OPEN','IN_PROGRESS')
			      AND i.slaDueAt IS NOT NULL
			      AND i.slaDueAt < :now
			      AND i.isSlaBreached = false
			""")
	List<Incident> findBreachedIncidents(@Param("now") LocalDateTime now);

	@Query("""
			    SELECT i FROM Incident i
			    WHERE i.priority = 'CRITICAL'
			      AND i.status = 'OPEN'
			      AND i.assignedTo IS NULL
			      AND i.createdAt < :threshold
			""")
	List<Incident> findCriticalUnassigned(@Param("threshold") LocalDateTime threshold);

	long countByCreatedBy(User user);

	long countByAssignedTo(User user);

	long countByStatus(IncidentStatus status);

	long countByCreatedByAndStatus(User user, IncidentStatus status);

	long countByAssignedToAndStatus(User user, IncidentStatus status);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :dept AND i.status = :status")
	long countByDeptAndStatus(@Param("dept") String dept, @Param("status") IncidentStatus status);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.category.departmentName = :dept")
	long countByDept(@Param("dept") String dept);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.isSlaBreached = true AND i.category.departmentName = :dept")
	long countSlaBreachedByDept(@Param("dept") String dept);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.createdAt >= :from")
	long countCreatedSince(@Param("from") LocalDateTime from);

	@Query("SELECT COUNT(i) FROM Incident i WHERE i.isSlaBreached = true")
	long countSlaBreached();

	@Query("SELECT i.category.categoryName, COUNT(i) FROM Incident i GROUP BY i.category.categoryName")
	List<Object[]> countByCategory();

	@Query("SELECT i.priority, COUNT(i) FROM Incident i GROUP BY i.priority")
	List<Object[]> countByPriority();

	@Query("SELECT i.assignedTo.id, COUNT(i) FROM Incident i WHERE i.assignedTo IS NOT NULL AND i.category.departmentName = :dept GROUP BY i.assignedTo.id")
	List<Object[]> teamWorkloadByDept(@Param("dept") String dept);

	List<Incident> findTop10ByOrderByCreatedAtDesc();

	@Query("""
			SELECT COUNT(i) FROM Incident i
			WHERE i.status IN ('RESOLVED', 'CLOSED')
			AND i.slaDueAt IS NOT NULL
			AND i.resolvedAt IS NOT NULL
			AND i.resolvedAt <= i.slaDueAt
			""")
	long countResolvedOnTime();

	@Query("""
			SELECT COUNT(i) FROM Incident i
			WHERE i.status IN ('RESOLVED', 'CLOSED')
			AND i.slaDueAt IS NOT NULL
			""")
	long countResolvedWithSla();

	@Query("""
			SELECT COUNT(i) FROM Incident i
			WHERE i.isSlaBreached = true
			OR (
			    i.status IN ('OPEN', 'IN_PROGRESS')
			    AND i.slaDueAt IS NOT NULL
			    AND i.slaDueAt < :now
			)
			""")
	long countBreached(@Param("now") LocalDateTime now);

	@Query("""
			SELECT COUNT(i) FROM Incident i
			WHERE i.createdAt >= :startOfDay
			AND i.createdAt < :endOfDay
			""")
	long countCreatedToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

	@Query("""
			SELECT i FROM Incident i
			WHERE i.createdAt >= :from
			ORDER BY i.createdAt ASC
			""")
	List<Incident> findAllCreatedSince(@Param("from") LocalDateTime from);

	@Query("""
			SELECT i.category.categoryName, COUNT(i)
			FROM Incident i
			GROUP BY i.category.categoryName
			ORDER BY COUNT(i) DESC
			""")
	List<Object[]> countGroupedByCategory();

	List<Incident> findAllByCategory_Id(Long categoryId);

	@Query("""
			SELECT i.category.departmentName, COUNT(i)
			FROM Incident i
			GROUP BY i.category.departmentName
			ORDER BY COUNT(i) DESC
			""")
	List<Object[]> countGroupedByDepartment();

	List<Incident> findByCreatedBy_EmailOrderByCreatedAtDesc(String email);

	long countByCreatedBy_Email(String email);

	long countByCreatedBy_EmailAndStatus(String email, IncidentStatus open);

	@Query("SELECT i FROM Incident i WHERE i.status IN ('OPEN','IN_PROGRESS')")
	List<Incident> findActiveIncidents();
	
	@Query("SELECT i FROM Incident i WHERE i.assignedTo = :assignee AND i.status IN ('OPEN', 'IN_PROGRESS')")
	List<Incident> findOpenIncidentsByAssignee(@Param("assignee") User assignee);

}
