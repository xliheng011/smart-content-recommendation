package com.example.backend.service;


import com.example.backend.dto.RecommendationResponse;
import com.example.backend.entity.Content;
import com.example.backend.entity.UserBehavior;
import com.example.backend.repository.ContentRepository;
import com.example.backend.repository.UserBehaviorRepository;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;



@Service
public class RecommendationService {


    private final UserBehaviorRepository userBehaviorRepository;

    private final ContentRepository contentRepository;



    public RecommendationService(
            UserBehaviorRepository userBehaviorRepository,
            ContentRepository contentRepository
    ){

        this.userBehaviorRepository =
                userBehaviorRepository;

        this.contentRepository =
                contentRepository;

    }





    public List<RecommendationResponse> recommend(
            Long userId
    ){


        // 查询用户行为

        List<UserBehavior> behaviors =
                userBehaviorRepository
                        .findByUserId(userId);



        Map<String,Integer> categoryScore =
                new HashMap<>();



        for(UserBehavior behavior: behaviors){


            Content content =
                    contentRepository
                            .findById(
                                    behavior.getContentId()
                            )
                            .orElse(null);



            if(content==null){
                continue;
            }



            int score =
                    behavior.getBehaviorType()
                            .equals("LIKE")
                            ?3
                            :1;



            categoryScore.merge(
                    content.getCategory(),
                    score,
                    Integer::sum
            );

        }






        // 查询所有内容

        List<Content> contents =
                contentRepository.findAll();




        return contents.stream()

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
                        Comparator.comparing(
                                        RecommendationResponse::getScore
                                )
                                .reversed()
                )

                .limit(10)

                .collect(Collectors.toList());


    }


}