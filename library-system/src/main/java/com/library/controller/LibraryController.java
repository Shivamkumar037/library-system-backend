package com.library.controller;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Includes CrossOrigin
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
// FIX: Ye line sabhi origins (frontend links) ko allow karti hai
@CrossOrigin(origins = "*") 
public class LibraryController {

    @Autowired
    private LibraryService service;

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

    // --- Home Page and Book Details ---

    @GetMapping("/books/recent")
    public ResponseEntity<List<Book>> getRecentBooks() {
        return ResponseEntity.ok(service.getRecentBooks(35));
    }

    @GetMapping("/books/details/{bookId}")
    public ResponseEntity<?> getBookDetails(@PathVariable Long bookId) {
        try {
            Book book = service.getBookDetails(bookId);
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // --- User Book Management ---

    @PostMapping("/books/upload")
    public ResponseEntity<?> uploadBook(
            @RequestParam("bookName") String bookName,
            @RequestParam("description") String description,
            @RequestParam("uploaderIdentifier") String uploaderIdentifier,
            @RequestParam("file") MultipartFile file) {
        try {
            Book book = service.uploadBook(bookName, description, file, uploaderIdentifier);
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/books/user/{identifier}")
    public ResponseEntity<List<Book>> getBooksByUser(@PathVariable String identifier) {
        return ResponseEntity.ok(service.getBooksByUploader(identifier));
    }

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
