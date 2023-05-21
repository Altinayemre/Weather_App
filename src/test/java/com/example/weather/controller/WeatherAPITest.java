package com.example.weather.controller;

import com.example.weather.dto.WeatherDto;
import com.example.weather.exception.GeneralExceptionAdvice;
import com.example.weather.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static com.example.weather.TestSupport.formatter;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "server.port=0")
@DirtiesContext
class WeatherAPITest {

    @MockBean
    private WeatherService weatherService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetWeather_whenCityParameterValid_shouldReturnWeatherDto() throws Exception {
        LocalDateTime localDateTime = LocalDateTime.parse("2023-05-20 23:55", formatter);
        WeatherDto expected = new WeatherDto("Antalya","Turkey",18,localDateTime);
        mockMvc = MockMvcBuilders.standaloneSetup(new WeatherAPI(this.weatherService))
                .setControllerAdvice(GeneralExceptionAdvice.class)
                .build();

        when(weatherService.getWeather("Antalya")).thenReturn(expected);

        mockMvc.perform(get("/v1/api/weather/Antalya").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityName", is("Antalya")))
                .andExpect(jsonPath("$.country", is("Turkey")))
                .andExpect(jsonPath("$.temperature", is(18)))
                .andExpect(jsonPath("$.updatedTime", is("2023-05-20 23:55")));
    }

    @Test
    public void testGetWeather_whenCityParameterIsNotValid_shouldReturnHTTP400BadRequest() throws Exception {
        mockMvc.perform(get("/v1/api/weather/123").contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

    }
}