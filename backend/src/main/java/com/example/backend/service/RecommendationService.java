package com.example.backend.service;


import com.example.backend.dto.RecommendationResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class RecommendationService {


    private final UserBehaviorRepository userBehaviorRepository;

    private final ContentRepository contentRepository;

    private final RedisTemplate<String, Object> redisTemplate;



    public RecommendationService(
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository,
            RedisTemplate<String, Object> redisTemplate
    ) {

        this.userBehaviorRepository =
                userBehaviorRepository;

        this.contentRepository =
                contentRepository;

        this.redisTemplate =
                redisTemplate;

    }





    /**
     * 用户推荐
     *
     * Redis缓存30分钟
     *
     */
    public List<RecommendationResponse> recommend(
            Long userId
    ) {


        String cacheKey =
                "recommend:user:" + userId;



        /*
         * 1. 查询Redis
         */
        Object cache =
                redisTemplate.opsForValue()
                        .get(cacheKey);



        if (cache != null) {


            System.out.println(
                    "推荐缓存命中: "
                            + cacheKey
            );


            return (List<RecommendationResponse>) cache;

        }



        System.out.println(
                "推荐缓存未命中: "
                        + cacheKey
        );




        /*
         * 2. 查询用户行为
         */
        List<UserBehavior> behaviors =
                userBehaviorRepository
                        .findByUserId(userId);



        /*
         * 分类兴趣分数
         *
         * LIKE = 3
         * VIEW = 1
         */
        Map<String,Integer> categoryScore =
                new HashMap<>();




        for(UserBehavior behavior : behaviors) {


            Content content =
                    contentRepository
                            .findById(
                                    behavior.getContentId()
                            )
                            .orElse(null);



            if(content == null) {

                continue;

            }



            int score =
                    "LIKE".equals(
                            behavior.getBehaviorType()
                    )
                            ? 3
                            : 1;



            categoryScore.merge(
                    content.getCategory(),
                    score,
                    Integer::sum
            );


        }





        /*
         * 3. 查询全部内容
         */
        List<Content> contents =
                contentRepository.findAll();





        /*
         * 4. 计算推荐分数
         */
        List<RecommendationResponse> result =
                contents.stream()

                        .map(content -> {


                            int score =
                                    categoryScore
                                            .getOrDefault(
                                                    content.getCategory(),
                                                    0
                                            );



                            return new RecommendationResponse(

                                    content.getId(),

                                    content.getTitle(),

                                    content.getCategory(),

                                    score
                            );


                        })



                        .sorted(
                                Comparator
                                        .comparing(
                                                RecommendationResponse::getScore
                                        )
                                        .reversed()
                        )


                        .limit(10)


                        .collect(
                                Collectors.toList()
                        );







        /*
         * 5. 写入Redis
         *
         * 保存30分钟
         */
        redisTemplate.opsForValue()
                .set(
                        cacheKey,
                        result,
                        Duration.ofMinutes(30)
                );



        System.out.println(
                "推荐缓存写入: "
                        + cacheKey
        );



        return result;

    }


}