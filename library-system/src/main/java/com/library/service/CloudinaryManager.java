package com.library.service;

import com.library.model.Book;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@Service
public class LibraryService {

    @Autowired private UserRepository userRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private CloudinaryManager cloudinaryManager;

    // --- Helper Method ---
    private User findUserByEmail(String email) throws Exception {
        return userRepository.findByEmail(email).orElseThrow(() -> new Exception("User not found"));
    }

    // --- USER ---
    public User register(User user, String secretCode) throws Exception {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new Exception("Email already exists.");
        }
        user.setRole("ADMIN_SECRET_CODE".equals(secretCode) ? User.Role.ADMIN : User.Role.STUDENT);
        return userRepository.save(user);
    }

    public User login(String email, String password) throws Exception {
        User user = findUserByEmail(email);
        if (!user.getPassword().equals(password)) throw new Exception("Wrong password");
        if (!user.isActive()) throw new Exception("Account is banned.");

        user.setLastLogin(java.time.LocalDateTime.now());
        return userRepository.save(user);
    }

    // --- BOOK ---
    @Transactional
    public Book uploadBook(String title, String desc, MultipartFile file, String email) throws Exception {
        User uploader = findUserByEmail(email);
        if (!uploader.isActive()) throw new Exception("You are banned from uploading.");

        Map<String, Object> result = cloudinaryManager.uploadFile(file);

        String publicId = (String) result.get("public_id");
        String resType = (String) result.get("resource_type");
        String format = (String) result.get("format");
        Long bytes = Long.valueOf(result.get("bytes").toString());

        String dlUrl = cloudinaryManager.generateDownloadUrl(publicId, resType);
        String thumbUrl = cloudinaryManager.generatePreviewUrl(publicId, resType);

        Book book = new Book();
        book.setBookName(title);
        book.setDescription(desc);
        book.setDownloadUrl(dlUrl);
        book.setThumbnailUrl(thumbUrl);

        book.setPublicId(publicId);
        book.setResourceType(resType);

        book.setFileFormat(format);
        book.setFileSize(bytes);
        book.setUploadedByUserId(uploader.getId());
        book.setUploadedByUserName(uploader.getName());

        return bookRepository.save(book);
    }

    public List<Book> getRecent() {
        return bookRepository.findTop50ByOrderByUploadDateDesc();
    }

    public Book getBook(Long id) throws Exception {
        return bookRepository.findById(id).orElseThrow(() -> new Exception("Book not found"));
    }

    // 🔥 FIX: Increment download count AND dynamically regenerate the correct download URL
    @Transactional
    public String trackDownload(Long bookId) throws Exception {
        Book book = getBook(bookId);
        book.setDownloadCount(book.getDownloadCount() + 1);
        bookRepository.save(book);

        // Dynamically generate the download link using the stored Public ID and Resource Type.
        // This MUST now generate a valid link structure.
        return cloudinaryManager.generateDownloadUrl(book.getPublicId(), book.getResourceType());
    }

    public void deleteBook(Long id, String email) throws Exception {
        User user = findUserByEmail(email);
        Book book = getBook(id);

        if(user.getRole() != User.Role.ADMIN && !book.getUploadedByUserId().equals(user.getId())) {
            throw new Exception("Unauthorized");
        }

        cloudinaryManager.deleteFile(book.getPublicId());
        bookRepository.delete(book);
    }

    // Admin Only
    public void deleteUser(Long userId, String adminEmail) throws Exception {
        User admin = findUserByEmail(adminEmail);
        if(admin.getRole() != User.Role.ADMIN) throw new Exception("Not Admin");
        userRepository.deleteById(userId);
    }

    public List<User> getAllUsers(String adminEmail) throws Exception {
        User admin = findUserByEmail(adminEmail);
        if(admin.getRole() != User.Role.ADMIN) throw new Exception("Not Admin");
        return userRepository.findAll();
    }
}
