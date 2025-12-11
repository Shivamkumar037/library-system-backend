package com.library.repository;

import com.library.model.Book; // FIX: Import the correct Entity, NOT java.awt.print.Book
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByOwnerIdAndDeletedFalse(Long ownerId, Pageable pageable);

    Page<Book> findByDeletedFalse(Pageable pageable);

    Page<Book> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String desc, Pageable pageable);

    List<Book> findByOwnerId(Long ownerId);
}