package com.library.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users") // Good practice to name tables
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // Plain text stored (Consider hashing in future)

    private String mobile;

    // New Fields for better management
    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN or STUDENT

    private boolean isActive = true; // To ban users if needed

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;

    public enum Role {
        ADMIN, STUDENT
    }
}