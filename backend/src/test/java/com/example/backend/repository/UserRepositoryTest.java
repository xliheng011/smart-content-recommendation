package com.example.backend.repository;

import com.example.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindByUsername() {
        // 创建测试用户
        User user = new User();

        user.setUsername("test_user");
        user.setPassword("test_password");
        user.setEmail("test@example.com");
        user.setNickname("测试用户");
        user.setAvatarUrl(null);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 保存到数据库
        User savedUser = userRepository.save(user);

        // 验证数据库自动生成了 ID
        assertNotNull(savedUser.getId());

        // 根据用户名查询
        Optional<User> result =
                userRepository.findByUsername("test_user");

        // 验证查询结果
        assertTrue(result.isPresent());
        assertEquals("test_user", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
    }
}