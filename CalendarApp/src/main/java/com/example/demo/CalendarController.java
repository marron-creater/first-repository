package com.example.demo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

	// 日本語ページ用
	@GetMapping("/calendar")
	String showCalendarJapanese(@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month, @RequestParam(required = false) String weatherPlace,
			Model model) {

		// HTMLから値を受け取ってきていない場合の初期値
		String currentPlace = (weatherPlace == null) ? CalendarService.DEFAULTPLACE : weatherPlace;
		int currentYear = (year == null) ? LocalDate.now().getYear() : year;
		int currentMonth = (month == null) ? LocalDate.now().getMonthValue() : month;

		// HTMLに既定値以上の年が入ったら変換して上限、下限値に設定するため
		if (currentYear < 1950) {
			currentYear = 1950;
			currentMonth = 1;
		} else if (currentYear > 2999) {
			currentYear = 2999;
			currentMonth = 12;
		}

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
	String showCalendarEnglish(@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month, @RequestParam(required = false) String weatherPlace,
			Model model) {

		// HTMLから値を受け取ってきていない場合の初期値
		String currentPlace = (weatherPlace == null) ? "35.6785,139.6823" : weatherPlace;
		int currentYear = (year == null) ? LocalDate.now().getYear() : year;
		int currentMonth = (month == null) ? LocalDate.now().getMonthValue() : month;

		// HTMLに既定値以上の年が入ったら変換して上限、下限値に設定するため
		if (currentYear < 1950) {
			currentYear = 1950;
			currentMonth = 1;
		} else if (currentYear > 2999) {
			currentYear = 2999;
			currentMonth = 12;
		}

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
