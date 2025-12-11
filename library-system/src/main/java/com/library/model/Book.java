package com.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
@EntityListeners(AuditingEntityListener.class)
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 1000)
    private String description;

    private Long ownerId; // userId of uploader

    private String publicId; // Cloudinary ID
    private int accountIndex; // Which Cloudinary account (0-4)
    private long size; // Bytes
    private String format; // pdf
    private String viewUrl;
    private String downloadUrl;

    @ElementCollection
    @CollectionTable(name = "book_pages_preview", joinColumns = @JoinColumn(name = "book_id"))
    @OrderColumn(name = "page_index") // FIX: This creates a Primary Key (book_id + page_index)
    @Column(name = "image_url", length = 1000)
    private List<String> pagesPreview;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private int downloads;
    private boolean deleted; // Soft delete
}