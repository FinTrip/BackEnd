package org.example.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface UploadImageFile {
    String uploadImage(MultipartFile file) throws IOException;
}