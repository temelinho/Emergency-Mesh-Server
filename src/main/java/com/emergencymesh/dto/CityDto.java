package com.emergencymesh.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CityDto {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Info {
        private String id;
        private String name;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CityWithDistricts {
        private String id;
        private String name;
        private List<String> districts;
    }
}
