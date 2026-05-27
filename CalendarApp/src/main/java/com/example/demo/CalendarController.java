package com.example.demo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Controller
public class CalendarController {

	@GetMapping("/calendar")
	String showCalendarJapanese(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
			@RequestParam(required = false) String weatherPlace, Model model) {
		String currentPlace = (weatherPlace == null) ? "35.6785,139.6823" : weatherPlace;
		YearMonth current = (year == null || month == null) ? YearMonth.now() : YearMonth.of(year, month);

		model.addAttribute("year", current.getYear());
		model.addAttribute("month", current.getMonthValue());
		model.addAttribute("yearMonth", current);
		model.addAttribute("dayList", dayOfWeekJapanese());
		model.addAttribute("weatherplace", currentPlace);
		model.addAttribute("lang","ja");
		
		List<List<CalendarElement>> calendarDate = generateDate(current.getYear(), current.getMonthValue());
		injectWeatherIcon(calendarDate, currentPlace);
		
		model.addAttribute("date", calendarDate);
		return "calendar";
	}

	@GetMapping("/calendar/en")
	String showCalendarEnglish(@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month, @RequestParam(required = false) String weatherPlace,
			Model model) {
		String currentPlace = (weatherPlace == null) ? "35.6785,139.6823" : weatherPlace;
		YearMonth current = (year == null || month == null) ? YearMonth.now() : YearMonth.of(year, month);

		model.addAttribute("year", current.getYear());
		model.addAttribute("month", current.getMonthValue());
		model.addAttribute("yearMonth", current);
		model.addAttribute("dayList", dayOfWeekEnglish());
		model.addAttribute("weatherplace", currentPlace);
		model.addAttribute("lang","en");
		
		List<List<CalendarElement>> calendarDate = generateDateEnglish(current.getYear(), current.getMonthValue());
		injectWeatherIcon(calendarDate, currentPlace);
		model.addAttribute("date", calendarDate);
		return "calendar";
	}

	// 英語で曜日名を返すメソッド
	private static String[] dayOfWeekEnglish() {
		final String[] dayOfWeekStrings = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
				"Saturday" };
		return dayOfWeekStrings;
	}

	// 日本語で曜日名を返すメソッド
	private static String[] dayOfWeekJapanese() {
		final String[] dayOfWeekStrings = { "日", "月", "火", "水", "木", "金", "土" };
		return dayOfWeekStrings;
	}

	public static List<List<CalendarElement>> generateDateEnglish(int currentYear, int currentMonth) {

		// 日付データの準備
		LocalDate date = LocalDate.of(currentYear, currentMonth, 1);
		date = backDayToSunday(date);
		// カレンダーに出力するための日のみの値、月のみの値を取得.if,for文で使用するためint型
		return getDateElementEnglish(currentMonth, date);
	}
	
	public static List<List<CalendarElement>> generateDate(int currentYear, int currentMonth) {

		// 日付データの準備
		LocalDate date = LocalDate.of(currentYear, currentMonth, 1);
		date = backDayToSunday(date);
		// カレンダーに出力するための日のみの値、月のみの値を取得.if,for文で使用するためint型
		return getDateElement(currentMonth, date);
	}

	// カレンダーの始まりを日曜日に揃えるため取得した日から日曜日まで日付を戻す関数
	private static LocalDate backDayToSunday(LocalDate firstDay) {
		for (int i = 0; i < 6; i++) {
			if (firstDay.getDayOfWeek() == DayOfWeek.SUNDAY) {
				break;
			} else {
				firstDay = firstDay.minusDays(1);
			}
		}
		return firstDay;
	}

	// カレンダーで表示する日付とその要素を取得
	private static List<List<CalendarElement>> getDateElement(int currentMonth, LocalDate displayDate) {

		// 日付とその要素を入れる２次元リストを作成
		List<List<CalendarElement>> displayArray = new ArrayList<>();
		LocalDate today = LocalDate.now();
		int day = displayDate.getDayOfMonth();
		int month = displayDate.getMonthValue();

		Map<LocalDate, String> holidayDate = new HashMap<>();
		try {
			holidayDate = holidayMap();
		} catch (IOException e) {
			System.out.println("ファイルが読み込めませんでした。");
			e.printStackTrace();
		}
		// カレンダーの最大が6週のため6行分ループを回す。
		for (int i = 0; i < 6; i++) {
			// 5行目以降で日が7日よりも小さい場合は次月のみの行ができてしまうためループを抜ける
			if (i >= 4 && day <= 7) {
				break;
			}

			// １週間分の情報を入れるリストを作成
			List<CalendarElement> week = new ArrayList<>();
			// 一週間は7日なので7列分ループを回す
			for (int j = 0; j < 7; j++) {
				String text;
				if (day == 1 || i == 0 && j == 0) {
					// 表示する日が月初かカレンダーの左上なら月日を表示するためのtext用意
					text = month + "/" + day;
				} else {
					// 一般的な場合は日のみ表示するためのtext用意
					text = day + "日";
				}

				// 年の繰り上げ、繰り下げを加味して先月、翌月の判定用の変数の作成
				int lastMonth = (currentMonth - 1) % 12;
				if (lastMonth == 0) {
					lastMonth = 12;
				}
				int nextMonth = (currentMonth + 1) % 12;
				if (nextMonth == 0) {
					nextMonth = 1;
				}

				// クラスの要素をそれぞれ判定してリストに格納
				boolean isToday = displayDate.isEqual(today);
				boolean isLastMonth = (lastMonth == month);
				boolean isNextMonth = (nextMonth == month);
				String holidayName = holidayDate.get(displayDate);
				week.add(new CalendarElement(displayDate, text, isToday, isLastMonth, isNextMonth, null, holidayName));

				// 日にちを進めて月日を更新する
				displayDate = displayDate.plusDays(1);
				day = displayDate.getDayOfMonth();
				month = displayDate.getMonthValue();
			}

			// 週のリストを一つのリストに入れる
			displayArray.add(week);
		}
		return displayArray;
	}

	// カレンダーで表示する日付とその要素を取得
	private static List<List<CalendarElement>> getDateElementEnglish(int currentMonth, LocalDate displayDate) {

		// 日付とその要素を入れる２次元リストを作成
		List<List<CalendarElement>> displayArray = new ArrayList<>();
		LocalDate today = LocalDate.now();
		int day = displayDate.getDayOfMonth();
		int month = displayDate.getMonthValue();

		Map<LocalDate, String> holidayDate = new HashMap<>();
		try {
			holidayDate = holidayMap();
		} catch (IOException e) {
			System.out.println("ファイルが読み込めませんでした。");
			e.printStackTrace();
		}
		// カレンダーの最大が6週のため6行分ループを回す。
		for (int i = 0; i < 6; i++) {
			// 5行目以降で日が7日よりも小さい場合は次月のみの行ができてしまうためループを抜ける
			if (i >= 4 && day <= 7) {
				break;
			}

			// １週間分の情報を入れるリストを作成
			List<CalendarElement> week = new ArrayList<>();
			// 一週間は7日なので7列分ループを回す
			for (int j = 0; j < 7; j++) {
				String text;
				if (day == 1 || i == 0 && j == 0) {
					// 表示する日が月初かカレンダーの左上なら月日を表示するためのtext用意
					text = month + "/" + day;
				} else {
					// 一般的な場合は日のみ表示するためのtext用意
					text = Integer.toString(day);
				}

				// 年の繰り上げ、繰り下げを加味して先月、翌月の判定用の変数の作成
				int lastMonth = (currentMonth - 1) % 12;
				if (lastMonth == 0) {
					lastMonth = 12;
				}
				int nextMonth = (currentMonth + 1) % 12;
				if (nextMonth == 0) {
					nextMonth = 1;
				}

				// クラスの要素をそれぞれ判定してリストに格納
				boolean isToday = displayDate.isEqual(today);
				boolean isLastMonth = (lastMonth == month);
				boolean isNextMonth = (nextMonth == month);
				String holidayName = holidayDate.get(displayDate);
				week.add(new CalendarElement(displayDate,text, isToday, isLastMonth, isNextMonth, null, holidayName));

				// 日にちを進めて月日を更新する
				displayDate = displayDate.plusDays(1);
				day = displayDate.getDayOfMonth();
				month = displayDate.getMonthValue();
			}

			// 週のリストを一つのリストに入れる
			displayArray.add(week);
		}
		return displayArray;
	}

	// htmlで情報を送るクラス
	@NoArgsConstructor
	@AllArgsConstructor
	@Getter
	public static class CalendarElement {
		private LocalDate date;
		private String text;
		private boolean isToday;
		private boolean isLastMonth;
		private boolean isNextMonth;
		@Setter
		private String weatherIcon;
		private String holidayName;

	}

	// 天気のアイコンを取得して返すメソッド
	public static String[] getWeatherIcon(String latitude, String longitude) {

		LocalDate startDay = LocalDate.now();
		LocalDate endDay = startDay.plusDays(3);

		JsonNode weatherCodeNode;
		String noDate[] = new String[4];
		try {
			// 天気コードの取得
			weatherCodeNode = fetchJsonNode(startDay, endDay, latitude, longitude);
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			throw new IllegalArgumentException();
		} catch (IOException | InterruptedException ex) {
			System.out.println("通信エラーが発生しました。");
			ex.printStackTrace();
			return noDate;
		}

		int[] weatherCodeArray = new int[weatherCodeNode.size()];// 天気コードを格納する配列
		return weatherCodeToIcon(weatherCodeArray, weatherCodeNode, startDay);
	}

	// APIを利用しJSON形式のデータを取得するメソッド
	private static JsonNode fetchJsonNode(LocalDate startDay, LocalDate endDay, String latitude, String longitude)
			throws IOException, InterruptedException {

		// URL,client,request,requestの作成
		final String URL_FORMAT = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=weather_code&timezone=Asia/Tokyo&start_date=%s&end_date=%s";
		String url = String.format(URL_FORMAT, latitude, longitude, startDay, endDay);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		HttpResponse<String> response;
		ObjectMapper mapper = new ObjectMapper();

		response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode root = mapper.readTree(response.body());

		// 天気コードが取得できているか確認
		if (!root.has("daily") || !root.get("daily").has("weather_code")) {
			throw new IllegalArgumentException("入力された期間の天気コードは取得できません。");
		}
		return root.get("daily").get("weather_code");

	}

	// 受け取った天気コードを絵文字に変換するメソッド
	private static String[] weatherCodeToIcon(int weatherCodeArray[], JsonNode weatherCodeNode, LocalDate displayDay) {

		String[] weatherIcon = new String[weatherCodeNode.size()];
		for (int i = 0; i < weatherCodeNode.size(); i++) {
			weatherCodeArray[i] = weatherCodeNode.get(i).asInt();// 比較するためint型に変換
			switch (weatherCodeArray[i]) {
			case 0, 1, 2:
				weatherIcon[i] = "☀";
				break;
			case 3:
				weatherIcon[i] = "☁";
				break;
			case 45, 48, 56, 57, 51, 53, 55, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99:
				weatherIcon[i] = "☔";
				break;
			case 71, 73, 75, 77, 85, 86:
				weatherIcon[i] = "⛄";
				break;
			}

			displayDay = displayDay.plusDays(1);
		}

		return weatherIcon;
	}

	// csvファイルからデータを読み込むメソッド
	public static Map<LocalDate, String> holidayMap() throws IOException {

		Map<LocalDate, String> holidayMap = new HashMap<>();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
		// ファイルパスの取得
		Path path = Paths.get("src/main/resources/static/csv/syukujitsu.csv");
		List<String> lines = null;
		try {
			// すべての行をListに格納
			lines = Files.readAllLines(path, Charset.forName("Shift_JIS"));
		} catch (IOException e) {
			System.out.println("ファイル読み込み失敗");
			e.printStackTrace();
			return holidayMap;
		}
		// 行の数だけループを回して一行ずつ参照
		for (int i = 1; i < lines.size(); i++) {
			// CSVファイルのため,で要素を分ける
			String[] data = lines.get(i).split(",");
			if (data.length >= 2) {
				LocalDate holiday = LocalDate.parse(data[0], formatter);
				holidayMap.put(holiday, data[1]);
			}
		}
		return holidayMap;
	}

	private void injectWeatherIcon(List<List<CalendarElement>> calendarDate, String weatherPlace) {
		String[] splitString = weatherPlace.split(",");
		String latitude = splitString[0];
		String longitude = splitString[1];

		LocalDate today = LocalDate.now();
		LocalDate weatherEnd = today.plusDays(3);
		LocalDate firstDate = calendarDate.get(0).get(0).getDate();
		List<CalendarElement> lastWeek = calendarDate.get(calendarDate.size()-1);
		LocalDate LastDay = lastWeek.get(lastWeek.size()-1).getDate();
		if(weatherEnd.isBefore(firstDate)||today.isAfter(LastDay)) {
			return;
		}
		
		String[] weatherIcons = getWeatherIcon(latitude, longitude);
		for (List<CalendarElement> week : calendarDate) {
			for (CalendarElement element : week) {
				LocalDate targetDate = element.getDate();
				// 今日(0日後)から3日後までの4日間を判定
				if (targetDate.isEqual(today)) {
					element.setWeatherIcon(weatherIcons[0]); // 今日
				} else if (targetDate.isEqual(today.plusDays(1))) {
					element.setWeatherIcon(weatherIcons[1]); // 1日後
				} else if (targetDate.isEqual(today.plusDays(2))) {
					element.setWeatherIcon(weatherIcons[2]); // 2日後
				} else if (targetDate.isEqual(today.plusDays(3))) {
					element.setWeatherIcon(weatherIcons[3]); // 3日後
				}
			}
		}
	}
}
