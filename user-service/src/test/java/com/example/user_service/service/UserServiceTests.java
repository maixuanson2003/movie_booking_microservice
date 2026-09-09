package com.example.user_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.user_service.exception.BadRequestException;
import com.example.user_service.exception.ResourceNotFoundException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.sharedLogic.dto.CheckPasswordRequest;
import com.example.user_service.sharedLogic.mapper.UserMapper;

class UserServiceTests {
    @Test
    void registrationSavesActiveUserWithEncodedPassword() {
        var request = new com.example.user_service.sharedLogic.dto.request.UserRegister(
                "alice", "alice@example.com", "correct-password", null);
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertEquals("ACTIVE", saved.getStatus());
            assertEquals("USER", saved.getRole());
            assertTrue(encoder.matches("correct-password", saved.getPassword()));
            saved.setId(42L);
            return saved;
        });
        var result = service.registerUser(request);
        assertTrue(result.isLogin());
        assertEquals(42L, result.userDto().getId());
        assertNull(result.userDto().getPassword());
    }

    private final UserRepository repository = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserService service = new UserService(repository, new UserMapper(), encoder);

    private User account() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole("USER");
        user.setPassword(encoder.encode("correct-password"));
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void loginReturnsUserOnlyForValidActiveAccount() {
        User user = account();
        var result = service.isLogin("alice", "correct-password");
        assertTrue(result.isLogin());
        assertEquals("alice", result.userDto().getUsername());
        assertNull(result.userDto().getPassword());
        assertFalse(service.isLogin("alice", "wrong").isLogin());
        assertNull(service.isLogin("alice", "wrong").userDto());
        assertFalse(service.isLogin("missing", "correct-password").isLogin());
        user.setStatus("INACTIVE");
        assertNull(service.isLogin("alice", "correct-password").userDto());
        assertFalse(service.isLogin("alice", "correct-password").isLogin());
    }

    @Test
    void lookupNeverReturnsPasswordHash() {
        User user = account();
        var dto = service.getUserByUsername("alice");
        assertEquals(1L, dto.getId());
        assertEquals("USER", dto.getRole());
        assertNull(dto.getPassword());
        assertNotNull(user.getPassword());
    }

    @Test
    void verifiesPasswordOnlyForNamedActiveAccount() {
        User user = account();
        assertTrue(service.checkPassword(new CheckPasswordRequest("alice", "correct-password")));
        assertFalse(service.checkPassword(new CheckPasswordRequest("alice", "wrong")));
        when(repository.findByUsername("bob")).thenReturn(Optional.empty());
        assertFalse(service.checkPassword(new CheckPasswordRequest("bob", "correct-password")));
        user.setStatus("INACTIVE");
        assertFalse(service.checkPassword(new CheckPasswordRequest("alice", "correct-password")));
    }

    @Test
    void rejectsInvalidInputAndReportsMissingLookup() {
        assertThrows(BadRequestException.class, () -> service.getUserByUsername(" "));
        assertThrows(BadRequestException.class, () -> service.checkPassword(null));
        assertThrows(BadRequestException.class,
                () -> service.checkPassword(new CheckPasswordRequest("alice", "")));
        verifyNoInteractions(repository);
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getUserByUsername("missing"));
    }
}
