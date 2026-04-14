package com.emergencymesh.dto;

import lombok.Data;

@Data
public class AiTriageResponse {
    private String text;
    private String priority; // LOW, MEDIUM, HIGH, EMERGENCY
    private int label;
}
