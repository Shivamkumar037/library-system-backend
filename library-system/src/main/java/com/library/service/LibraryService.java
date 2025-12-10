package com.library.service;

import com.library.model.Book;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class LibraryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CloudinaryManager cloudinaryManager; // Make sure CloudinaryManager.java file exists in the same package

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Authentication Logic ---
    public User registerUser(User user, String refCode) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists.");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Admin Logic
        if ("@shivamkumar037rdsk".equals(refCode)) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        return userRepository.save(user);
    }

    public User loginUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid Password");
        }
        return user;
    }

    // --- Book Upload Logic ---
    public Book uploadBook(String bookName, String description, MultipartFile file, String email) throws IOException {
        // Validation check for CloudinaryManager
        if (cloudinaryManager == null) {
            throw new RuntimeException("Internal Error: Cloudinary Service not initialized.");
        }

        // 1. Upload file to Cloudinary
        // Using Map raw type to catch whatever Cloudinary returns
        Map uploadResult = cloudinaryManager.uploadFileSmartly(file);

        // 2. Save details to Database
        Book book = new Book();
        book.setBookName(bookName);
        book.setDescription(description);

        // Safe retrieval from Map
        book.setFileUrl(uploadResult.get("url").toString());
        book.setPublicId(uploadResult.get("public_id").toString());

        book.setOriginalFileName(file.getOriginalFilename());
        book.setUploadedByEmail(email);

        return bookRepository.save(book);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAllByOrderByUploadedAtDesc();
    }

    // --- Admin Delete Logic ---
    public void deleteUser(Long id, String adminEmail) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!"ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("Access Denied");
        }
        userRepository.deleteById(id);
    }
}