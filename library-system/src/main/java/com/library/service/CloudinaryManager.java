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

    // List to hold all 5 Cloudinary clients for Failover System
    private final List<Cloudinary> cloudinaryClients = new ArrayList<>();

    // Constructor: Load all 5 accounts from application.properties
    public CloudinaryManager(
            // Account 1
            @Value("${cloudinary.acc1.name}") String name1, @Value("${cloudinary.acc1.key}") String key1, @Value("${cloudinary.acc1.secret}") String secret1,
            // Account 2
            @Value("${cloudinary.acc2.name}") String name2, @Value("${cloudinary.acc2.key}") String key2, @Value("${cloudinary.acc2.secret}") String secret2,
            // Account 3
            @Value("${cloudinary.acc3.name}") String name3, @Value("${cloudinary.acc3.key}") String key3, @Value("${cloudinary.acc3.secret}") String secret3,
            // Account 4
            @Value("${cloudinary.acc4.name}") String name4, @Value("${cloudinary.acc4.key}") String key4, @Value("${cloudinary.acc4.secret}") String secret4,
            // Account 5
            @Value("${cloudinary.acc5.name}") String name5, @Value("${cloudinary.acc5.key}") String key5, @Value("${cloudinary.acc5.secret}") String secret5
    ) {
        // Initialize all clients
        addClient(name1, key1, secret1);
        addClient(name2, key2, secret2);
        addClient(name3, key3, secret3);
        addClient(name4, key4, secret4);
        addClient(name5, key5, secret5);
    }

    // Helper method to add valid clients only
    private void addClient(String name, String key, String secret) {
        if (name != null && !name.isEmpty() && !key.isEmpty()) {
            cloudinaryClients.add(new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", name,
                    "api_key", key,
                    "api_secret", secret,
                    "secure", true)));
        }
    }

    // Logic: Try Account 1 -> If fail/full -> Try Account 2 -> ... -> Account 5
    public Map<String, String> upload(MultipartFile multipartFile) throws IOException {
        File fileToUpload = convert(multipartFile);
        Exception lastException = null;

        try {
            for (int i = 0; i < cloudinaryClients.size(); i++) {
                Cloudinary client = cloudinaryClients.get(i);
                try {
                    // 1. Upload File (Let Cloudinary decide auto/raw/image)
                    // "auto" means Cloudinary will check if it's a PDF, Image, or Video
                    Map<?, ?> result = client.uploader().upload(fileToUpload, ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "library-system-files"));

                    String publicId = (String) result.get("public_id");
                    
                    // IMPORTANT: Get the resource type (image, raw, or video) from the response
                    // This is crucial to prevent broken links
                    String resourceType = (String) result.get("resource_type");

                    // 2. Generate Safe Download URL using SDK
                    // Instead of string replacement, we use the SDK builder.
                    // .flags("attachment") forces the browser/app to download the file instead of opening it.
                    String downloadUrl = client.url()
                            .resourceType(resourceType) // Uses correct type (fixes ERR_INVALID_RESPONSE)
                            .transformation(new Transformation().flags("attachment")) 
                            .generate(publicId);

                    // 3. Generate Thumbnail (Only if it is an image)
                    String thumbnailUrl = downloadUrl; // Default to download link
                    if ("image".equals(resourceType)) {
                        try {
                            thumbnailUrl = client.url()
                                    .resourceType(resourceType)
                                    .transformation(new Transformation().width(300).quality("auto").crop("fill"))
                                    .generate(publicId);
                        } catch (Exception ignored) {
                            // If thumbnail fails, keep original link
                        }
                    }

                    System.out.println("Upload Success on Account #" + (i + 1) + " | Type: " + resourceType);

                    // Return the standardized response
                    return Map.of(
                            "url", downloadUrl, 
                            "public_id", publicId,
                            "thumbnail_url", thumbnailUrl,
                            "account_used", "Account " + (i + 1)
                    );

                } catch (Exception e) {
                    System.err.println("Failed on Account #" + (i + 1) + ": " + e.getMessage());
                    lastException = e;
                    // Loop continues to next account if this one fails
                }
            }

            // If all 5 accounts fail
            throw new IOException("All 5 Cloudinary accounts failed. Last error: " +
                    (lastException != null ? lastException.getMessage() : "Unknown"));

        } finally {
            // Cleanup: Delete temporary file from server
            if (fileToUpload != null && fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }
    }

    public void delete(String publicId) {
        // We try to delete from all accounts because we don't know which one holds the file.
        // We also try deleting as 'image', 'video', and 'raw' to be 100% sure it's gone.
        for (Cloudinary client : cloudinaryClients) {
            try {
                client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
                client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video"));
                client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            } catch (Exception ignored) {
                // Ignore errors (file might not exist on this specific account)
            }
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
