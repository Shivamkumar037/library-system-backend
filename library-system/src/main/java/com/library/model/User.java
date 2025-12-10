package com.library.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_users") // FIX: Table ka naam 'app_users' kiya gaya hai
@Data
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email; // Used as username for login
    private String password;
    private String mobile;
    private Boolean isAdmin = false; // Default is false (Normal user)
}
