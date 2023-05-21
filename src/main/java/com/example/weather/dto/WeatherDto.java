package com.example.weather.dto;

import com.example.weather.model.WeatherEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record WeatherDto(
        String cityName,
        String country,
        Integer temperature,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
        LocalDateTime updatedTime
){
    public static WeatherDto convert(WeatherEntity entity) {
        return new WeatherDto(
                entity.getCityName(),
                entity.getCountry(),
                entity.getTemperature(),
                entity.getUpdatedTime()
        );
    }
}
