package com.example.backend.service;

import com.example.backend.common.ApiException;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.AuthUser;
import com.example.backend.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /* ============================
       注册
    ============================ */

    @Transactional
    public LoginResponse register(RegisterRequest request) {

        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("该用户名已被注册");
        }

        String email = normalizeEmail(request.getEmail());

        if (email != null && userRepository.existsByEmail(email)) {
            throw ApiException.conflict("该邮箱已被注册");
        }

        User user = new User();

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(email);
        user.setNickname(resolveNickname(request.getNickname(), username));
        user.setRole("USER");

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User saved = userRepository.save(user);

        return buildLoginResponse(saved);
    }

    /* ============================
       登录
    ============================ */

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String username = request.getUsername().trim();

        String loginRole = request.getRole();

        if (loginRole == null || loginRole.isBlank()) {
            loginRole = "USER";
        }

        loginRole = loginRole.trim().toUpperCase();

        if (!"USER".equals(loginRole) && !"ADMIN".equals(loginRole)) {
            throw ApiException.badRequest("无效的登录角色");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        ApiException.unauthorized("用户名或密码错误")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw ApiException.unauthorized("用户名或密码错误");
        }

        /*
         * 关键安全点：即使前端声明以 ADMIN 登录，
         * 只要数据库里该账号是 USER，就拒绝。
         */
        if (!loginRole.equalsIgnoreCase(user.getRole())) {
            throw ApiException.forbidden("当前账号没有该登录权限");
        }

        return buildLoginResponse(user);
    }

    /* ============================
       注销
    ============================ */

    public void logout(String token) {
        tokenService.revoke(token);
    }

    /* ============================
       当前用户
    ============================ */

    @Transactional(readOnly = true)
    public UserResponse currentUser(AuthUser authUser) {

        if (authUser == null) {
            throw ApiException.unauthorized("尚未登录");
        }

        User user = userRepository.findById(authUser.userId())
                .orElseThrow(() -> ApiException.unauthorized("登录状态已失效"));

        return toResponse(user);
    }

    /* ============================
       工具
    ============================ */

    private LoginResponse buildLoginResponse(User user) {

        AuthUser authUser = new AuthUser(
                user.getId(),
                user.getUsername(),
                user.getRole()
        );

        String token = tokenService.issue(authUser);

        return new LoginResponse(
                token,
                TokenService.TOKEN_TTL.toSeconds(),
                toResponse(user)
        );
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

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

    private String resolveNickname(String nickname, String username) {

        if (nickname == null || nickname.isBlank()) {
            return username;
        }

        return nickname.trim();
    }
}
