package com.example.demo;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CalendarElement {
	private LocalDate date;
	private String text;
	private boolean isToday;
	private boolean isLastMonth;
	private boolean isNextMonth;
	@Setter
	private String weatherIcon;
	private String holidayName;

}
