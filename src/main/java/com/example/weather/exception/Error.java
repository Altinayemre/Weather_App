package com.example.weather.exception;

public record Error(
        String code,
        String type,
        String info
) { }
