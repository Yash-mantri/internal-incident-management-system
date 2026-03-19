package com.iimp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iimp.dto.DashboardDtos;
import com.iimp.dto.UserDtos;
import com.iimp.entity.SlaConfig;
import com.iimp.repository.SlaConfigRepository;
import com.iimp.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins="http://192.168.0.166:5173",allowCredentials = "true")
public class AdminController {

    private final UserService userService;
    private final SlaConfigRepository slaConfigRepository;

  
    @GetMapping("/getAllUsers")
    public ResponseEntity<List<UserDtos.UserDetail>> listAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/getUserById/{id}")
    public ResponseEntity<UserDtos.UserDetail> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

  
    @PostMapping("/createUser")
    public ResponseEntity<UserDtos.UserDetail> createUser(
            @Valid @RequestBody UserDtos.CreateUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(req));
    }


    @PutMapping("/updateUserById/{id}")
    public ResponseEntity<UserDtos.UserDetail> updateUser(
            @PathVariable Long id,
            @RequestBody UserDtos.UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

  
    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deactivateUser(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

  
    @PatchMapping("/users/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long id) {
        userService.reactivateUser(id);
        return ResponseEntity.ok().build();
    }

  
    @GetMapping("/getSLA")
    public ResponseEntity<List<SlaConfig>> listSlaConfigs() {
        return ResponseEntity.ok(slaConfigRepository.findAll());
    }

  
    @PutMapping("/updateSLA")
    public ResponseEntity<SlaConfig> updateSla(
            @Valid @RequestBody DashboardDtos.UpdateSlaRequest req) {
        SlaConfig config = slaConfigRepository.findById(req.getId())
                .orElseThrow(() -> new com.iimp.exception.ResourceNotFoundException(
                        "SLA config not found: " + req.getId()));
        config.setResolutionTimeHours(req.getResolutionTimeHours());
        return ResponseEntity.ok(slaConfigRepository.save(config));
    }

  
    @GetMapping("/support-staff")
    public ResponseEntity<List<UserDtos.UserSummary>> getActiveSupportStaff() {
        return ResponseEntity.ok(userService.getActiveSupportStaff());
    }
}
