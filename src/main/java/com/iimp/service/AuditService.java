package com.iimp.service;

import com.iimp.entity.Incident;
import com.iimp.entity.User;

public interface AuditService {

	void log(Incident incident, User changedBy, String action, String oldValue, String newValue);

	void log(Incident incident, User changedBy, String action);

}