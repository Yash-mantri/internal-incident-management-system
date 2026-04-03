package com.iimp.service;

import com.iimp.dto.TicketVolumeDto;
import com.iimp.entity.Incident;
import com.iimp.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final IncidentRepository incidentRepository;

    @Override
	public List<TicketVolumeDto> getTicketVolume(String range) {
        return switch (range.toLowerCase()) {
            case "month" -> getMonthlyVolume();
            case "year"  -> getYearlyVolume();
            default      -> getWeeklyVolume();   // "week" is default
        };
    }

   
    private List<TicketVolumeDto> getWeeklyVolume() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(6).atStartOfDay();

        List<Incident> incidents = incidentRepository.findAllCreatedSince(from);

        // Build ordered map: date → count
        Map<LocalDate, Long> countByDay = incidents.stream()
            .collect(Collectors.groupingBy(
                i -> i.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));

        List<TicketVolumeDto> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE"); // Mon, Tue…

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            result.add(TicketVolumeDto.builder()
                .label(day.format(fmt))
                .count(countByDay.getOrDefault(day, 0L))
                .build());
        }
        return result;
    }

    
    private List<TicketVolumeDto> getMonthlyVolume() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(29).atStartOfDay();

        List<Incident> incidents = incidentRepository.findAllCreatedSince(from);

        // Week buckets: 0-6 days ago = Week 4, 7-13 = Week 3, etc.
        long[] weeks = new long[4];
        for (Incident i : incidents) {
            long daysAgo = java.time.temporal.ChronoUnit.DAYS
                .between(i.getCreatedAt().toLocalDate(), today);
            int bucket = (int) Math.min(daysAgo / 7, 3); // 0=most recent
            weeks[3 - bucket]++;                          // reverse so Week 1 is oldest
        }

        List<TicketVolumeDto> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            result.add(TicketVolumeDto.builder()
                .label("Week " + (i + 1))
                .count(weeks[i])
                .build());
        }
        return result;
    }

    
    private List<TicketVolumeDto> getYearlyVolume() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusMonths(11).withDayOfMonth(1).atStartOfDay();

        List<Incident> incidents = incidentRepository.findAllCreatedSince(from);

        Map<String, Long> countByMonth = incidents.stream()
            .collect(Collectors.groupingBy(
                i -> i.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                Collectors.counting()
            ));

        List<TicketVolumeDto> result = new ArrayList<>();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("MMM");

        for (int i = 11; i >= 0; i--) {
            LocalDate month = today.minusMonths(i).withDayOfMonth(1);
            String key = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            result.add(TicketVolumeDto.builder()
                .label(month.format(labelFmt))
                .count(countByMonth.getOrDefault(key, 0L))
                .build());
        }
        return result;
    }
}

