package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FileListTest {
	public static void main(String[] args) throws IOException {
		Path startPath = Paths.get(".");

		List<Path> csvPath = Files.walk(startPath).filter(path -> path.toString().endsWith(".csv"))
				.filter(path -> !path.startsWith(startPath.resolve("target"))).collect(Collectors.toList());
	}
}