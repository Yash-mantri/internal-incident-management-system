package com.iimp.service;

import java.util.List;

import com.iimp.dto.IncidentDtos;
import com.iimp.dto.IncidentDtos.AddCategoryRequest;
import com.iimp.dto.UserDtos;
import com.iimp.entity.Category;
import com.iimp.entity.User;

public interface UserService {

	UserDtos.UserDetail createUser(UserDtos.CreateUserRequest req);

	UserDtos.UserDetail updateUser(Long userId, UserDtos.UpdateUserRequest req);

	void deactivateUser(Long userId, String adminEmail);

	void reactivateUser(Long userId);

	List<UserDtos.UserDetail> getAllUsers();

	List<UserDtos.UserSummary> getActiveSupportStaff();

	UserDtos.UserDetail getUserById(Long id);

	UserDtos.UserDetail toDetail(User u);

	UserDtos.UserSummary toSummary(User u);

	boolean createDepartment(String departmentName);

	Boolean deleteByDepartmentName(String departmentName);

	UserDtos.UserDetail deleteUserById(Long id);

	List<Category> getCategories();

	Boolean addCategory(IncidentDtos.AddCategoryRequest req);

	Boolean updateCategory(Long id, AddCategoryRequest req);

}