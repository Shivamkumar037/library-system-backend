package com.library.repository;

import com.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    // New method to find books uploaded by a specific user ID
    List<Book> findByUploadedByUserId(Long uploadedByUserId);
}