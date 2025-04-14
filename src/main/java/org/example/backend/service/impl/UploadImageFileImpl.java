package org.example.backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.example.backend.service.UploadImageFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadImageFileImpl implements UploadImageFile {

    private final Cloudinary cloudinary;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp"
    ));

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        validateImage(file);
        
        assert file.getOriginalFilename() != null;
        String publicValue = generatePublicValue(file.getOriginalFilename());
        log.info("publicValue is: {}", publicValue);
        String extension = getFileName(file.getOriginalFilename())[1];
        log.info("extension is: {}", extension);
        File fileUpload = convert(file);
        log.info("fileUpload is: {}", fileUpload);
        
        try {
            cloudinary.uploader().upload(fileUpload, ObjectUtils.asMap("public_id", publicValue));
            return cloudinary.url().generate(StringUtils.join(publicValue, ".", extension));
        } finally {
            cleanDisk(fileUpload);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "File size exceeds maximum limit of 5MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.FILE_UPLOAD_ERROR, "Invalid file type. Allowed types: JPEG, PNG, GIF, WEBP");
        }
    }

    private File convert(MultipartFile file) throws IOException {
        assert file.getOriginalFilename() != null;
        File convFile = new File(StringUtils.join(generatePublicValue(file.getOriginalFilename()), ".", getFileName(file.getOriginalFilename())[1]));
        try(InputStream is = file.getInputStream()) {
            Files.copy(is, convFile.toPath());
        }
        return convFile;
    }

    private void cleanDisk(File file) {
        try {
            log.info("Cleaning up temporary file: {}", file.toPath());
            Path filePath = file.toPath();
            Files.delete(filePath);
        } catch (IOException e) {
            log.error("Error cleaning up temporary file: {}", e.getMessage());
            // We don't throw here as this is cleanup code
        }
    }

    private String generatePublicValue(String originalName){
        String fileName = getFileName(originalName)[0];
        return StringUtils.join(UUID.randomUUID().toString(), "_", fileName);
    }

    private String[] getFileName(String originalName) {
        return originalName.split("\\.");
    }
}