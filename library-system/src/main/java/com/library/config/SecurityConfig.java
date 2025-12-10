package com.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF disable karna zaroori hai REST APIs ke liye
                .csrf(AbstractHttpConfigurer::disable)
                // 2. CORS configuration apply karein
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints (Login/Register/Home Page)
                        .requestMatchers("/api/auth/**", "/api/books/recent", "/api/books/details/**").permitAll()

                        // Admin Endpoints: Sirf admin role allowed
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN") // Assuming role is managed in User model

                        // Baki saare endpoints ke liye authentication zaroori hai
                        .anyRequest().authenticated()
                )

                // 4. Session Management: REST API ke liye stateless session
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Note: Real application mein JWT filter add karna hoga.
        return http.build();
    }

    // CORS configuration for allowing frontend access
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Frontend ka URL yahan add karein (ya sab allow karne ke liye "*")
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Password Hashing ke liye Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}