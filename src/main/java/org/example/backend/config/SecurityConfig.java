package org.example.backend.config;

import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .requestMatchers("/api/video-call/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/trip/**").permitAll()
                .requestMatchers("/api/blog/**").permitAll()
                .requestMatchers("/api/comment/**").permitAll()
                .requestMatchers("/api/messages/**").permitAll()
                .requestMatchers("/api/chatroom/**").permitAll()
                .requestMatchers("/api/payment/**").permitAll()
                .requestMatchers("/api/wallet/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .cors().configurationSource(corsConfigurationSource());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3001")); // Cho phép FE từ localhost:3000
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // Cho phép FE từ localhost:3000

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // Nếu cần gửi cookie hoặc header Authorization

     
     

        // Thêm headers mới
        configuration.addAllowedHeader("Origin");
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedHeader("Accept");
        configuration.addAllowedHeader("Authorization");
        configuration.addAllowedHeader("Access-Control-Allow-Origin");
        configuration.addAllowedHeader("Access-Control-Request-Method");
        configuration.addAllowedHeader("Access-Control-Request-Headers");
        configuration.addAllowedHeader("Access-Control-Allow-Credentials");

        // Thêm exposed headers
        configuration.addExposedHeader("Access-Control-Allow-Origin");
        configuration.addExposedHeader("Access-Control-Allow-Credentials");

        // Thêm max age
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }
        return authentication.getName();
    }
}
