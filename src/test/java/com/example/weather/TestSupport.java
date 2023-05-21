package com.example.weather;

import com.example.weather.model.WeatherEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TestSupport {

    public static final String WEATHER_API_ENDPOINT = "/v1/api/open-weather";
    public final String WEATHER_STACK_API_BASE_URL = "weather-base-api-url?access_key=api-key&query=";

    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static String requestedCity = "Antalya";

    public Instant getCurrentInstant() {
        String instantExpected = "2023-05-19T09:30:00Z";
        Clock clock = Clock.fixed(Instant.parse(instantExpected), Clock.systemDefaultZone().getZone());

        return Instant.now(clock);
    }

    public LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.ofInstant(getCurrentInstant(), Clock.systemDefaultZone().getZone());
    }

    public WeatherEntity getSavedWeatherEntity(String responseLocalTime) {
        return new WeatherEntity(
                "id",
                requestedCity,
                "Antalya",
                "Turkey",
                18,
                getCurrentLocalDateTime(),
                LocalDateTime.parse(responseLocalTime, formatter));
    }

    public String getAntalyaWeatherJson() {
       // language=Json
        return """
                {
                    "request": {
                          "type": "City",
                          "query": "Antalya, Turkey",
                          "language": "tr",
                          "unit": "m"
                    },
                    "location": {
                         "name": "Antalya",
                         "country": "Turkey",
                         "region": "Antalya",
                         "lat": "36.900",
                         "lon": "30.700",
                         "timezone_id": "Europe/Istanbul",
                         "localtime": "2023-05-19 12:00",
                         "localtime_epoch": 1671457200,
                         "utc_offset": "3.0"
                    },
                     "current": {
                         "observation_time": "12:00 PM",
                         "temperature": 18,
                         "weather_code": 113,
                         "weather_icons": [
                          "https://assets.weatherstack.com/images/wsymbols01_png_64/wsymbol_0001_sunny.png"
                          ],
                          "weather_descriptions": [
                          "Sunny"
                          ],
                          "wind_speed": 5,
                          "wind_degree": 150,
                          "wind_dir": "SSE",
                          "pressure": 1015,
                          "precip": 0,
                          "humidity": 55,
                          "cloudcover": 0,
                          "feelslike": 19,
                          "uv_index": 6,
                          "visibility": 10,
                          "is_day": "no"
                    }
                  }
                """;
    }

    public String getErrorResponseJson() {
        // language=json
        return """
                {
                    "success": false,
                    "error": {
                        "code": 105,
                        "type": "https_access_restricted",
                        "info": "Access Restricted - Your current Subscription Plan does not support HTTPS Encryption."
                    }
                }
                """;
    }
}
