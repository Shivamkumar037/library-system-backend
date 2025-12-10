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
            // 1. CSRF disable karein (APIs ke liye zaroori)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. CORS ko explicit activate karein
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Permissions set karein
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll() // Abhi ke liye sab allow hai (Testing)
            )
            
            // 4. Stateless Session (JWT style)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // FIX: Sabhi origins (*) ko allow karein
        // setAllowedOriginPatterns use karein taaki Credentials (Cookies) bhi allow ho sakein
        configuration.setAllowedOriginPatterns(List.of("*")); 
        
        // Methods: GET, POST, PUT, DELETE, OPTIONS sab allow karein
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Headers: Authorization, Content-Type, etc.
        configuration.setAllowedHeaders(List.of("*"));
        
        // Credentials allow karein
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
