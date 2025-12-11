package com.library.service;

import com.library.dto.LibraryDtos;
import com.library.exception.GlobalExceptionHandler;
import com.library.model.Book;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// ================= BOOK SERVICE =================
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CloudinaryManager cloudinaryManager;

    @Transactional
    public LibraryDtos.BookResponse uploadBook(LibraryDtos.BookUploadRequest metadata, MultipartFile file, Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("User not found"));

        // Upload to Cloudinary with failover
        CloudinaryManager.UploadResult result = cloudinaryManager.uploadFile(file);

        // Generate Previews
        List<String> previews = cloudinaryManager.generatePreviews(result.publicId(), result.accountIndex(), 5);

        Book book = Book.builder()
                .name(metadata.getName())
                .description(metadata.getDescription())
                .ownerId(userId)
                .publicId(result.publicId())
                .accountIndex(result.accountIndex())
                .viewUrl(result.url())
                .downloadUrl(result.url()) // Cloudinary delivers raw file
                .format(result.format())
                .size(file.getSize())
                .pagesPreview(previews)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        // FIX: save() returns the entity directly, so no .orElseThrow() here
        Book saved = bookRepository.save(book);

        // Update user stats
        user.setUploadsCount(user.getUploadsCount() + 1);
        userRepository.save(user);

        return mapToResponse(saved);
    }

    public LibraryDtos.BookResponse getBook(Long id) {
        Book book = bookRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Book not found"));
        return mapToResponse(book);
    }

    @Transactional
    public void deleteBook(Long bookId, Long userId, boolean isAdmin) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Book not found"));

        if (!book.getOwnerId().equals(userId) && !isAdmin)
            throw new GlobalExceptionHandler.ForbiddenException("Not authorized");

        book.setDeleted(true); // Soft delete

        // FIX: save() returns the entity directly, so no .orElseThrow() here
        bookRepository.save(book);

        // Also remove from Cloudinary (optional, usually keep for backup or hard delete later)
        cloudinaryManager.deleteFile(book.getPublicId(), book.getAccountIndex());
    }

    public void incrementDownload(Long bookId) {
        // findById returns Optional -> We use .orElseThrow()
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException("Book not found"));

        book.setDownloads(book.getDownloads() + 1);

        // FIX: save() returns the entity directly -> We DO NOT use .orElseThrow()
        bookRepository.save(book);
    }

    public Page<LibraryDtos.BookResponse> searchBooks(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return bookRepository.findByDeletedFalse(pageable).map(this::mapToResponse);
        }
        return bookRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)
                .map(this::mapToResponse);
    }

    private LibraryDtos.BookResponse mapToResponse(Book b) {
        return LibraryDtos.BookResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .description(b.getDescription())
                .ownerId(b.getOwnerId())
                .viewUrl(b.getViewUrl())
                .downloadUrl(b.getDownloadUrl())
                .previews(b.getPagesPreview())
                .downloads(b.getDownloads())
                .sizeBytes(b.getSize())
                .createdAt(b.getCreatedAt())
                .build();
    }
}