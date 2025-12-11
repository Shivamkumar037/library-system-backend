package com.library.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.*;

@Service
public class CloudinaryManager {

    // Account 0
    @Value("${cloudinary.accounts[0].cloud-name}") private String c0Name;
    @Value("${cloudinary.accounts[0].api-key}") private String c0Key;
    @Value("${cloudinary.accounts[0].api-secret}") private String c0Secret;

    // Account 1
    @Value("${cloudinary.accounts[1].cloud-name}") private String c1Name;
    @Value("${cloudinary.accounts[1].api-key}") private String c1Key;
    @Value("${cloudinary.accounts[1].api-secret}") private String c1Secret;

    // Account 2
    @Value("${cloudinary.accounts[2].cloud-name}") private String c2Name;
    @Value("${cloudinary.accounts[2].api-key}") private String c2Key;
    @Value("${cloudinary.accounts[2].api-secret}") private String c2Secret;

    // Account 3
    @Value("${cloudinary.accounts[3].cloud-name}") private String c3Name;
    @Value("${cloudinary.accounts[3].api-key}") private String c3Key;
    @Value("${cloudinary.accounts[3].api-secret}") private String c3Secret;

    // Account 4
    @Value("${cloudinary.accounts[4].cloud-name}") private String c4Name;
    @Value("${cloudinary.accounts[4].api-key}") private String c4Key;
    @Value("${cloudinary.accounts[4].api-secret}") private String c4Secret;

    private final List<Cloudinary> clients = new ArrayList<>();

    @PostConstruct
    public void init() {
        addClient(c0Name, c0Key, c0Secret);
        addClient(c1Name, c1Key, c1Secret);
        addClient(c2Name, c2Key, c2Secret);
        addClient(c3Name, c3Key, c3Secret);
        addClient(c4Name, c4Key, c4Secret);
    }

    private void addClient(String name, String key, String secret) {
        if (name == null || name.isEmpty()) return;
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", name);
        config.put("api_key", key);
        config.put("api_secret", secret);
        clients.add(new Cloudinary(config));
    }

    public UploadResult uploadFile(MultipartFile file) throws IOException {
        // Failover logic: Try account 0, if fail/full, try 1, etc.
        for (int i = 0; i < clients.size(); i++) {
            try {
                Cloudinary client = clients.get(i);
                Map params = ObjectUtils.asMap(
                        "resource_type", "auto",
                        "folder", "library_books"
                );
                Map uploadResult = client.uploader().upload(file.getBytes(), params);

                return new UploadResult(
                        (String) uploadResult.get("public_id"),
                        (String) uploadResult.get("secure_url"),
                        (String) uploadResult.get("format"),
                        i // Account index
                );
            } catch (Exception e) {
                System.err.println("Upload failed on account " + i + ": " + e.getMessage());
                // Continue to next account
            }
        }
        throw new IOException("All Cloudinary accounts failed or quotas exceeded.");
    }

    public void deleteFile(String publicId, int accountIndex) {
        if (accountIndex >= 0 && accountIndex < clients.size()) {
            try {
                clients.get(accountIndex).uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> generatePreviews(String publicId, int accountIndex, int pages) {
        List<String> previews = new ArrayList<>();
        if (accountIndex < 0 || accountIndex >= clients.size()) return previews;

        Cloudinary client = clients.get(accountIndex);

        // Generate 5 page previews
        for (int i = 1; i <= pages; i++) {
            String pageUrl = client.url()
                    .transformation(new com.cloudinary.Transformation().page(i).width(600).crop("limit"))
                    .resourceType("image")
                    .format("jpg")
                    .generate(publicId);
            previews.add(pageUrl);
        }
        return previews;
    }

    public static record UploadResult(String publicId, String url, String format, int accountIndex) {}
}