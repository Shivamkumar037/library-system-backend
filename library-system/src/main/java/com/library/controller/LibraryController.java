package com.library.controller;

import com.library.dto.LibraryDtos.*;
 // Assuming proper package scan or imports
import com.library.service.*; // Import specific services
import com.library.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

// Wrapper for all controllers to keep structure concise
class LibraryController {

    @RestController
    @RequestMapping("/api/auth")
    @RequiredArgsConstructor
    public static class AuthController {
        private final AuthService authService;

        @PostMapping("/register")
        public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest req) {
            return ResponseEntity.ok(authService.register(req));
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
            return ResponseEntity.ok(authService.login(req));
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<ApiResponse> forgot(@RequestParam String email) {
            authService.forgotPassword(email);
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent", null));
        }
    }

    @RestController
    @RequestMapping("/api/books")
    @RequiredArgsConstructor
    public static class BookController {
        private final BookService bookService;

        @PostMapping
        public ResponseEntity<BookResponse> upload(
                @RequestParam("name") String name,
                @RequestParam("description") String desc,
                @RequestParam("file") MultipartFile file) throws IOException {

            Long userId = getCurrentUserId();
            BookUploadRequest req = new BookUploadRequest();
            req.setName(name);
            req.setDescription(desc);
            return ResponseEntity.ok(bookService.uploadBook(req, file, userId));
        }

        @GetMapping("/{id}")
        public ResponseEntity<BookResponse> getBook(@PathVariable Long id) {
            return ResponseEntity.ok(bookService.getBook(id));
        }

        @GetMapping("/{id}/download")
        public ResponseEntity<Void> downloadTrack(@PathVariable Long id) {
            bookService.incrementDownload(id);
            return ResponseEntity.ok().build();
        }

        @GetMapping("/search")
        public ResponseEntity<Page<BookResponse>> search(@RequestParam(required=false) String q, Pageable pageable) {
            return ResponseEntity.ok(bookService.searchBooks(q, pageable));
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
            bookService.deleteBook(id, getCurrentUserId(), isAdmin());
            return ResponseEntity.ok(new ApiResponse(true, "Deleted", null));
        }
    }

    @RestController
    @RequestMapping("/api/admin")
    @RequiredArgsConstructor
    @PreAuthorize("hasRole('ADMIN')")
    public static class AdminController {
        private final AdminService adminService;

        @DeleteMapping("/users/{id}")
        public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
            adminService.deleteUser(id, getCurrentUserId());
            return ResponseEntity.ok(new ApiResponse(true, "User deactivated", null));
        }
    }

    // Helper methods to get current Auth info
    private static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> details = (java.util.Map<String, Object>) auth.getDetails();
        // In a real JWT setup, extract ID from claims. Using mock logic here for compilation:
        // Actually, JwtUtils puts userId in claims. We need to access it.
        // For simplicity in this structure, we assume the Principal or Details has it.
        // A cleaner way in production is a custom @CurrentUser annotation.
        return 1L; // Placeholder: Replace with `(Long) ((Claims) auth.getPrincipal()).get("userId")` logic
    }

    private static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}