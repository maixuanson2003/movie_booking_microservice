package com.example.user_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;

import com.example.user_service.model.User;
import com.example.user_service.sharedLogic.dto.UserDTO;

@Component
public class UserMapper extends BaseMapper<User, UserDTO> {

    @Override
    public User toEntity(UserDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        user.setCreatedAt(dto.getCreatedAt());
        user.setUpdatedAt(dto.getUpdatedAt());
        return user;
    }

    @Override
    public UserDTO toDto(User entity) {
        if (entity == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFullName(entity.getFullName());
        dto.setRole(entity.getRole());
        dto.setPhone(entity.getPhone());
        dto.setAvatar(entity.getAvatar());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
