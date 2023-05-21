package com.example.weather.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.weather.TestSupport;
import com.example.weather.constants.Constants;
import com.example.weather.dto.WeatherDto;
import com.example.weather.dto.WeatherResponse;
import com.example.weather.exception.ErrorResponse;
import com.example.weather.exception.WeatherStackApiException;
import com.example.weather.model.WeatherEntity;
import com.example.weather.repositroy.WeatherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WeatherServiceTest extends TestSupport {

    private WeatherRepository weatherRepository;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new ParameterNamesModule());

        restTemplate = mock(RestTemplate.class);
        weatherRepository = mock(WeatherRepository.class);
        Clock clock = mock(Clock.class);

        Constants constants = new Constants();
        constants.setWeatherStackApiBaseUrl("weather-base-api-url");
        constants.setApiKey("api-key");
        constants.setApiCallLimit(30);

        weatherService = new WeatherService(weatherRepository, restTemplate, clock);

        when(clock.instant()).thenReturn(getCurrentInstant());
        when(clock.getZone()).thenReturn(Clock.systemDefaultZone().getZone());
    }

    @Test
    void testGetWeather_whenFirstRequestForRequestedCity_shouldCallWeatherStackAPIAndSaveResponseAndReturnWeatherDto() throws Exception {
        String responseJson = getAntalyaWeatherJson();
        WeatherResponse weatherResponse = this.objectMapper.readValue(responseJson, WeatherResponse.class);
        WeatherEntity savedEntity = getSavedWeatherEntity(weatherResponse.location().localTime());

        WeatherDto expected = new WeatherDto(savedEntity.getCityName(), savedEntity.getCountry(), savedEntity.getTemperature(), savedEntity.getUpdatedTime());

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity,String.class)).thenReturn(ResponseEntity.ok(responseJson));
        when(weatherRepository.save(any(WeatherEntity.class))).thenReturn(savedEntity);

        WeatherDto actual = this.weatherService.getWeather(requestedCity);

        assertEquals(expected,actual);

        verify(restTemplate).getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class);
        verify(weatherRepository).save(any(WeatherEntity.class));
    }

    @Test
    public void testGetWeather_whenWeatherStackReturnError_shouldThrowWeatherStackApiException() throws Exception {
        String requestedCity="Antarctica";
        String responseJson = getErrorResponseJson();
        ErrorResponse response = objectMapper.readValue(responseJson, ErrorResponse.class);

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class)).thenReturn(ResponseEntity.ok(responseJson));

        assertThatThrownBy(() -> weatherService.getWeather(requestedCity))
                .isInstanceOf(WeatherStackApiException.class)
                .isEqualTo(new WeatherStackApiException(response));

        verify(restTemplate).getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class);
        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testGetWeather_whenWeatherStackReturnUnknownResponse_shouldThrowRuntimeException() {
        String responseJson = "InvalidResponse";

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.empty());
        when(restTemplate.getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class)).thenReturn(ResponseEntity.ok(responseJson));

        assertThatThrownBy(() -> weatherService.getWeather(requestedCity))
                .isInstanceOf(RuntimeException.class);

        verify(restTemplate).getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class);
        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testGetWeather_whenCityAlreadyExistsAndNotOlderThan30Minutes_shouldReturnWeatherDtoAndNotCallWeatherStackAPI() throws Exception {
        String responseJson = getAntalyaWeatherJson();
        WeatherResponse weatherResponse = this.objectMapper.readValue(responseJson, WeatherResponse.class);
        WeatherEntity savedEntity = getSavedWeatherEntity(weatherResponse.location().localTime());

        WeatherDto expected = new WeatherDto(savedEntity.getCityName(), savedEntity.getCountry(), savedEntity.getTemperature(), savedEntity.getUpdatedTime());

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.of(savedEntity));

        WeatherDto actual = this.weatherService.getWeather(requestedCity);

        assertEquals(expected,actual);

        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verifyNoInteractions(restTemplate);
        verifyNoMoreInteractions(weatherRepository);
    }

    @Test
    public void testGetWeather_whenCityAlreadyExistsAndOlderThan30Minutes_shouldCallWeatherStackAPIAndSaveWeatherAndReturnWeatherDto() throws Exception {
        String responseJson = getAntalyaWeatherJson();
        WeatherResponse weatherResponse = this.objectMapper.readValue(responseJson, WeatherResponse.class);
        WeatherEntity oldEntity = new WeatherEntity(
                "id",
                requestedCity,
                "Antalya",
                "Turkey",
                18,
                LocalDateTime.parse("2023-05-19 09:30",formatter),
                LocalDateTime.parse(weatherResponse.location().localTime(), formatter));

        WeatherEntity savedEntity = getSavedWeatherEntity(weatherResponse.location().localTime());

        WeatherDto expected = new WeatherDto(savedEntity.getCityName(), savedEntity.getCountry(), savedEntity.getTemperature(), savedEntity.getUpdatedTime());

        when(weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity)).thenReturn(Optional.of(oldEntity));
        when(restTemplate.getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class)).thenReturn(ResponseEntity.ok(responseJson));
        when(weatherRepository.save(any(WeatherEntity.class))).thenReturn(savedEntity);

        WeatherDto actual = this.weatherService.getWeather(requestedCity);

        assertEquals(expected, actual);

        verify(weatherRepository).findFirstByRequestedCityNameOrderByUpdatedTimeDesc(requestedCity);
        verify(restTemplate).getForEntity(WEATHER_STACK_API_BASE_URL+requestedCity, String.class);
        verify(weatherRepository).save(any(WeatherEntity.class));
    }

    @Test
    public void testClearCache() {
        Logger logger = (Logger) LoggerFactory.getLogger(WeatherService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        weatherService.clearCache();

        List<ILoggingEvent> logsList = listAppender.list;

        assertEquals("Caches are cleared", logsList.get(0).getMessage());
        assertEquals(Level.INFO, logsList.get(0).getLevel());
    }

}