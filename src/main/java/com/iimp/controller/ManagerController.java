package com.iimp.controller;

import com.iimp.dto.UserDtos;
import com.iimp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class ManagerController {

    private final UserService userService;

    
    @GetMapping("/getAllsupport-staff")
    public ResponseEntity<List<UserDtos.UserSummary>> getActiveSupportStaff() {
        return ResponseEntity.ok(userService.getActiveSupportStaff());
    }
}
