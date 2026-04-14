package com.emergencymesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EmergencyMeshServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmergencyMeshServerApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.boot.CommandLineRunner startAiService() {
		return args -> {
			String os = System.getProperty("os.name").toLowerCase();
			String command = os.contains("win") ? "cmd /c start /b python ai-service/main.py --port 8030" : "python3 ai-service/main.py --port 8030 &";
			
			System.out.println("[AI-STARTUP] Starting AI Triage Service...");
			try {
				Runtime.getRuntime().exec(command);
				System.out.println("[AI-STARTUP] AI Triage Service triggered successfully.");
			} catch (Exception e) {
				System.err.println("[AI-STARTUP] Failed to start AI Service: " + e.getMessage());
			}
		};
	}

}
