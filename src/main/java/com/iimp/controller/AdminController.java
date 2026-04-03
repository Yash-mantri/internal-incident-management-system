package com.iimp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.DashboardDtos;
import com.iimp.dto.IncidentDtos;
import com.iimp.dto.UserDtos;
import com.iimp.entity.Category;
import com.iimp.entity.SlaConfig;
import com.iimp.repository.SlaConfigRepository;
import com.iimp.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://iimp-3gso.onrender.com", allowCredentials = "true")
public class AdminController {

	private final UserService userService;
	private final SlaConfigRepository slaConfigRepository;

	@GetMapping("/users")
	public ResponseEntity<List<UserDtos.UserDetail>> listAllUsers() {
		return ResponseEntity.ok(userService.getAllUsers());
	}

	@GetMapping("/users/{id}")
	public ResponseEntity<UserDtos.UserDetail> getUser(@PathVariable Long id) {
		return ResponseEntity.ok(userService.getUserById(id));
	}

	@PostMapping("/users")
	public ResponseEntity<UserDtos.UserDetail> createUser(@Valid @RequestBody UserDtos.CreateUserRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
	}

	@PutMapping("/users/{id}")
	public ResponseEntity<UserDtos.UserDetail> updateUser(@PathVariable Long id,
			@RequestBody UserDtos.UpdateUserRequest req) {
		return ResponseEntity.ok(userService.updateUser(id, req));
	}

	@DeleteMapping("/users/{id}")
	public ResponseEntity<UserDtos.UserDetail> deleteUserById(@PathVariable Long id) {
		if (userService.deleteUserById(id) == null)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
		else
			return ResponseEntity.status(HttpStatus.OK).body(userService.deleteUserById(id));
	}

	@PatchMapping("/users/{id}/deactivate")
	public ResponseEntity<Void> deactivateUser(@PathVariable Long id,
			@AuthenticationPrincipal UserDetails userDetails) {
		userService.deactivateUser(id, userDetails.getUsername());
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/users/{id}/reactivate")
	public ResponseEntity<Void> reactivateUser(@PathVariable Long id) {
		userService.reactivateUser(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/departments/{department}")
	public ResponseEntity<Map<String, String>> createDepartment(@PathVariable String department) {
		if (userService.createDepartment(department))
			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("Department created Successfully", department));
		else
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("Department with Name already exits ", department));
	}

	@DeleteMapping("/departments/{departmentName}")
	public ResponseEntity<String> deleteDepartmentByName(@PathVariable String departmentName) {
		if (userService.deleteByDepartmentName(departmentName))
			return ResponseEntity.status(HttpStatus.OK)
					.body("Department with name: " + departmentName + " deleted Successfully");
		else
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Department does not exits");
	}

	@GetMapping("/sla-configs")
	public ResponseEntity<List<SlaConfig>> listSlaConfigs() {
		return ResponseEntity.ok(slaConfigRepository.findAll());
	}

	@PutMapping("/sla-configs")
	public ResponseEntity<SlaConfig> updateSla(@Valid @RequestBody DashboardDtos.UpdateSlaRequest req) {
		SlaConfig config = slaConfigRepository.findById(req.getId()).orElseThrow(
				() -> new com.iimp.exception.ResourceNotFoundException("SLA config not found: " + req.getId()));
		config.setResolutionTimeHours(req.getResolutionTimeHours());
		return ResponseEntity.ok(slaConfigRepository.save(config));
	}

	@GetMapping("/support-staff")
	public ResponseEntity<List<UserDtos.UserSummary>> getActiveSupportStaff() {
		return ResponseEntity.ok(userService.getActiveSupportStaff());
	}

	@GetMapping("/category")
	public ResponseEntity<List<Category>> getCategories() {
		return ResponseEntity.ok(userService.getCategories());
	}

	@PostMapping("/category")
	public ResponseEntity<Boolean> addCategory(@Valid @RequestBody IncidentDtos.AddCategoryRequest req) {
		return ResponseEntity.ok(userService.addCategory(req));
	}

	@PutMapping("/{id}/category")
	public ResponseEntity<Boolean> updateCategory(@PathVariable Long id,
			@RequestBody IncidentDtos.AddCategoryRequest req) {
		return ResponseEntity.ok(userService.updateCategory(id, req));
	}
}
