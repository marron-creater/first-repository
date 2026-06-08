package com.example.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CalendarRequestDto {
	@NotBlank
	@Pattern(regexp = "^(19[5-9][0-9]|2[0-9]{3})$")
	private String year;

	@NotBlank
	@Pattern(regexp = "^(0?[1-9]|1[0-2])$")
	private String month;

	@NotBlank
	@Pattern(regexp = "^(35\\.6785,139\\.6823|40\\.71,-74\\.01)$")
	private String weatherPlace;
}