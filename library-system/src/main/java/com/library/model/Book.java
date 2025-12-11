package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bookName;

    @Column(length = 1000)
    private String description;

    // URLs
    private String downloadUrl;    // Direct download
    private String thumbnailUrl;   // Preview image

    // Technical Details
    private String publicId;       // Cloudinary ID
    private String resourceType;   // 'image' or 'raw'
    private String fileFormat;     // pdf, doc, etc.
    private Long fileSize;         // In bytes

    // Ownership & Stats
    private Long uploadedByUserId;
    private String uploadedByUserName;

    private int downloadCount = 0; // To track popularity

    private LocalDateTime uploadDate = LocalDateTime.now();
}