package com.emergencymesh.controller;

import com.emergencymesh.dto.CityDto;
import com.emergencymesh.service.LocationSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationSuggestionController {

    private final LocationSuggestionService locationSuggestionService;

    @GetMapping("/cities")
    public ResponseEntity<List<CityDto.Info>> getCities() {
        return ResponseEntity.ok(locationSuggestionService.getAllCities());
    }

    @GetMapping("/cities/{cityId}/districts")
    public ResponseEntity<List<String>> getDistricts(@PathVariable String cityId) {
        return ResponseEntity.ok(locationSuggestionService.getDistrictsByCityId(cityId));
    }

    @GetMapping("/districts-by-name")
    public ResponseEntity<List<String>> getDistrictsByName(@RequestParam String cityName) {
        return ResponseEntity.ok(locationSuggestionService.getDistrictsByCityName(cityName));
    }
}
