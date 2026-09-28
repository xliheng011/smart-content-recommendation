package com.example.backend.repository;

import com.example.backend.entity.Content;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ContentRepository
        extends JpaRepository<Content, Long>,
        JpaSpecificationExecutor<Content> {

    @Transactional
    @Modifying
    @Query("UPDATE Content c SET c.viewCount = c.viewCount + 1 WHERE c.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("UPDATE Content c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 取消点赞时扣减，使用 CASE 保证不会出现负数。
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE Content c
               SET c.likeCount = CASE WHEN c.likeCount > 0
                                      THEN c.likeCount - 1
                                      ELSE 0 END
             WHERE c.id = :id
            """)
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 分类聚合。
     */
    @Query("""
            SELECT c.category AS name, COUNT(c) AS total
              FROM Content c
             WHERE c.category IS NOT NULL AND c.category <> ''
             GROUP BY c.category
             ORDER BY COUNT(c) DESC
            """)
    List<CategoryCount> countGroupByCategory();

    /**
     * 同分类下的其他内容，用于"相关阅读"。
     */
    List<Content> findByCategoryAndIdNot(
            String category,
            Long id,
            Pageable pageable
    );

    List<Content> findByAuthorId(Long authorId);

    /**
     * 某作者发布的内容，最新在前，用于"我的文章"。
     */
    List<Content> findByAuthorIdOrderByIdDesc(Long authorId);

    /**
     * 分类聚合投影。
     */
    interface CategoryCount {

        String getName();

        long getTotal();
    }
}
