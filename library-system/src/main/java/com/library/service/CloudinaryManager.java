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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CloudinaryManager {

    private final List<Cloudinary> cloudinaryClients = new ArrayList<>();

    // Constructor to load accounts
    public CloudinaryManager(
            @Value("${cloudinary.acc1.name}") String name1, @Value("${cloudinary.acc1.key}") String key1, @Value("${cloudinary.acc1.secret}") String secret1,
            @Value("${cloudinary.acc2.name}") String name2, @Value("${cloudinary.acc2.key}") String key2, @Value("${cloudinary.acc2.secret}") String secret2,
            @Value("${cloudinary.acc3.name}") String name3, @Value("${cloudinary.acc3.key}") String key3, @Value("${cloudinary.acc3.secret}") String secret3,
            @Value("${cloudinary.acc4.name}") String name4, @Value("${cloudinary.acc4.key}") String key4, @Value("${cloudinary.acc4.secret}") String secret4,
            @Value("${cloudinary.acc5.name}") String name5, @Value("${cloudinary.acc5.key}") String key5, @Value("${cloudinary.acc5.secret}") String secret5
    ) {
        addClient(name1, key1, secret1);
        addClient(name2, key2, secret2);
        addClient(name3, key3, secret3);
        addClient(name4, key4, secret4);
        addClient(name5, key5, secret5);
    }

    private void addClient(String name, String key, String secret) {
        if (name != null && !name.isEmpty() && !key.isEmpty()) {
            cloudinaryClients.add(new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", name,
                    "api_key", key,
                    "api_secret", secret,
                    "secure", true)));
        }
    }

    // --- 1. Validation Logic ---
    private String validateAndDetermineSubFolder(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) throw new IOException("Filename missing.");

        String lowerCaseName = originalFilename.toLowerCase();

        // STRICT CHECK: Only PDF or Word Docs allowed
        if (lowerCaseName.endsWith(".pdf") || lowerCaseName.endsWith(".doc") || lowerCaseName.endsWith(".docx")) {
            return "documents";
        }

        throw new IOException("Invalid file type. Only PDF and MS Word (.doc, .docx) files are allowed.");
    }

    // --- 2. Upload Logic ---
    public Map<String, String> upload(MultipartFile multipartFile) throws IOException {
        String subFolder = validateAndDetermineSubFolder(multipartFile);
        String targetFolder = "library-system-files/" + subFolder;

        File fileToUpload = convert(multipartFile);
        Exception lastException = null;

        try {
            for (int i = 0; i < cloudinaryClients.size(); i++) {
                Cloudinary client = cloudinaryClients.get(i);
                try {
                    // Upload as 'auto' (Cloudinary detects pdf/raw)
                    Map<?, ?> result = client.uploader().upload(fileToUpload, ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", targetFolder));

                    String publicId = (String) result.get("public_id");
                    String resourceType = (String) result.get("resource_type");

                    // Generate basic download URL (Force attachment)
                    String downloadUrl = generateDownloadUrl(publicId, resourceType);

                    // Generate Thumbnail (Page 1)
                    String thumbnailUrl = generatePageImage(publicId, 1);

                    System.out.println("Upload Success on Account #" + (i + 1));

                    return Map.of(
                            "url", downloadUrl,
                            "public_id", publicId,
                            "thumbnail_url", thumbnailUrl,
                            "resource_type", resourceType,
                            "folder", targetFolder,
                            "account_used", "Account " + (i + 1)
                    );

                } catch (Exception e) {
                    lastException = e;
                }
            }
            throw new IOException("All Cloudinary accounts failed. " + (lastException != null ? lastException.getMessage() : ""));
        } finally {
            if (fileToUpload != null && fileToUpload.exists()) fileToUpload.delete();
        }
    }

    // --- 3. URL Generation Helpers ---

    // Generate FORCE DOWNLOAD Link
    public String generateDownloadUrl(String publicId, String resourceType) {
        // We pick the first client to generate URL (URLs are standard across clients if publicId is known)
        return cloudinaryClients.get(0).url()
                .resourceType(resourceType)
                .transformation(new Transformation().flags("attachment"))
                .generate(publicId);
    }

    // Generate INLINE VIEW Link (Full File)
    public String generateViewUrl(String publicId, String resourceType) {
        return cloudinaryClients.get(0).url()
                .resourceType(resourceType)
                .transformation(new Transformation().flags("inline")) // Browsers will try to open it
                .generate(publicId);
    }

    // Generate Specific Page Image (For Preview 4-5 pages)
    public String generatePageImage(String publicId, int pageNumber) {
        try {
            return cloudinaryClients.get(0).url()
                    .resourceType("image") // Treat PDF pages as images
                    .transformation(new Transformation()
                            .width(800) // Decent reading width
                            .crop("limit")
                            .page(String.valueOf(pageNumber)) // Fetch specific page
                            .fetchFormat("jpg")) // Convert to lightweight JPG
                    .generate(publicId);
        } catch (Exception e) {
            return ""; // Return empty if page generation fails (e.g., page doesn't exist)
        }
    }

    public void delete(String publicId) {
        for (Cloudinary client : cloudinaryClients) {
            try {
                client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            } catch (Exception ignored) { }
        }
    }

    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }
}
