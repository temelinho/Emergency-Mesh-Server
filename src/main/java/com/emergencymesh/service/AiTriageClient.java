package com.emergencymesh.service;

import com.emergencymesh.dto.AiTriageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiTriageClient {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${app.ai-service.url}")
    private String AI_SERVICE_URL;

    public AiTriageResponse predictPriority(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("text", text);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(AI_SERVICE_URL, request, AiTriageResponse.class);
        } catch (Exception e) {
            System.err.println("[AI-CLIENT] Fallback mode: " + e.getMessage());
            return null; // Fallback to rule-based
        }
    }
}
