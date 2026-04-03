package com.iimp.service;

import java.util.List;

import com.iimp.dto.TicketVolumeDto;

public interface ReportService {

	List<TicketVolumeDto> getTicketVolume(String range);

}