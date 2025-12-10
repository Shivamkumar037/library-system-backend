package com.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookName;
    private String description;

    // Cloudinary ya S3 link
    private String downloadUrl;

    // Cloudinary public ID for deletion and thumbnail
    private String filePublicId;

    // Uploader's user ID (for permission checks)
    private Long uploadedByUserId;

    // Uploader ka naam/email (Home page display ke liye)
    private String uploadedByUserName;

    // First page/Thumbnail image URL
    private String thumbnailUrl;

    // Date of upload for 'recent books' query
    private java.time.LocalDateTime uploadDate = java.time.LocalDateTime.now();

}