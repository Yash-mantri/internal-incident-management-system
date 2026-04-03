package com.iimp.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.iimp.entity.User;
import com.iimp.repository.StaffLeaveRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BusinessHoursCalculator {

	public static final LocalTime DAY_START = LocalTime.of(9, 0);

	public static final LocalTime DAY_END = LocalTime.of(19, 0);

	public static final double HOURS_PER_DAY = Duration.between(DAY_START, DAY_END).toMinutes() / 60.0; // = 10.0

	private final StaffLeaveRepository leaveRepository;

	public LocalDateTime calcDeadline(LocalDateTime from, double slaHours, User assignee) {
		if (slaHours <= 0) {
			return from;
		}

		LocalDateTime cursor = snapToNextBusinessStart(from);

		Set<LocalDate> leaveDays = fetchLeaveDays(assignee, cursor.toLocalDate(), cursor.toLocalDate().plusDays(90));

		double remaining = slaHours;

		while (remaining > 0) {
			LocalDate day = cursor.toLocalDate();

			if (isNonWorkingDay(day, leaveDays)) {
				cursor = day.plusDays(1).atTime(DAY_START);
				continue;
			}

			LocalDateTime endOfDay = day.atTime(DAY_END);
			double hoursAvailableToday = Duration.between(cursor, endOfDay).toMinutes() / 60.0;

			if (remaining <= hoursAvailableToday) {

				return cursor.plusMinutes((long) (remaining * 60));
			}

			remaining -= hoursAvailableToday;
			cursor = day.plusDays(1).atTime(DAY_START);
		}

		return cursor;
	}

	public double calcElapsedBusinessHours(LocalDateTime from, LocalDateTime to, User assignee) {
		if (!to.isAfter(from))
			return 0.0;

		LocalDateTime cursor = snapToNextBusinessStart(from);
		if (!cursor.isBefore(to))
			return 0.0;

		Set<LocalDate> leaveDays = fetchLeaveDays(assignee, cursor.toLocalDate(), to.toLocalDate());

		double total = 0.0;

		while (cursor.isBefore(to)) {
			LocalDate day = cursor.toLocalDate();

			if (isNonWorkingDay(day, leaveDays)) {
				cursor = day.plusDays(1).atTime(DAY_START);
				continue;
			}

			LocalDateTime endOfDay = day.atTime(DAY_END);
			LocalDateTime windowEnd = to.isBefore(endOfDay) ? to : endOfDay;

			if (cursor.isBefore(windowEnd)) {
				total += Duration.between(cursor, windowEnd).toMinutes() / 60.0;
			}

			cursor = day.plusDays(1).atTime(DAY_START);
		}

		return total;
	}

	public boolean isBreached(LocalDateTime from, double slaHours, User assignee) {
		LocalDateTime deadline = calcDeadline(from, slaHours, assignee);
		return LocalDateTime.now().isAfter(deadline);
	}

	public double remainingBusinessHours(LocalDateTime from, double slaHours, User assignee) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime deadline = calcDeadline(from, slaHours, assignee);

		if (!now.isBefore(deadline))
			return 0.0;

		return calcElapsedBusinessHours(now, deadline, assignee);
	}

	private LocalDateTime snapToNextBusinessStart(LocalDateTime dt) {
		LocalDate day = dt.toLocalDate();
		LocalTime time = dt.toLocalTime();

		if (time.isBefore(DAY_START)) {
			dt = day.atTime(DAY_START);
		}

		else if (!time.isBefore(DAY_END)) {
			dt = day.plusDays(1).atTime(DAY_START);
			day = dt.toLocalDate();
		}

		while (isWeekend(dt.toLocalDate())) {
			dt = dt.toLocalDate().plusDays(1).atTime(DAY_START);
		}

		return dt;
	}

	private boolean isNonWorkingDay(LocalDate day, Set<LocalDate> leaveDays) {
		return isWeekend(day) || leaveDays.contains(day);
	}

	private boolean isWeekend(LocalDate day) {
		DayOfWeek dow = day.getDayOfWeek();
		return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
	}

	private Set<LocalDate> fetchLeaveDays(User assignee, LocalDate from, LocalDate to) {
		if (assignee == null)
			return Set.of();
		return new HashSet<>(leaveRepository.findApprovedLeaveDates(assignee, from, to));
	}
}