package com.example.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // These endpoints are called before the caller has a login token.
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/users/check-password", "/api/users/isLogin", "/api/users/register"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/users/username/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/check-password", "/api/users/isLogin", "/api/users/register").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
