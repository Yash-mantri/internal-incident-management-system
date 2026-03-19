package com.iimp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.iimp.entity.Otp;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
	Optional<Otp> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, Otp.OtpPurpose purpose);

}
