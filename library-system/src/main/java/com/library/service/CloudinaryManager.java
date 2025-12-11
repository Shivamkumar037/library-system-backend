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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CloudinaryManager {

    private final List<Cloudinary> cloudinaryAccounts = new ArrayList<>();

    // Constructor Injection (Unchanged)
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

    // --- LOGIC 1: Validation ---
    private String getTargetFolder(String filename) throws IOException {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")) return "library-system/documents";
        if (lower.matches(".*\\.(jpg|jpeg|png)$")) return "library-system/images";
        throw new IOException("Only PDF, DOCX, and Image files are allowed.");
    }

    // --- LOGIC 2: Multi-Account Upload (Failover) ---
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {
        String folder = getTargetFolder(Objects.requireNonNull(file.getOriginalFilename()));
        File tempFile = convert(file);

        Map params = ObjectUtils.asMap("resource_type", "auto", "folder", folder);

        for (int i = 0; i < cloudinaryAccounts.size(); i++) {
            Cloudinary currentAccount = cloudinaryAccounts.get(i);
            try {
                Map result = currentAccount.uploader().upload(tempFile, params);
                result.put("account_index", i);
                
                if (tempFile.exists()) tempFile.delete();

                System.out.println("Upload successful on Account " + (i + 1));
                return result;

            } catch (Exception e) {
                System.err.println("Upload failed on Account " + (i + 1) + ": " + e.getMessage());
                System.err.println("Switching to next account...");
            }
        }

        if (tempFile.exists()) tempFile.delete();
        throw new IOException("All 5 Cloudinary accounts are full or unavailable. Upload failed.");
    }

    // --- LOGIC 3: Smart URL Generation (FIXED) ---
    public String generateDownloadUrl(String publicId, String resourceType) {
        if (cloudinaryAccounts.isEmpty()) return "";
        
        Transformation t = new Transformation().flags("attachment");
        
        // 🔥 FIX: Agar resourceType 'image' hai, lekin publicId document folder mein hai (jaisa ki aapka case hai), 
        // toh hum download ke liye format ko PDF/DOCX mein force kar denge.
        if ("image".equals(resourceType) && publicId.contains("documents")) {
            t.fetchFormat("pdf"); // PDF files ko PDF format mein download karna force karega
            resourceType = "raw"; // Best practice: download ke liye raw resource type use karein
        }
        
        return cloudinaryAccounts.get(0).url()
                .resourceType(resourceType)
                .transformation(t)
                .generate(publicId);
    }

    public String generatePreviewUrl(String publicId, String resourceType) {
        if (cloudinaryAccounts.isEmpty()) return "";
        // Preview hamesha image format mein generate hoga (page 1)
        if ("image".equals(resourceType) || "pdf".equals(resourceType)) {
            return cloudinaryAccounts.get(0).url()
                    .resourceType("image")
                    .transformation(new Transformation().width(400).crop("limit").page(1).fetchFormat("jpg"))
                    .generate(publicId);
        }
        return "https://placehold.co/400x600?text=Document+Preview";
    }

    // --- LOGIC 4: Multi-Account Delete ---
    public void deleteFile(String publicId) {
        for (Cloudinary account : cloudinaryAccounts) {
            try {
                account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
                account.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
            } catch (Exception ignored) {}
        }
    }

    // 🔥 CRITICAL FIX: Use Files.createTempFile to avoid Permission Denied
    private File convert(MultipartFile file) throws IOException {
        File convFile = Files.createTempFile("upload_", Objects.requireNonNull(file.getOriginalFilename())).toFile();
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
