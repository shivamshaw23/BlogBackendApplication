package com.backend.blog.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.backend.blog.services.FileService;

@Service
public class FileServiceImpl implements FileService {

	@Override
	public String uploadImage(String path, MultipartFile file) throws IOException {

		// File name
		String originalName = file.getOriginalFilename();
		if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
			throw new IOException("Invalid file name. Unable to determine file extension.");
		}
		String extension = originalName.substring(originalName.lastIndexOf("."));

		// random name generate file
		String randomID = UUID.randomUUID().toString();
		String fileName1 = randomID.concat(extension);

		// Full path
		Path destinationPath = Paths.get(path, fileName1);

		// create folder if not created
		File directory = new File(path);
		if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new IOException("Failed to create directory for file upload");
			}
		}

		// file copy

		try (InputStream inputStream = file.getInputStream()) {
			Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
		}

		return fileName1;
	}

	@Override
	public InputStream getResource(String path, String fileName) throws FileNotFoundException {
		String fullPath = path + File.separator + fileName;
		InputStream is = new FileInputStream(fullPath);
		// db logic to return inpustream
		return is;
	}

}
