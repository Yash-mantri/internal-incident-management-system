package com.iimp.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.iimp.entity.StaffLeave;
import com.iimp.entity.User;

@Repository
public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {
	boolean existsByUserAndLeaveDateAndApprovedTrue(User user, LocalDate leaveDate);

	@Query("""
			SELECT sl.leaveDate
			FROM StaffLeave sl
			WHERE sl.user = :user
			  AND sl.approved = true
			  AND sl.leaveDate BETWEEN :from AND :to
			ORDER BY sl.leaveDate
			""")
	List<LocalDate> findApprovedLeaveDates(@Param("user") User user, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	List<StaffLeave> findByUserOrderByLeaveDateDesc(User user);

	@Query("""
			SELECT sl FROM StaffLeave sl
			WHERE sl.user = :user
			  AND sl.approved = true
			  AND YEAR(sl.leaveDate)  = :year
			  AND MONTH(sl.leaveDate) = :month
			ORDER BY sl.leaveDate
			""")
	List<StaffLeave> findApprovedLeaveForMonth(@Param("user") User user, @Param("year") int year,
			@Param("month") int month);

	@Query("""
			SELECT sl FROM StaffLeave sl
			WHERE sl.approved = false
			  AND sl.approvedBy IS NULL
			ORDER BY sl.leaveDate
			""")
	List<StaffLeave> findPendingLeaveRequests();

	@Query("""
			SELECT sl FROM StaffLeave sl
			WHERE sl.approved = false
			  AND sl.approvedBy IS NULL
			  AND sl.user.department = :department
			ORDER BY sl.leaveDate
			""")
	List<StaffLeave> findPendingLeaveRequestsByDepartment(@Param("department") String department);

	Optional<StaffLeave> findByUserAndLeaveDate(User user, LocalDate leaveDate);

	@Query("""
			SELECT sl FROM StaffLeave sl
			WHERE sl.approved = true
			  AND sl.leaveDate = :date
			  AND sl.user.department = :department
			""")
	List<StaffLeave> findApprovedLeavesOnDateByDepartment(@Param("date") LocalDate date,
			@Param("department") String department);

	@Query("""
			SELECT COUNT(sl) FROM StaffLeave sl
			WHERE sl.user = :user
			  AND sl.approved = true
			  AND sl.leaveDate BETWEEN :from AND :to
			""")
	long countApprovedLeaveDays(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

	void deleteByUserAndLeaveDate(User user, LocalDate leaveDate);
}