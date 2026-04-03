package com.iimp.service;

import com.iimp.dto.DashboardDtos;

public interface DashboardService {

	DashboardDtos.EmployeeDashboard getEmployeeDashboard(String email);

	DashboardDtos.SupportDashboard getSupportDashboard(String email);

	DashboardDtos.ManagerDashboard getManagerDashboard(String email);

	DashboardDtos.AdminDashboard getAdminDashboard(String email);

}