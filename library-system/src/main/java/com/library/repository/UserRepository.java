package com.library.repository;

import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Login aur registration ke liye user ko email se dhoondhta hai.
     * Email field ko hum username ke taur par use kar rahe hain.
     */
    User findByEmail(String email);

    // Admin user search ke liye agar ID se dhoondhna ho (Optional)
    Optional<User> findById(Long id);
}