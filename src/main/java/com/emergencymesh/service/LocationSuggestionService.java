package com.emergencymesh.service;

import com.emergencymesh.dto.CityDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationSuggestionService {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private List<CityDto.CityWithDistricts> cityData = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource("classpath:data/cities.json");
            try (InputStream inputStream = resource.getInputStream()) {
                cityData = objectMapper.readValue(inputStream, new TypeReference<List<CityDto.CityWithDistricts>>() {});
                log.info("Loaded {} cities from cities.json", cityData.size());
            }
        } catch (IOException e) {
            log.error("Failed to load cities.json", e);
        }
    }

    public List<CityDto.Info> getAllCities() {
        return cityData.stream()
                .map(city -> new CityDto.Info(city.getId(), city.getName()))
                .collect(Collectors.toList());
    }

    public List<String> getDistrictsByCityId(String cityId) {
        return cityData.stream()
                .filter(city -> city.getId().equals(cityId))
                .findFirst()
                .map(CityDto.CityWithDistricts::getDistricts)
                .orElse(new ArrayList<>());
    }

    public List<String> getDistrictsByCityName(String cityName) {
        return cityData.stream()
                .filter(city -> city.getName().equalsIgnoreCase(cityName))
                .findFirst()
                .map(CityDto.CityWithDistricts::getDistricts)
                .orElse(new ArrayList<>());
    }
}
