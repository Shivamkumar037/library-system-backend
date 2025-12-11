package com.library.service;

import com.library.config.JwtUtils;
import com.library.dto.LibraryDtos.*;
import com.library.exception.GlobalExceptionHandler.*;
import com.library.model.*;
import com.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

// ================= AUTH SERVICE =================
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetRepository resetRepository;
    
    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    public ApiResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) throw new BadRequestException("Username taken");
        if (userRepository.existsByEmail(req.getEmail())) throw new BadRequestException("Email taken");

        User.Role role = User.Role.USER;
        // Secret Key Admin Detection
        if (adminSecretKey.equals(req.getSecretKey())) {
            role = User.Role.ADMIN;
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .mobile(req.getMobile())
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        return new ApiResponse(true, "User registered successfully", null);
    }

    public LoginResponse login(LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(req.getUsername()).orElseThrow();
        
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtils.generateToken(new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), List.of()), user.getId(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
    
    public void forgotPassword(String email) {
        // Mock sending email
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        resetRepository.save(token);
        System.out.println("OTP for " + email + ": " + otp); 
    }
}

