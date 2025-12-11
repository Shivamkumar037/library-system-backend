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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ConcurrencyFilter concurrencyFilter;

    // ConcurrencyFilter ko inject kar rahe hain taaki use Security Chain mein daal sakein
    public SecurityConfig(ConcurrencyFilter concurrencyFilter) {
        this.concurrencyFilter = concurrencyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF Disable (APIs ke liye zaroori)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. CORS Enable
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Queue Logic Filter Add (Sabse pehle yeh chalega)
                .addFilterBefore(concurrencyFilter, UsernamePasswordAuthenticationFilter.class)

                // 4. Permissions (Filhal hum custom auth logic service mein use kar rahe hain, isliye sab allow hai)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**").permitAll()
                )

                // 5. Stateless Session (Server par load kam karne ke liye)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Frontend ko allow karein (Mobile/Web sabke liye '*')
        configuration.setAllowedOriginPatterns(List.of("*"));

        // Saare methods allow karein
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Saare headers allow karein
        configuration.setAllowedHeaders(List.of("*"));

        // Cookies/Credentials allow
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Password Hashing ke liye (Optional, agar future mein encrypted password chahiye)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}