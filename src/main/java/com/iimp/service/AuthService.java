package com.iimp.service;

import com.iimp.dto.AuthDtos;

public interface AuthService {

	AuthDtos.LoginResponse login(AuthDtos.LoginRequest req);

	void changePassword(AuthDtos.ChangePasswordRequest req);

	AuthDtos.LoginResponse refreshToken(AuthDtos.RefreshTokenRequest req);

	void forgotPassword(AuthDtos.ForgotPasswordRequest req);

}