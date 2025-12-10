package com.library.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Service
public class CloudinaryManager {

    private final Cloudinary cloudinary;

    // Environment variables se credentials load karein
    public CloudinaryManager(
            @Value("${CLOUDINARY_CLOUD_NAME}") String cloudName,
            @Value("${CLOUDINARY_API_KEY}") String apiKey,
            @Value("${CLOUDINARY_API_SECRET}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true));
    }

    // File Upload Method
    public Map<String, String> upload(MultipartFile multipartFile) throws IOException {
        // MultipartFile ko temporary Java File mein convert karein
        File fileToUpload = convert(multipartFile);

        try {
            // Upload parameters: resource_type 'auto' rakha hai, folder set kiya hai
            Map<?, ?> result = cloudinary.uploader().upload(fileToUpload, ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "library-system-files"));

            // Thumbnail URL generate karein (Cloudinary transformation use karke)
            String publicId = (String) result.get("public_id");
            String thumbnailUrl = cloudinary.url().transformation(
                            // Width 300px, quality auto, crop fill
                            new com.cloudinary.Transformation().width(300).quality("auto").crop("fill"))
                    .type("upload").generate(publicId);

            // Required results return karein
            return Map.of(
                    "url", (String) result.get("secure_url"),
                    "public_id", publicId,
                    "thumbnail_url", thumbnailUrl
            );
        } finally {
            // Temporary file delete karein
            if (fileToUpload != null && fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }
    }

    // File Deletion Method
    public void delete(String publicId) throws Exception {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    // Utility to convert MultipartFile to File
    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}