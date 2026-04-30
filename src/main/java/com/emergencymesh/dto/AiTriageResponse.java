package com.emergencymesh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiTriageResponse {
    private String text;
    private String priority; // LOW, MEDIUM, HIGH, EMERGENCY
    private int label;
}
