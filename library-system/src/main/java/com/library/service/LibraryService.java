package com.library.service;

import com.library.model.Book;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LibraryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CloudinaryManager cloudinaryManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Helper Methods ---

    private User findUserByIdentifier(String identifier) {
        // Finds user by ID (if identifier is numeric) or email
        try {
            Long userId = Long.parseLong(identifier);
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return userRepository.findByEmail(identifier);
        }
    }

    private void checkAdminPermission(String adminIdentifier) throws Exception {
        User admin = findUserByIdentifier(adminIdentifier);
        if (admin == null || !admin.getIsAdmin()) {
            throw new Exception("Access Denied: Only administrators can perform this action.");
        }
    }

    // --- Authentication ---

    public User registerUser(User user, String refCode) throws Exception {
        if (userRepository.findByEmail(user.getEmail()) != null) {
            throw new Exception("Email already registered.");
        }
        // Admin RefCode logic (e.g., if refCode matches a secret code, set isAdmin=true)
        if ("ADMIN_SECRET_CODE".equals(refCode)) {
            user.setIsAdmin(true);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User loginUser(String email, String rawPassword) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("User not found.");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new Exception("Invalid password.");
        }
        return user;
    }

    // --- Book Retrieval ---

    public List<Book> getRecentBooks(int count) {
        // Last se 'count' books fetch karta hai, uploadDate ke hisaab se.
        PageRequest pageRequest = PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "uploadDate"));
        return bookRepository.findAll(pageRequest).getContent();
    }

    public Book getBookDetails(Long bookId) throws Exception {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new Exception("Book not found."));
    }

    public List<Book> getBooksByUploader(String identifier) {
        // User ko dhoondhe aur uski books return karein
        User uploader = findUserByIdentifier(identifier);
        if (uploader != null) {
            return bookRepository.findByUploadedByUserId(uploader.getId());
        }
        return List.of();
    }

    // --- Book Management (User Only) ---

    @Transactional
    public Book uploadBook(String bookName, String description, MultipartFile file, String uploaderIdentifier) throws Exception {
        User uploader = findUserByIdentifier(uploaderIdentifier);
        if (uploader == null) {
            throw new Exception("Uploader user not found.");
        }

        // 1. File Upload to Cloudinary/S3
        Map<String, String> uploadResult = cloudinaryManager.upload(file);

        // 2. Book Object Creation
        Book book = new Book();
        book.setBookName(bookName);
        book.setDescription(description);
        book.setDownloadUrl(uploadResult.get("url"));
        book.setFilePublicId(uploadResult.get("public_id"));
        book.setThumbnailUrl(uploadResult.get("thumbnail_url")); // Assuming CloudinaryManager generates thumbnail
        book.setUploadedByUserId(uploader.getId());
        book.setUploadedByUserName(uploader.getEmail()); // Ya phir uploader.getName()

        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBookDetails(Long bookId, String updaterIdentifier, String newName, String newDescription) throws Exception {
        Book book = getBookDetails(bookId);
        User updater = findUserByIdentifier(updaterIdentifier);

        // Permission Check: Sirf uploader hi edit kar sakta hai
        if (updater == null || !book.getUploadedByUserId().equals(updater.getId())) {
            throw new Exception("Permission Denied: You are not the owner of this book.");
        }

        if (newName != null) book.setBookName(newName);
        if (newDescription != null) book.setDescription(newDescription);

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long bookId, String deleterIdentifier) throws Exception {
        Book book = getBookDetails(bookId);
        User deleter = findUserByIdentifier(deleterIdentifier);

        // Permission Check: Sirf uploader ya Admin hi delete kar sakta hai
        if (deleter == null || (!book.getUploadedByUserId().equals(deleter.getId()) && !deleter.getIsAdmin())) {
            throw new Exception("Permission Denied: You are not authorized to delete this book.");
        }

        // Cloudinary se file delete karein
        cloudinaryManager.delete(book.getFilePublicId());

        bookRepository.delete(book);
    }

    // --- Admin Actions ---

    public List<User> getAllUsers(String adminIdentifier) {
        try {
            checkAdminPermission(adminIdentifier);
            return userRepository.findAll();
        } catch (Exception e) {
            // Permission check fail hone par khali list ya exception throw kar sakte hain
            return List.of();
        }
    }

    @Transactional
    public User updateUserData(Long userId, String adminIdentifier, Map<String, String> payload) throws Exception {
        checkAdminPermission(adminIdentifier);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found."));

        // Admin keval in fields ko update kar sakta hai
        if (payload.containsKey("name")) user.setName(payload.get("name"));
        if (payload.containsKey("email")) user.setEmail(payload.get("email"));
        if (payload.containsKey("mobile")) user.setMobile(payload.get("mobile"));
        if (payload.containsKey("isAdmin")) user.setIsAdmin(Boolean.parseBoolean(payload.get("isAdmin")));
        if (payload.containsKey("password")) user.setPassword(passwordEncoder.encode(payload.get("password")));

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, String adminIdentifier) throws Exception {
        checkAdminPermission(adminIdentifier);

        if (!userRepository.existsById(userId)) {
            throw new Exception("User not found.");
        }
        // Note: Real world scenario mein, is user ki saari books bhi delete karni hongi.

        userRepository.deleteById(userId);
    }
}