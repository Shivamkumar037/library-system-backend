package com.library.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryManager {

    private final List<Cloudinary> cloudinaryAccounts = new ArrayList<>();

    @PostConstruct
    public void init() {
        // --- Account 1 ---
        addAccount("dqcswwkhm", "536161493549588", "OVD0uq4wBOuMEY_rierCj1sWRoY");
        // --- Account 2 ---
        addAccount("dqcswwkhm", "933184417245545", "qoAf63U5FrAWD_u-FdsLhYudLks");
        // --- Account 3 ---
        addAccount("dqcswwkhm", "964982191121326", "kPh5ZBW5K-CW9v-Mgziz5d5HHEU");
        // --- Account 4 ---
        addAccount("dqcswwkhm", "357493596555868", "iJm7cuVJ_lxDHZFg5O5MJNseF0g");
        // --- Account 5 ---
        addAccount("dqcswwkhm", "412446428444689", "6khzMLUeio0yNK71vAQ6M-AMSA0");
    }

    private void addAccount(String cloudName, String apiKey, String apiSecret) {
        Map<String, String> config = ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", "true"
        );
        cloudinaryAccounts.add(new Cloudinary(config));
    }

    public Map uploadFileSmartly(MultipartFile file) throws IOException {
        IOException lastException = null;

        // Loop chala kar check karega ki kaunsa account available hai
        for (int i = 0; i < cloudinaryAccounts.size(); i++) {
            try {
                // Upload karne ki koshish
                return cloudinaryAccounts.get(i).uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("resource_type", "auto"));
            } catch (IOException e) {
                // Agar fail hua, to error save karke agle account par jayega
                lastException = e;
                System.err.println("Cloudinary Account " + (i + 1) + " failed: " + e.getMessage());
            }
        }
        // Agar saare 5 accounts fail ho gaye tab error dega
        throw new IOException("All 5 Cloudinary accounts failed! Last error: " + (lastException != null ? lastException.getMessage() : "Unknown"));
    }
}