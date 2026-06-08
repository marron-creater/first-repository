package com.example.demo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.validation.Valid;

@Controller
public class CalendarController {

	// 日本語ページ用
	@GetMapping("/calendar")
	String showCalendarJapanese(@Valid @ModelAttribute CalendarRequestDto form, BindingResult result, Model model) {

		// 入力された値が正しくなくても初期画面になるようにそれぞれの値を処理
		YearMonthPlace yearMonthPlace = CalendarService.returnYearMonthPlace(form, result.hasErrors());
		String currentPlace = yearMonthPlace.weatherPlace();
		int currentYear = yearMonthPlace.year();
		int currentMonth = yearMonthPlace.month();

		// HTMLにデータを渡す
		model.addAttribute("year", currentYear);
		model.addAttribute("month", currentMonth);
		model.addAttribute("yearMonth", currentYear + "年" + currentMonth + "月");
		model.addAttribute("dayList", CalendarService.DAYOFWEEKJAPANESE);
		model.addAttribute("weatherPlace", currentPlace);
		// カレンダーの要素をServiceから取ってきて渡す
		boolean isEnglish = false;
		List<List<CalendarElement>> calendarDate = CalendarService.returnCalendarElement(currentYear, currentMonth,
				currentPlace, isEnglish);
		model.addAttribute("date", calendarDate);

		// 日本語用ページを表示させる
		return "japaneseCalendar";
	}

	// 英語ページ用
	@GetMapping("/calendar/en")
	String showCalendarEnglish(@Valid @ModelAttribute CalendarRequestDto form, BindingResult result, Model model) {

		// 入力された値が正しくなくても初期画面になるようにそれぞれの値を処理
		YearMonthPlace yearMonthPlace = CalendarService.returnYearMonthPlace(form, result.hasErrors());
		String currentPlace = yearMonthPlace.weatherPlace();
		int currentYear = yearMonthPlace.year();
		int currentMonth = yearMonthPlace.month();

		// 月を英語にしてフォーマット
		LocalDate date = LocalDate.of(currentYear, currentMonth, 1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH);
		String englishMonth = date.format(formatter);

		// HTMLにデータを渡す
		model.addAttribute("year", currentYear);
		model.addAttribute("month", currentMonth);
		model.addAttribute("yearMonth", englishMonth + " " + currentYear);
		model.addAttribute("dayList", CalendarService.DAYOFWEEKENGLISH);
		model.addAttribute("weatherPlace", currentPlace);
		// カレンダーの要素をServiceから取ってきて渡す
		boolean isEnglish = true;
		List<List<CalendarElement>> calendarDate = CalendarService.returnCalendarElement(currentYear, currentMonth,
				currentPlace, isEnglish);
		model.addAttribute("date", calendarDate);

		// 英語用ページを表示させる
		return "englishCalendar";
	}
}