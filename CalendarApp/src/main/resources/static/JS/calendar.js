function getParam(name) {
	return new URLSearchParams(window.location.search).get(name);
}

document.getElementById("language-select").addEventListener("change",function (){
    var selectedLanguage = this.value;
	var year = getParam("year");
	var month = getParam("month");
	var weatherPlace = getParam("weatherPlace");
	
	var params = new URLSearchParams();
	if(year) params.set("year",year);
   if(month) params.set("month",month);
   if(weatherPlace) params.set("weatherPlace",weatherPlace);
   
   var basePath = (selectedLanguage === "en" ) ? "/calendar/en" : "/calendar";
   
   window.location.href = basePath + "?" + params.toString();
});
const translations = {
	ja: {
		language:言語,
		japaneseSelect:日本語,
		englishSelect:英語,
		weather:天気,
		tokyoWeather:東京,
		newYorkWeather:ニューヨーク
	},
	en: {
		language:language,
			japaneseSelect:Japanese,
			englishSelect:English,
			weather:weather,
			tokyoWeather:Tokyo,
			newYorkWeather:NewYork
	}
}

const selectBox = document.getElementById('language-select');


