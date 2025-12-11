package com.library.service;

import com.library.model.Book;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
public class LibraryService {

    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private CloudinaryManager cloudinaryManager;

    // ---------------- USER REGISTER ----------------
    public User register(User user, String secretCode) throws Exception {

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new Exception("Email already exists.");
        }

        // Admin check
        if ("ADMIN_SECRET_CODE".equals(secretCode)) {
            user.setRole(User.Role.ADMIN);
        } else {
            user.setRole(User.Role.STUDENT);
        }

        return userRepository.save(user);
    }

    // ---------------- LOGIN ----------------
    public User login(String email, String password) throws Exception {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        if (!user.getPassword().equals(password))
            throw new Exception("Wrong password");

        if (!user.isActive())
            throw new Exception("Account is banned.");

        user.setLastLogin(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    // ---------------- UPLOAD BOOK ----------------
    public Book uploadBook(String title, String desc, MultipartFile file, String email) throws Exception {

        User uploader = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        if (!uploader.isActive())
            throw new Exception("You are banned from uploading.");

        // 1. Upload to Cloudinary
        Map<String, Object> result = cloudinaryManager.uploadFile(file);

        String publicId = (String) result.get("public_id");
        Long bytes = Long.valueOf(result.get("bytes").toString());
        String format = (String) result.get("format");

        // CloudinaryManager automatically detects resource_type now
        String resourceType = cloudinaryManager.detectForDB(publicId);

        // 2. Generate URLs
        String dlUrl = cloudinaryManager.generateDownloadUrl(publicId);
        String thumbUrl = cloudinaryManager.generatePreviewUrl(publicId);

        // 3. Save to DB
        Book book = new Book();
        book.setBookName(title);
        book.setDescription(desc);
        book.setDownloadUrl(dlUrl);
        book.setThumbnailUrl(thumbUrl);
        book.setPublicId(publicId);
        book.setFileFormat(format);
        book.setFileSize(bytes);
        book.setResourceType(resourceType);
        book.setUploadedByUserId(uploader.getId());
        book.setUploadedByUserName(uploader.getName());

        return bookRepository.save(book);
    }

    // ---------------- GET BOOKS ----------------
    public List<Book> getRecent() {
        return bookRepository.findTop50ByOrderByUploadDateDesc();
    }

    public Book getBook(Long id) throws Exception {
        return bookRepository.findById(id)
                .orElseThrow(() -> new Exception("Book not found"));
    }

    // ---------------- DOWNLOAD TRACKER ----------------
    public String trackDownload(Long bookId) throws Exception {

        Book book = getBook(bookId);
        book.setDownloadCount(book.getDownloadCount() + 1);
        bookRepository.save(book);

        // Always return Cloudinary download URL
        return book.getDownloadUrl();
    }

    // ---------------- DELETE BOOK ----------------
    public void deleteBook(Long id, String email) throws Exception {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User error"));

        Book book = getBook(id);

        // Only admin or uploader can delete
        if(user.getRole() != User.Role.ADMIN && !book.getUploadedByUserId().equals(user.getId())) {
            throw new Exception("Unauthorized");
        }

        // Delete from Cloudinary
        cloudinaryManager.deleteFile(book.getPublicId());

        // Delete from DB
        bookRepository.delete(book);
    }

    // ---------------- ADMIN DELETE USER ----------------
    public void deleteUser(Long userId, String adminEmail) throws Exception {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new Exception("Admin not found"));

        if(admin.getRole() != User.Role.ADMIN)
            throw new Exception("Not Admin");

        userRepository.deleteById(userId);
    }

    // ---------------- GET ALL USERS (ADMIN) ----------------
    public List<User> getAllUsers(String adminEmail) throws Exception {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new Exception("Admin not found"));

        if(admin.getRole() != User.Role.ADMIN)
            throw new Exception("Not Admin");

        return userRepository.findAll();
    }
}
