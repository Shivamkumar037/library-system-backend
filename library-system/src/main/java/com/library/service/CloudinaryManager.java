package com.library.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;package com.library.service;

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
    
    /**
     * Files ko validate karta hai aur unhe 'documents' folder mein rakhne ke liye subfolder return karta hai.
     * @throws IOException agar file PDF ya MS Word (.doc, .docx) nahi hai.
     */
    private String validateAndDetermineSubFolder(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IOException("Filename missing.");
        }
        
        String lowerCaseName = originalFilename.toLowerCase();
        
        // Allowed extensions: pdf, doc, docx
        if (lowerCaseName.matches(".*\\.(pdf|doc|docx)$")) {
            return "documents";
        } 
        
        // Agar file allowed list mein nahi hai, toh error throw karein
        throw new IOException("Only PDF (.pdf) and MS Word (.doc, .docx) files are allowed for upload.");
    }

    // Logic: Try Account 1 -> If fail/full -> Try Account 2 -> ... -> Account 5
    public Map<String, String> upload(MultipartFile multipartFile) throws IOException {
        // File validation and folder determination (NEW STEP)
        String subFolder = validateAndDetermineSubFolder(multipartFile);
        String targetFolder = "library-system-files/" + subFolder;
        
        File fileToUpload = convert(multipartFile);
        Exception lastException = null;

        try {
            for (int i = 0; i < cloudinaryClients.size(); i++) {
                Cloudinary client = cloudinaryClients.get(i);
                try {
                    // 1. Upload File
                    Map<?, ?> result = client.uploader().upload(fileToUpload, ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", targetFolder)); 
                            
                    String publicId = (String) result.get("public_id");
                    String resourceType = (String) result.get("resource_type");
                    
                    // 2. Generate Safe Download URL (Download API equivalent)
                    // .flags("attachment") ensures direct download, not preview.
                    String downloadUrl = client.url()
                            .resourceType(resourceType) 
                            .transformation(new Transformation().flags("attachment")) 
                            .generate(publicId);

                    // 3. Generate Thumbnail/Preview URL (First page as image)
                    // We generate a JPG thumbnail of the first page (page=1)
                    String previewUrl = client.url()
                        .resourceType("image") // Documents are treated as images for thumbnail generation
                        .transformation(new Transformation().width(300).height(400).crop("fill").fetchFormat("jpg").page("1"))
                        .generate(publicId);

                    System.out.println("Upload Success on Account #" + (i + 1) + " | Type: " + resourceType + " | Folder: " + targetFolder);

                    // Return both URLs: 'url' (download) and 'thumbnail_url' (preview)
                    return Map.of(
                            "url", downloadUrl, // Frontend will use this for the download button
                            "public_id", publicId,
                            "thumbnail_url", previewUrl, // Frontend will use this for home screen display
                            "resource_type", resourceType, 
                            "folder", targetFolder,        
                            "account_used", "Account " + (i + 1)
                    );

                } catch (Exception e) {
                    System.err.println("Failed on Account #" + (i + 1) + ": " + e.getMessage());
                    lastException = e;
                }
            }

            throw new IOException("All 5 Cloudinary accounts failed. Last error: " +
                    (lastException != null ? lastException.getMessage() : "Unknown"));

        } finally {
            if (fileToUpload != null && fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }
    }

    public void delete(String publicId) {
        for (Cloudinary client : cloudinaryClients) {
            try {
                // Try deleting as image, video, and raw to be safe
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

    // Helper method to determine subfolder based on file extension
    private String determineSubFolder(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "others";
        }
        
        String lowerCaseName = originalFilename.toLowerCase();
        
        // Checking for common image extensions
        if (lowerCaseName.matches(".*\\.(jpe?g|png|gif|webp|svg)$")) {
            return "images";
        } 
        // Checking for common video extensions
        else if (lowerCaseName.matches(".*\\.(mp4|mov|avi|wmv|mkv)$")) {
            return "videos";
        } 
        // Checking for common document extensions (includes PDF)
        else if (lowerCaseName.matches(".*\\.(pdf|doc|docx|txt|xls|xlsx|ppt|pptx)$")) {
            return "documents";
        }
        
        return "others";
    }

    // Logic: Try Account 1 -> If fail/full -> Try Account 2 -> ... -> Account 5
    public Map<String, String> upload(MultipartFile multipartFile) throws IOException {
        File fileToUpload = convert(multipartFile);
        Exception lastException = null;

        // --- NEW LOGIC: Determine Target Folder ---
        String subFolder = determineSubFolder(multipartFile);
        String targetFolder = "library-system-files/" + subFolder; // Subfolder ke saath path
        // ------------------------------------------

        try {
            for (int i = 0; i < cloudinaryClients.size(); i++) {
                Cloudinary client = cloudinaryClients.get(i);
                try {
                    // 1. Upload File (Now using the dynamically determined folder)
                    Map<?, ?> result = client.uploader().upload(fileToUpload, ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", targetFolder)); // <-- Dynamic folder used here

                    String publicId = (String) result.get("public_id");
                    String resourceType = (String) result.get("resource_type"); // Cloudinary ka final type
                    
                    // 2. Generate Safe Download URL using SDK (Download Fix)
                    String downloadUrl = client.url()
                            .resourceType(resourceType) 
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
                        } catch (Exception ignored) {}
                    }

                    System.out.println("Upload Success on Account #" + (i + 1) + " | Type: " + resourceType + " | Folder: " + targetFolder);

                    // Return the standardized response
                    return Map.of(
                            "url", downloadUrl, 
                            "public_id", publicId,
                            "thumbnail_url", thumbnailUrl,
                            "resource_type", resourceType, // Added resource type to map
                            "folder", targetFolder,        // Added folder to map
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
        for (Cloudinary client : cloudinaryClients) {
            try {
                // Try deleting as image, video, and raw to be safe
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

