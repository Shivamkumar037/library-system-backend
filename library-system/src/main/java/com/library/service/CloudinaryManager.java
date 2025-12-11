package com.library.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class CloudinaryManager {

    private final List<Cloudinary> cloudinaryAccounts = new ArrayList<>();

    // ------------ CONSTRUCTOR (5 ACCOUNTS) ------------
    public CloudinaryManager(
            @Value("${cloudinary.acc1.name}") String n1, @Value("${cloudinary.acc1.key}") String k1, @Value("${cloudinary.acc1.secret}") String s1,
            @Value("${cloudinary.acc2.name}") String n2, @Value("${cloudinary.acc2.key}") String k2, @Value("${cloudinary.acc2.secret}") String s2,
            @Value("${cloudinary.acc3.name}") String n3, @Value("${cloudinary.acc3.key}") String k3, @Value("${cloudinary.acc3.secret}") String s3,
            @Value("${cloudinary.acc4.name}") String n4, @Value("${cloudinary.acc4.key}") String k4, @Value("${cloudinary.acc4.secret}") String s4,
            @Value("${cloudinary.acc5.name}") String n5, @Value("${cloudinary.acc5.key}") String k5, @Value("${cloudinary.acc5.secret}") String s5
    ) {
        addAccount(n1, k1, s1);
        addAccount(n2, k2, s2);
        addAccount(n3, k3, s3);
        addAccount(n4, k4, s4);
        addAccount(n5, k5, s5);
    }

    private void addAccount(String name, String key, String secret) {
        if (name != null && !name.isEmpty()) {
            cloudinaryAccounts.add(new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", name,
                    "api_key", key,
                    "api_secret", secret,
                    "secure", true
            )));
        }
    }


    // ------------ VALIDATION ------------
    private String getTargetFolder(String filename) throws IOException {
        String lower = filename.toLowerCase();

        if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx"))
            return "library-system/documents";

        if (lower.matches(".*\\.(jpg|jpeg|png)$"))
            return "library-system/images";

        throw new IOException("Only PDF, DOCX and Images allowed.");
    }


    // ------------ UPLOAD WITH FAILOVER ------------
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {

        String folder = getTargetFolder(file.getOriginalFilename());
        File tempFile = convert(file);

        Map params = ObjectUtils.asMap("resource_type", "auto", "folder", folder);

        for (int i = 0; i < cloudinaryAccounts.size(); i++) {

            Cloudinary account = cloudinaryAccounts.get(i);

            try {
                Map result = account.uploader().upload(tempFile, params);
                result.put("account_index", i);

                tempFile.delete();
                return result;

            } catch (Exception ex) {
                System.err.println("Failed on Account " + (i + 1) + ": " + ex.getMessage());
            }
        }

        tempFile.delete();
        throw new IOException("All Cloudinary accounts failed.");
    }


    // ------------ DETECT RESOURCE TYPE (for DB) ------------
    public String detectForDB(String publicId) {
        String id = publicId.toLowerCase();

        if (id.contains("images")) return "image";
        if (id.contains("documents")) return "raw";

        return "raw";
    }


    // ------------ DOWNLOAD URL (AUTO) ------------
    public String generateDownloadUrl(String publicId) {

        if (cloudinaryAccounts.isEmpty()) return "";

        String resourceType = detectForDB(publicId);

        return cloudinaryAccounts.get(0).url()
                .resourceType(resourceType)
                .transformation(new Transformation().flags("attachment"))
                .generate(publicId);
    }


    // ------------ PREVIEW URL ------------
    public String generatePreviewUrl(String publicId) {

        if (cloudinaryAccounts.isEmpty()) return "";

        String type = detectForDB(publicId);

        // Images preview
        if (type.equals("image")) {
            return cloudinaryAccounts.get(0).url()
                    .resourceType("image")
                    .transformation(
                            new Transformation()
                                    .width(400)
                                    .crop("limit")
                                    .fetchFormat("jpg")
                    )
                    .generate(publicId);
        }

        // PDF/DOC first page preview
        return cloudinaryAccounts.get(0).url()
                .resourceType("image")
                .transformation(
                        new Transformation()
                                .page(1)
                                .width(400)
                                .crop("limit")
                                .fetchFormat("jpg")
                )
                .generate(publicId);
    }


    // ------------ DELETE FROM ALL ACCOUNTS ------------
    public void deleteFile(String publicId) {

        for (Cloudinary account : cloudinaryAccounts) {
            try { account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image")); } catch (Exception ignored) {}
            try { account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw")); } catch (Exception ignored) {}
            try { account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video")); } catch (Exception ignored) {}
        }
    }


    // ------------ TEMP FILE CREATOR ------------
    private File convert(MultipartFile file) throws IOException {
        File convFile = Files.createTempFile("upload_", file.getOriginalFilename()).toFile();
        try (FileOutputStream out = new FileOutputStream(convFile)) {
            out.write(file.getBytes());
        }
        return convFile;
    }
}
