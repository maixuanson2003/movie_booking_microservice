package com.example.user_service.service;



import java.util.Optional;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.user_service.exception.BadRequestException;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.sharedLogic.dto.CheckPasswordRequest;
import com.example.user_service.sharedLogic.dto.LoginResult;
import com.example.user_service.sharedLogic.dto.UserDTO;
import com.example.user_service.sharedLogic.dto.request.UserRegister;
import com.example.user_service.sharedLogic.mapper.UserMapper;

@Service
@Transactional(readOnly = true)
public class UserService extends BaseService<User, Long> {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    private final RegistrationOutbox registrationOutbox;

    public UserService(UserRepository repository, UserMapper mapper, PasswordEncoder passwordEncoder,
            RegistrationOutbox registrationOutbox) {
        super(repository);
        this.repository = repository;
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
        this.registrationOutbox = registrationOutbox;
    }

    @Transactional
    public LoginResult registerUser(UserRegister userRegister) {

        Optional<User> userOptional = repository.findByUsername(userRegister.getUsername());
        if (!userOptional.isEmpty()) {
            throw new BadRequestException("Username already exists");
        }
        Optional<User> emailOptional = repository.findByEmail(userRegister.getEmail());

        if (!emailOptional.isEmpty()) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(userRegister.getUsername())
                .email(userRegister.getEmail())
                .password(passwordEncoder.encode(userRegister.getPassword()))
                .role("USER")
                .phone(userRegister.getPhone())
                .build();

        User savedUser = repository.save(user);
        registrationOutbox.enqueue(savedUser);
        return new LoginResult(mapper.toDto(savedUser), true);
    }

    public UserDTO getUserByUsername(String username) {
        validateUsername(username);
        return repository.findByUsername(username).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean checkPassword(CheckPasswordRequest request) {
        if (request == null) {
            throw new BadRequestException("Credentials are required");
        }
        return isLogin(request.username(), request.password()).isLogin();
    }

    public LoginResult isLogin(String username, String password) {
        validateUsername(username);
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password must not be blank");
        }
        Optional<User> userOptional = repository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return new LoginResult(null, false);
        }
        if (userOptional.get().getStatus() == null || !"ACTIVE".equals(userOptional.get().getStatus())) {
            return new LoginResult(null, false);
        }

        boolean isPasswordMatch = passwordEncoder.matches(password, userOptional.get().getPassword());
        if (!isPasswordMatch) {
            return new LoginResult(null, false);
        }
        return new LoginResult(mapper.toDto(userOptional.get()), true);
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username must not be blank");
        }
    }
}
