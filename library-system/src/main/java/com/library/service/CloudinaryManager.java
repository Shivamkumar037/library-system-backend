package com.library.service;

import com.cloudinary.Cloudinary;
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

    // List to hold all 5 Cloudinary clients
    private final List<Cloudinary> cloudinaryClients = new ArrayList<>();

    public CloudinaryManager(
            // Account 1
            @Value("${cloudinary.acc1.name}") String name1,
            @Value("${cloudinary.acc1.key}") String key1,
            @Value("${cloudinary.acc1.secret}") String secret1,
            // Account 2
            @Value("${cloudinary.acc2.name}") String name2,
            @Value("${cloudinary.acc2.key}") String key2,
            @Value("${cloudinary.acc2.secret}") String secret2,
            // Account 3
            @Value("${cloudinary.acc3.name}") String name3,
            @Value("${cloudinary.acc3.key}") String key3,
            @Value("${cloudinary.acc3.secret}") String secret3,
            // Account 4
            @Value("${cloudinary.acc4.name}") String name4,
            @Value("${cloudinary.acc4.key}") String key4,
            @Value("${cloudinary.acc4.secret}") String secret4,
            // Account 5
            @Value("${cloudinary.acc5.name}") String name5,
            @Value("${cloudinary.acc5.key}") String key5,
            @Value("${cloudinary.acc5.secret}") String secret5
    ) {
        // Initialize all clients and add to list
        addClient(name1, key1, secret1);
        addClient(name2, key2, secret2);
        addClient(name3, key3, secret3);
        addClient(name4, key4, secret4);
        addClient(name5, key5, secret5);
    }

    private void addClient(String name, String key, String secret) {
        // Ensure credentials are valid before adding
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
        Map<?, ?> result = null;
        Exception lastException = null;

        try {
            for (int i = 0; i < cloudinaryClients.size(); i++) {
                Cloudinary client = cloudinaryClients.get(i);
                try {
                    // Upload karne ki koshish karein
                    result = client.uploader().upload(fileToUpload, ObjectUtils.asMap(
                            "resource_type", "auto",
                            "folder", "library-system-files"));

                    // Agar upload successful hua, toh yahi ruk jayein aur result return karein
                    String publicId = (String) result.get("public_id");
                    String thumbnailUrl = client.url().transformation(
                                    new com.cloudinary.Transformation().width(300).quality("auto").crop("fill"))
                            .type("upload").generate(publicId);

                    System.out.println("Upload successful on Account #" + (i + 1));

                    return Map.of(
                            "url", (String) result.get("secure_url"),
                            "public_id", publicId,
                            "thumbnail_url", thumbnailUrl,
                            "account_used", "Account " + (i + 1)
                    );

                } catch (Exception e) {
                    // Agar current account full hai ya error aaya, toh log karein aur loop continue karein
                    System.err.println("Failed on Account #" + (i + 1) + ": " + e.getMessage());
                    lastException = e;
                }
            }

            // Agar 5 accounts try karne ke baad bhi upload nahi hua
            throw new IOException("All 5 Cloudinary accounts failed. Please check storage limits. Last error: " +
                    (lastException != null ? lastException.getMessage() : "Unknown"));

        } finally {
            // Temporary file delete karein
            if (fileToUpload != null && fileToUpload.exists()) {
                fileToUpload.delete();
            }
        }
    }

    public void delete(String publicId) throws Exception {
        // Delete ke liye humein pata nahi kis account pe file hai, isliye sab par delete command bhejenge.
        // Jo account file hold nahi karta, wo ignore karega.
        for (Cloudinary client : cloudinaryClients) {
            try {
                client.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                // Ignore delete errors on secondary accounts
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
