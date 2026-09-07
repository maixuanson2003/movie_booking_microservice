package com.example.user_service.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.exception.BadRequestException;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.sharedLogic.dto.CheckPasswordRequest;
import com.example.user_service.sharedLogic.dto.UserDTO;
import com.example.user_service.sharedLogic.mapper.UserMapper;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, UserMapper mapper, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO getUserByUsername(String username) {
        validateUsername(username);
        return repository.findByUsername(username).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean checkPassword(CheckPasswordRequest request) {
        if (request == null) throw new BadRequestException("Credentials are required");
        validateUsername(request.username());
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password must not be blank");
        }
        // A password only authenticates the account named in this request.
        return repository.findByUsername(request.username())
                .filter(user -> "ACTIVE".equals(user.getStatus()))
                .filter(user -> user.getPassword() != null)
                .map(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .orElse(false);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username must not be blank");
        }
    }
}
