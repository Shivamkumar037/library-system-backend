package com.library.controller;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
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

            // refCode check for Admin
            User savedUser = service.registerUser(user, payload.get("refCode"));
            savedUser.setPassword(null); // Hide password in response
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

    // --- Books ---

    @PostMapping("/books/upload")
    public ResponseEntity<?> upload(
            @RequestParam("bookName") String bookName,
            @RequestParam("description") String description,
            @RequestParam("userEmail") String userEmail,
            @RequestParam("file") MultipartFile file) {
        try {
            Book book = service.uploadBook(bookName, description, file, userEmail);
            return ResponseEntity.ok(book);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getBooks() {
        return ResponseEntity.ok(service.getAllBooks());
    }

    // --- Admin Actions ---

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestParam String adminEmail) {
        try {
            service.deleteUser(id, adminEmail);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}