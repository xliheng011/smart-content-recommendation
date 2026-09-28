package com.example.backend.repository;

import com.example.backend.entity.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface UserBehaviorRepository
        extends JpaRepository<UserBehavior, Long> {

    List<UserBehavior> findByUserId(Long userId);

    /**
     * 按用户查询最近行为（倒序）。
     */
    List<UserBehavior> findTop30ByUserIdOrderByIdDesc(Long userId);

    /**
     * 供"最近互动"去重使用：多取一些原始记录，
     * 因为同一篇内容可能被反复浏览，去重后条数会明显缩水。
     */
    List<UserBehavior> findTop100ByUserIdOrderByIdDesc(Long userId);

    /**
     * 判断是否已经产生过某种行为，用于点赞去重。
     */
    boolean existsByUserIdAndContentIdAndBehaviorType(
            Long userId,
            Long contentId,
            String behaviorType
    );

    List<UserBehavior> findByUserIdAndContentIdAndBehaviorType(
            Long userId,
            Long contentId,
            String behaviorType
    );

    /**
     * 查询某用户某类行为的全部记录。
     */
    List<UserBehavior> findByUserIdAndBehaviorType(
            Long userId,
            String behaviorType
    );

    /**
     * 查询某用户某类行为的记录，按时间倒序（id 递增即时间递增）。
     * 阅读历史依赖它拿到"最近读过什么"。
     */
    List<UserBehavior> findByUserIdAndBehaviorTypeOrderByIdDesc(
            Long userId,
            String behaviorType
    );

    /**
     * 批量查询当前用户对一批内容的行为，避免列表页 N+1。
     */
    List<UserBehavior> findByUserIdAndContentIdInAndBehaviorType(
            Long userId,
            Collection<Long> contentIds,
            String behaviorType
    );

    long countByUserId(Long userId);

    long countByContentIdAndBehaviorType(
            Long contentId,
            String behaviorType
    );

    @Transactional
    @Modifying
    void deleteByUserIdAndContentIdAndBehaviorType(
            Long userId,
            Long contentId,
            String behaviorType
    );

    /**
     * 删除内容时连带清理其行为记录。
     */
    @Transactional
    @Modifying
    void deleteByContentId(Long contentId);

    /**
     * 去重统计某类行为涉及的内容数量。
     */
    @Query("""
            SELECT COUNT(DISTINCT b.contentId)
              FROM UserBehavior b
             WHERE b.userId = :userId
               AND b.behaviorType = :behaviorType
            """)
    long countDistinctContent(
            @Param("userId") Long userId,
            @Param("behaviorType") String behaviorType
    );
}
