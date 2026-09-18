package com.example.backend.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;



@Configuration
public class CorsConfig {


    @Bean
    public CorsFilter corsFilter(){


        CorsConfiguration config =
                new CorsConfiguration();


        //允许前端地址
        config.addAllowedOrigin(
                "http://localhost:5174"
        );


        //允许所有请求头
        config.addAllowedHeader("*");


        //允许 GET POST
        config.addAllowedMethod("*");


        //允许携带Cookie
        config.setAllowCredentials(true);



        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                config
        );


        return new CorsFilter(source);

    }

}