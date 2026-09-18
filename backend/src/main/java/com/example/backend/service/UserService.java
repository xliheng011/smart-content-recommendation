package com.example.backend.service;

import com.example.backend.dto.CreateUserRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建普通用户
     *
     * 注册出来的账号默认角色为 USER
     */
    public UserResponse createUser(CreateUserRequest request) {

        User user = new User();

        // 用户名
        user.setUsername(request.getUsername());

        // 密码使用 BCrypt 加密
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // 邮箱
        user.setEmail(request.getEmail());

        // 昵称
        user.setNickname(request.getNickname());

        // 头像
        user.setAvatarUrl(request.getAvatarUrl());

        // 新注册用户默认是普通用户
        user.setRole("USER");

        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 保存到 MySQL
        User savedUser = userRepository.save(user);

        // 转换成 API 返回对象
        return toResponse(savedUser);
    }

    /**
     * 根据 ID 获取用户
     */
    public UserResponse getUserById(Long id) {

        User user = userRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("用户不存在")
                );

        return toResponse(user);
    }

    /**
     * 获取所有用户
     */
    public List<UserResponse> getAllUsers() {

        return userRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole()
        );
    }
}