package com.example.backend.service;

import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户 / 管理员登录
     *
     * USER  -> 普通用户
     * ADMIN -> 管理员
     */
    public UserResponse login(LoginRequest request) {

        /*
         * 基础参数检查
         */
        if (request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名和密码不能为空"
            );
        }

        /*
         * 如果没有传 role，默认按照普通用户登录
         */
        String loginRole = request.getRole();

        if (loginRole == null || loginRole.isBlank()) {
            loginRole = "USER";
        }

        loginRole = loginRole.toUpperCase();

        /*
         * 只允许 USER / ADMIN
         */
        if (!"USER".equals(loginRole)
                && !"ADMIN".equals(loginRole)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "无效的登录角色"
            );
        }

        /*
         * 根据用户名查询用户
         */
        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "用户名或密码错误"
                        )
                );

        /*
         * 检查密码
         */
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户名或密码错误"
            );
        }

        /*
         * 检查数据库中的真实角色
         *
         * 这里非常重要：
         *
         * 前端即使发送 ADMIN，
         * 但数据库里这个用户是 USER，
         * 也不能以管理员身份登录。
         */
        if (!loginRole.equalsIgnoreCase(user.getRole())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "当前账号没有该登录权限"
            );
        }

        /*
         * 登录成功
         */
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