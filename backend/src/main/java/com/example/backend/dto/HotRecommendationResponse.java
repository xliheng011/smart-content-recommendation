package com.example.backend.dto;


public class HotRecommendationResponse {


    private Long id;

    private String title;

    private String category;

    private Integer score;



    public HotRecommendationResponse(
            Long id,
            String title,
            String category,
            Integer score
    ){

        this.id = id;
        this.title = title;
        this.category = category;
        this.score = score;

    }



    public Long getId() {
        return id;
    }


    public String getTitle() {
        return title;
    }


    public String getCategory() {
        return category;
    }


    public Integer getScore() {
        return score;
    }

}