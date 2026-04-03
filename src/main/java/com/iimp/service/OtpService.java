package com.iimp.service;


import com.iimp.enums.EventType;

public interface OtpService {

	String generateAndSend(String email, EventType purpose);

	void verify(String token,String email, String code, EventType purpose);

}