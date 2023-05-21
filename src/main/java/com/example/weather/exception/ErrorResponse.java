package com.example.weather.exception;

public record ErrorResponse(
        String success,
        Error error
) { }
