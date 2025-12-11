package com.library.controller;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.CloudinaryManager;
import com.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LibraryController {

    @Autowired
    private LibraryService service;

    @Autowired
    private CloudinaryManager cloudinaryManager;

    // --- Authentication ---

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        try {
            User user = new User();
            user.setName(payload.get("name"));
            user.setEmail(payload.get("email"));
            user.setPassword(payload.get("password"));
            user.setMobile(payload.get("mobile"));

            User savedUser = service.registerUser(user, payload.get("refCode"));
            savedUser.setPassword(null);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        try {
            User user = service.loginUser(payload.get("email"), payload.get("password"));
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Book Operations ---

    @GetMapping("/books/recent")
    public ResponseEntity<List<Book>> getRecentBooks() {
        return ResponseEntity.ok(service.getRecentBooks(35));
    }

    /**
     * API to DOWNLOAD the file.
     * Logic: Finds book -> Generates Attachment URL -> Redirects user to that URL.
     */
    @GetMapping("/books/download/{bookId}")
    public ResponseEntity<?> downloadBook(@PathVariable Long bookId) {
        try {
            Book book = service.getBookDetails(bookId);
            // Resource type "raw" or "image" based on database or file extension usually.
            // Assuming 'documents' are raw/image. CloudinaryManager handles 'auto' detection.
            // Ideally, Book model should store 'resourceType'. If not, we guess 'raw' for docs.
            String type = (book.getThumbnailUrl() != null && book.getThumbnailUrl().contains("/image/")) ? "image" : "raw";

            String downloadLink = cloudinaryManager.generateDownloadUrl(book.getPublicId(), type);

            // Redirect browser to the Cloudinary URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(downloadLink))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "Download failed: " + e.getMessage()));
        }
    }

    /**
     * API to VIEW/PREVIEW the file (Max 5 Pages).
     * Logic: Returns JSON containing direct view link AND list of first 5 pages as images.
     */
    @GetMapping("/books/view/{bookId}")
    public ResponseEntity<?> viewBook(@PathVariable Long bookId) {
        try {
            Book book = service.getBookDetails(bookId);
            String type = (book.getThumbnailUrl() != null && book.getThumbnailUrl().contains("/image/")) ? "image" : "raw";

            // 1. Full View Link (Opens PDF in browser)
            String fullViewUrl = cloudinaryManager.generateViewUrl(book.getPublicId(), type);

            // 2. Generate Preview Images for First 5 Pages
            List<String> previewPages = new ArrayList<>();
            // Only generate page previews if it's likely a PDF (indicated by image resource type in Cloudinary or extension)
            // Note: Cloudinary 'raw' files (like DOCX) cannot generate page previews easily.
            for (int i = 1; i <= 5; i++) {
                String pageUrl = cloudinaryManager.generatePageImage(book.getPublicId(), i);
                if (!pageUrl.isEmpty()) {
                    previewPages.add(pageUrl);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "bookId", book.getId(),
                    "title", book.getBookName(),
                    "fullViewUrl", fullViewUrl, // Link to open full PDF
                    "previewPages", previewPages // Array of [page1.jpg, page2.jpg...]
            ));

        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "View failed: " + e.getMessage()));
        }
    }

    @PostMapping("/books/upload")
    public ResponseEntity<?> uploadBook(
            @RequestParam("bookName") String bookName,
            @RequestParam("description") String description,
            @RequestParam("uploaderIdentifier") String uploaderIdentifier,
            @RequestParam("file") MultipartFile file) {
        try {
            // Service will call CloudinaryManager.upload which has the validation logic
            Book book = service.uploadBook(bookName, description, file, uploaderIdentifier);
            return ResponseEntity.ok(book);
        } catch (IOException e) {
            // Specific catch for validation errors (Invalid file type)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    /**
     * API to get uploads by a specific USER.
     * This allows seeing what others have uploaded.
     */
    @GetMapping("/books/user/{identifier}")
    public ResponseEntity<?> getBooksByUser(@PathVariable String identifier) {
        try {
            List<Book> books = service.getBooksByUploader(identifier);
            if (books.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No uploads found for this user", "books", new ArrayList<>()));
            }
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Modification & Delete ---

    @PutMapping("/books/{bookId}")
    public ResponseEntity<?> updateBook(
            @PathVariable Long bookId,
            @RequestParam("updaterIdentifier") String updaterIdentifier,
            @RequestBody Map<String, String> payload) {
        try {
            Book updatedBook = service.updateBookDetails(
                    bookId,
                    updaterIdentifier,
                    payload.get("bookName"),
                    payload.get("description"));
            return ResponseEntity.ok(updatedBook);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<?> deleteBook(
            @PathVariable Long bookId,
            @RequestParam("deleterIdentifier") String deleterIdentifier) {
        try {
            service.deleteBook(bookId, deleterIdentifier);
            return ResponseEntity.ok(Map.of("message", "Book deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Admin Actions ---

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers(@RequestParam String adminIdentifier) {
        return ResponseEntity.ok(service.getAllUsers(adminIdentifier));
    }

    @PutMapping("/admin/users/{userId}")
    public ResponseEntity<?> updateUserData(
            @PathVariable Long userId,
            @RequestParam String adminIdentifier,
            @RequestBody Map<String, String> payload) {
        try {
            User updatedUser = service.updateUserData(userId, adminIdentifier, payload);
            updatedUser.setPassword(null);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, @RequestParam String adminIdentifier) {
        try {
            service.deleteUser(userId, adminIdentifier);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
