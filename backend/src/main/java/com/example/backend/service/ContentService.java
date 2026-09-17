package com.example.backend.service;

import com.example.backend.dto.ContentResponse;
import com.example.backend.dto.CreateContentRequest;
import com.example.backend.entity.Content;
import com.example.backend.repository.ContentRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ContentService {


    private final ContentRepository contentRepository;

    private final UserBehaviorService userBehaviorService;

    private final RedisTemplate<String, Object> redisTemplate;



    public ContentService(
            ContentRepository contentRepository,
            UserBehaviorService userBehaviorService,
            RedisTemplate<String, Object> redisTemplate
    ) {

        this.contentRepository = contentRepository;
        this.userBehaviorService = userBehaviorService;
        this.redisTemplate = redisTemplate;

    }


    /**
     * 创建内容
     */
    public ContentResponse createContent(
            CreateContentRequest request
    ) {


        Content content = new Content();


        content.setTitle(request.getTitle());

        content.setContent(request.getContent());

        content.setCategory(request.getCategory());

        content.setAuthorId(request.getAuthorId());


        content.setViewCount(0);

        content.setLikeCount(0);



        LocalDateTime now = LocalDateTime.now();

        content.setCreatedAt(now);

        content.setUpdatedAt(now);


        Content saved =
                contentRepository.save(content);



        return toResponse(saved);

    }


    @Transactional
    public ContentResponse getContentById(
            Long id,
            Long userId
    ) {



        String cacheKey =
                "content:" + id;



        Object cached =
                redisTemplate
                        .opsForValue()
                        .get(cacheKey);



        if (cached instanceof ContentResponse response) {


            System.out.println(
                    "Redis 缓存命中: "
                            + cacheKey
            );



            if (userId != null) {

                userBehaviorService.recordBehavior(
                        userId,
                        id,
                        "VIEW"
                );

            }


            return response;

        }



        System.out.println(
                "Redis 缓存未命中: "
                        + cacheKey
        );



        contentRepository.findById(id)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "内容不存在"
                                )
                );



        contentRepository.incrementViewCount(id);


        if (userId != null) {


            userBehaviorService.recordBehavior(
                    userId,
                    id,
                    "VIEW"
            );

        }



        // =====================
        // 获取最新数据
        // =====================


        Content updatedContent =
                contentRepository.findById(id)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "内容不存在"
                                        )
                        );



        ContentResponse response =
                toResponse(updatedContent);



        redisTemplate
                .opsForValue()
                .set(
                        cacheKey,
                        response
                );



        return response;

    }


    public List<ContentResponse> getAllContents() {


        return contentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();


    }


    @Transactional
    public ContentResponse likeContent(
            Long id,
            Long userId
    ) {


        contentRepository.incrementLikeCount(id);


        if (userId != null) {


            userBehaviorService.recordBehavior(
                    userId,
                    id,
                    "LIKE"
            );

        }

        if(userId != null){

            redisTemplate.delete(
                    "recommend:user:" + userId
            );

            System.out.println(
                    "删除推荐缓存: recommend:user:" + userId
            );
        }


        String cacheKey =
                "content:" + id;


        redisTemplate.delete(cacheKey);

        Content updatedContent =
                contentRepository.findById(id)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "内容不存在"
                                        )
                        );


        return toResponse(updatedContent);

    }

    /**
     * Entity -> DTO
     */
    private ContentResponse toResponse(
            Content content
    ) {


        return new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getContent(),
                content.getCategory(),
                content.getAuthorId(),
                content.getViewCount(),
                content.getLikeCount()
        );
    }
}