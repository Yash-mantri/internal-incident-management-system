package com.iimp.repository;

import com.iimp.entity.User;
import com.iimp.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	List<User> findByRole(Role role);

	List<User> findByRoleAndIsActiveTrue(Role role);

	List<User> findByDepartmentAndRole(String department, Role role);

	List<User> findByIsActiveTrue();

	List<User> findByRoleAndIsActive(Role role, boolean isActive);
}
