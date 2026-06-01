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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class CalendarService {

	// 日本語でカレンダーの要素を返す
	public static List<List<CalendarElement>> returnCalendarElement(int currentYear, int currentMonth,
			String currentPlace, boolean isEnglish) {

		// カレンダーの左上の日の設定
		LocalDate calendarStart = LocalDate.of(currentYear, currentMonth, 1);
		calendarStart = backDayToSunday(calendarStart);

		// HTMLに渡すカレンダーの要素を取る
		List<List<CalendarElement>> japaneseCalendarElement;
		japaneseCalendarElement = getDateElement(currentMonth, calendarStart, isEnglish);
		try {
			injectWeatherIcon(japaneseCalendarElement, currentPlace);
		} catch (IllegalArgumentException | IOException e) {
			System.out.println("通信エラーが発生しました。");
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		return japaneseCalendarElement;
	}

	// 英語で曜日名を返すメソッド
	static String[] dayOfWeekEnglish() {
		final String[] dayOfWeekStrings = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
				"Saturday" };
		return dayOfWeekStrings;
	}

	// 日本語で曜日名を返すメソッド
	static String[] dayOfWeekJapanese() {
		final String[] dayOfWeekStrings = { "日", "月", "火", "水", "木", "金", "土" };
		return dayOfWeekStrings;
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
	private static List<List<CalendarElement>> getDateElement(int currentMonth, LocalDate displayDate,
			boolean isEnglish) {

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

			// textの準備
			for (int j = 0; j < 7; j++) {
				String text;
				if (day == 1 || i == 0 && j == 0) {
					// 表示する日が月初かカレンダーの左上なら月日を表示
					text = month + "/" + day;
				} else {
					// 一般的な場合は日のみ表示
					if (isEnglish) {
						text = Integer.toString(day);
					} else {
						text = day + "日";
					}
				}

				// 年の繰り上げ、繰り下げを加味して先月、翌月の判定用の変数の作成
				int lastMonth = (currentMonth - 1) % 12;
				if (lastMonth == 0) {
					lastMonth = 12;
				}
				int nextMonth = (currentMonth + 1) % 12;
				if (nextMonth == 0) {
					nextMonth = 12;
				}

				// クラスの要素をそれぞれ判定してリストに格納
				boolean isToday = displayDate.isEqual(today);
				boolean isLastMonth = (lastMonth == month);
				boolean isNextMonth = (nextMonth == month);
				String holidayName;
				if (isEnglish) {
					// 祝日の翻訳
					holidayName = toEnglishHoliday(holidayDate.get(displayDate));
				} else {
					holidayName = holidayDate.get(displayDate);
				}
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

	// HTMLに情報を送るクラス
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
	public static String[] getWeatherIcon(LocalDate today, LocalDate weatherEnd, String latitude, String longitude)
			throws IOException, InterruptedException {

		// URL,client,request,requestの作成
		final String URL_FORMAT = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=weather_code&timezone=Asia/Tokyo&start_date=%s&end_date=%s";
		String url = String.format(URL_FORMAT, latitude, longitude, today, weatherEnd);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
		HttpResponse<String> response;
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;

		// 天気コードの取得
		response = client.send(request, HttpResponse.BodyHandlers.ofString());
		root = mapper.readTree(response.body());

		// 天気コードが取得できているか確認
		if (!root.has("daily") || !root.get("daily").has("weather_code")) {
			throw new IllegalArgumentException("天気コードが取得できていません。");
		}

		return weatherCodeToIcon(root.get("daily").get("weather_code"), today);
	}

	// 受け取った天気コードを絵文字に変換するメソッド
	private static String[] weatherCodeToIcon(JsonNode weatherCodeNode, LocalDate displayDay) {

		int[] weatherCodeArray = new int[weatherCodeNode.size()];// 天気コードを格納する配列
		// それぞれのコードに対応する絵文字に変換する
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

	// CSVファイルからデータを読み込むメソッド
	public static Map<LocalDate, String> holidayMap() throws IOException {

		// 読み込むためのMapの作成
		Map<LocalDate, String> holidayMap = new HashMap<>();

		// ファイルパスの取得
		Path path = Paths.get("src/main/resources/static/csv/syukujitsu.csv");
		List<String> lines;
		try {
			// すべての行をListに格納
			lines = Files.readAllLines(path, Charset.forName("Shift_JIS"));
		} catch (IOException e) {
			System.out.println("ファイル読み込み失敗");
			e.printStackTrace();
			return holidayMap;
		}

		// CSVファイルのの文字列をLocalDate型に変換するためのフォーマット
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
		// 行の数だけループを回して一行ずつ参照
		for (int i = 1; i < lines.size(); i++) {
			// CSVファイルのため,で要素を分ける
			String[] data = lines.get(i).split(",");
			// 要素が２つ存在するもののみパースしてputする
			if (data.length == 2) {
				LocalDate holiday = LocalDate.parse(data[0], formatter);
				holidayMap.put(holiday, data[1]);
			}
		}

		return holidayMap;
	}

	// 天気アイコンをCalendarElement注入する関数
	static void injectWeatherIcon(List<List<CalendarElement>> calendarDate, String weatherPlace)
			throws IOException, InterruptedException {

		// 受け取った座標を緯度と経度分ける
		String[] splitString = weatherPlace.split(",");
		String latitude = splitString[0];
		String longitude = splitString[1];

		// 天気が必要な月かの判断、要らなければ注入しないで戻す
		LocalDate today = LocalDate.now();
		LocalDate weatherEnd = today.plusDays(3);
		LocalDate firstDate = calendarDate.get(0).get(0).getDate();
		List<CalendarElement> lastWeek = calendarDate.get(calendarDate.size() - 1);
		LocalDate LastDay = lastWeek.get(lastWeek.size() - 1).getDate();
		if (weatherEnd.isBefore(firstDate) || today.isAfter(LastDay)) {
			return;
		}

		// ４日間分の天気のアイコンを取得
		String[] weatherIcons = getWeatherIcon(today, weatherEnd, latitude, longitude);
		for (List<CalendarElement> week : calendarDate) {
			for (CalendarElement element : week) {
				LocalDate targetDate = element.getDate();

				// ４日分の天気アイコンをそれぞれの対応する日付に注入
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

	// 日本語の祝日を英語に翻訳する
	private static String toEnglishHoliday(String japaneseHoliday) {

		// 祝日でなければそのままnullを返却
		if (japaneseHoliday == null) {
			return null;
		}

		// 翻訳用のMapの作成
		final Map<String, String> holidayMap = new HashMap<>();
		holidayMap.put("元日", "New Year's Day");
		holidayMap.put("成人の日", "Coming-of-Age Day");
		holidayMap.put("建国記念日", "National Foundation Day");
		holidayMap.put("天皇誕生日", "Emperor's Birthday");
		holidayMap.put("春分の日", "Vernal Equinox Day");
		holidayMap.put("昭和の日", "Showa Day");
		holidayMap.put("憲法記念日", "Constitusion Memorial Day");
		holidayMap.put("みどりの日", "greenery Day");
		holidayMap.put("こどもの日", "Children's Day");
		holidayMap.put("海の日", "Marine Day");
		holidayMap.put("山の日", "Mountain Day");
		holidayMap.put("敬老の日", "Respect-for-the-Aged Day");
		holidayMap.put("秋分の日", "Autumnal Equinox Day");
		holidayMap.put("スポーツの日", "Sports Day");
		holidayMap.put("文化の日", "Culture Day");
		holidayMap.put("勤労感謝の日", "Labor Thanksgiving Day");
		holidayMap.put("休日", "Holiday");

		return holidayMap.get(japaneseHoliday);
	}

}
