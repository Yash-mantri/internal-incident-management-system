package com.iimp.service;

import com.iimp.dto.UserDtos;
import com.iimp.entity.User;
import com.iimp.enums.Role;
import com.iimp.exception.BadRequestException;
import com.iimp.exception.ResourceNotFoundException;
import com.iimp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

   

    @Transactional
    public UserDtos.UserDetail createUser(UserDtos.CreateUserRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered: " + req.getEmail());
        }
      
        String tempPassword = "Tmp@" + UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .role(req.getRole())
                .department(req.getDepartment())
                .isActive(true)
                .isFirstLogin(true)
                .build();
        
        userRepository.save(user);

        
        notificationService.send(user,
                "Welcome to IIMP! Your temporary password is: " + tempPassword
                        + " — Please change it on your first login.");

        return toDetail(user);
    }

    

    @Transactional
    public UserDtos.UserDetail updateUser(Long userId, UserDtos.UpdateUserRequest req) {
        User user = findById(userId);
        if (req.getName()       != null) user.setName(req.getName());
        if (req.getRole()       != null) user.setRole(req.getRole());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        userRepository.save(user);
        return toDetail(user);
    }

  

    @Transactional
    public void deactivateUser(Long userId, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (admin.getId().equals(userId)) {
            throw new BadRequestException("Admin cannot deactivate themselves");
        }
        User user = findById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

   

    @Transactional
    public void reactivateUser(Long userId) {
        User user = findById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

  

    @Transactional(readOnly = true)
    public List<UserDtos.UserDetail> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDetail).toList();
    }

  

    @Transactional(readOnly = true)
    public List<UserDtos.UserSummary> getActiveSupportStaff() {
        return userRepository.findByRoleAndIsActiveTrue(Role.SUPPORT_STAFF)
                .stream().map(this::toSummary).toList();
    }

  
    @Transactional(readOnly = true)
    public UserDtos.UserDetail getUserById(Long id) {
        return toDetail(findById(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserDtos.UserDetail toDetail(User u) {
        return UserDtos.UserDetail.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .department(u.getDepartment())
                .role(u.getRole())
                .active(u.isActive())
                .firstLogin(u.isFirstLogin())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public UserDtos.UserSummary toSummary(User u) {
        return UserDtos.UserSummary.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .department(u.getDepartment())
                .role(u.getRole().name())
                .build();
    }
}
