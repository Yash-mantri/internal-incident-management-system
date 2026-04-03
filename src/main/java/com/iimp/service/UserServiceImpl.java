package com.iimp.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iimp.dto.IncidentDtos;
import com.iimp.dto.IncidentDtos.AddCategoryRequest;
import com.iimp.dto.UserDtos;
import com.iimp.entity.Category;
import com.iimp.entity.User;
import com.iimp.enums.EventType;
import com.iimp.enums.Role;
import com.iimp.exception.BadRequestException;
import com.iimp.exception.ResourceNotFoundException;
import com.iimp.repository.CategoryRepository;
import com.iimp.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final CategoryRepository categoryRepository;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
      

    @Override
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

        
        notificationService.sendUserCreated(user,tempPassword,
                EventType.USER_CREATED);

        return toDetail(user);
    }

    

    @Override
	@Transactional
    public UserDtos.UserDetail updateUser(Long userId, UserDtos.UpdateUserRequest req) {
        User user = findById(userId);
        if (req.getName()       != null) user.setName(req.getName());
        if (req.getRole()       != null) user.setRole(req.getRole());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        userRepository.save(user);
        return toDetail(user);
    }

  

    @Override
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

   

    @Override
	@Transactional
    public void reactivateUser(Long userId) {
        User user = findById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

  

    @Override
	@Transactional(readOnly = true)
    public List<UserDtos.UserDetail> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDetail).toList();
    }

  

    @Override
	@Transactional(readOnly = true)
    public List<UserDtos.UserSummary> getActiveSupportStaff() {
        return userRepository.findByRoleAndIsActiveTrue(Role.SUPPORT_STAFF)
                .stream().map(this::toSummary).toList();
    }

  
    @Override
	@Transactional(readOnly = true)
    public UserDtos.UserDetail getUserById(Long id) {
        return toDetail(findById(id));
    }

   

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
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

    @Override
	public UserDtos.UserSummary toSummary(User u) {
        return UserDtos.UserSummary.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .department(u.getDepartment())
                .role(u.getRole().name())
                .build();
    }



    @Override
	@Transactional
	public boolean createDepartment( String departmentName) {
    	Optional<Category> op=categoryRepository.findByDepartmentName(departmentName);
    	if(op.isPresent()){
			return false;
		}
    	Category category=new Category();
    	category.setCategoryName(departmentName);
		category.setDepartmentName(departmentName);
		categoryRepository.save(category);
		return true;
		
	}


    @Override
	@Transactional
	public Boolean deleteByDepartmentName(String departmentName) {
    	Optional<Category> op=categoryRepository.findByDepartmentName(departmentName);
    	
		if(op.isPresent()){
			Optional<Category> optional= categoryRepository.deleteByDepartmentName(departmentName);
			if(optional.isPresent())return true;
			else return false;
		}
		return false;
	}



	@Override
	public UserDtos.UserDetail deleteUserById(Long id) {
		Optional<User> op=userRepository.findById(id);
		if(op.isEmpty()) {
			return null;
		}
		userRepository.deleteById(id);
	    User user=op.get();
		return toDetail(user) ;
	}



	@Override
	public List<Category> getCategories() {	
		// TODO Auto-generated method stub
		List<Category> categoryList = categoryRepository.findAll();
		if (categoryList.isEmpty()) {
			return null;
		}
		return categoryList;
	}


	@Override
	@Transactional	
	public Boolean addCategory(@Valid IncidentDtos.AddCategoryRequest req) {
		
		Category newCategory = new Category();
		newCategory.setCategoryName(req.getCategoryName());
		newCategory.setDepartmentName(req.getDepartmentName());
		categoryRepository.save(newCategory);
		return true;
	}

	@Override
	@Transactional
	public Boolean updateCategory(Long id, AddCategoryRequest req) {
		
		Optional<Category> category = categoryRepository.findById(id);
		if (category.isEmpty()) {
			return false;
		}
		Category updateCatgeory = category.get();
		updateCatgeory.setCategoryName(req.getCategoryName());
		updateCatgeory.setDepartmentName(req.getDepartmentName());
		return true;
	}
}
