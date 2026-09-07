package com.example.user_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_service.service.UserService;
import com.example.user_service.sharedLogic.dto.CheckPasswordRequest;
import com.example.user_service.sharedLogic.dto.UserDTO;

/** Login support for auth-service; responses are wrapped by ApiResponseAdvice. */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/username/{username}")
    public UserDTO getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    @PostMapping("/check-password")
    public boolean checkPassword(@RequestBody CheckPasswordRequest request) {
        return userService.checkPassword(request);
    }
}
