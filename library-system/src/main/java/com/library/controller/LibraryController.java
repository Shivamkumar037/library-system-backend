package com.library.controller;

import com.library.model.Book;
import com.library.model.User;
import com.library.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all for now
public class LibraryController {

    @Autowired private LibraryService service;

    // --- AUTH ---
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            User u = new User();
            u.setName(body.get("name"));
            u.setEmail(body.get("email"));
            u.setPassword(body.get("password"));
            u.setMobile(body.get("mobile"));
            return ResponseEntity.ok(service.register(u, body.get("refCode")));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.login(body.get("email"), body.get("password")));
        } catch (Exception e) { return ResponseEntity.status(401).body(Map.of("error", e.getMessage())); }
    }

    // --- BOOKS ---
    @GetMapping("/books/recent")
    public List<Book> getBooks() { return service.getRecent(); }

    @PostMapping("/books/upload")
    public ResponseEntity<?> upload(
            @RequestParam("bookName") String title,
            @RequestParam("description") String desc,
            @RequestParam("uploaderIdentifier") String email,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(service.uploadBook(title, desc, file, email));
        } catch (Exception e) { return ResponseEntity.status(500).body(Map.of("error", e.getMessage())); }
    }

    // New Download Endpoint - Tracks count and Redirects securely
    @GetMapping("/books/download/{id}")
    public ResponseEntity<?> download(@PathVariable Long id) {
        try {
            String safeUrl = service.trackDownload(id);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(safeUrl)).build();
        } catch (Exception e) { return ResponseEntity.status(404).body("Error: " + e.getMessage()); }
    }

    // View/Preview Endpoint
    @GetMapping("/books/view/{id}")
    public ResponseEntity<?> view(@PathVariable Long id) {
        try {
            Book b = service.getBook(id);
            // Just returning book object, frontend uses thumbnailUrl for preview
            return ResponseEntity.ok(b);
        } catch (Exception e) { return ResponseEntity.status(404).body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id, @RequestParam String deleterIdentifier) {
        try {
            service.deleteBook(id, deleterIdentifier);
            return ResponseEntity.ok(Map.of("message", "Deleted"));
        } catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    // --- ADMIN ---
    @GetMapping("/admin/users")
    public ResponseEntity<?> getUsers(@RequestParam String adminIdentifier) {
        try { return ResponseEntity.ok(service.getAllUsers(adminIdentifier)); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, @RequestParam String adminIdentifier) {
        try { service.deleteUser(id, adminIdentifier); return ResponseEntity.ok(Map.of("message", "Deleted")); }
        catch (Exception e) { return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); }
    }
}