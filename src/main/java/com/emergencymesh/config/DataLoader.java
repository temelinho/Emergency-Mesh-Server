package com.emergencymesh.config;

import com.emergencymesh.model.User;
import com.emergencymesh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final com.emergencymesh.repository.LocationRepository locationRepository;
    private final com.emergencymesh.repository.EmergencyRequestRepository emergencyRequestRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // Mesh mesajları için fallback kullanıcı (kayıtlı kullanıcı bulunamasa bile)
            if (userRepository.findByUsername("mesh_anonim").isEmpty()) {
                User meshAnonim = new User();
                meshAnonim.setUsername("mesh_anonim");
                meshAnonim.setPhoneNumber("+0000000000");
                meshAnonim.setPassword("");
                userRepository.save(meshAnonim);
                log.info("Fallback user mesh_anonim created");
            }

            if (userRepository.findByUsername("Alice").isEmpty()) {
                User user1 = new User();
                user1.setUsername("Alice");
                user1.setFirstName("Alice");
                user1.setLastName("Smith");
                user1.setCity("Istanbul");
                user1.setDistrict("Besiktas");

                User user2 = new User();
                user2.setUsername("Bob");
                user2.setPhoneNumber("+0987654321");
                user2.setFirstName("Bob");
                user2.setLastName("Jones");
                user2.setCity("Ankara");
                user2.setDistrict("Cankaya");

                userRepository.save(user1);
                userRepository.save(user2);

                // Add sample locations
                com.emergencymesh.model.Location loc1 = new com.emergencymesh.model.Location();
                loc1.setUser(user1);
                loc1.setLatitude(41.0422);
                loc1.setLongitude(29.0064);
                
                com.emergencymesh.model.Location loc2 = new com.emergencymesh.model.Location();
                loc2.setUser(user2);
                loc2.setLatitude(39.9208);
                loc2.setLongitude(32.8541);

                locationRepository.save(loc1);
                locationRepository.save(loc2);

                // Add sample emergency requests
                com.emergencymesh.model.EmergencyRequest req1 = new com.emergencymesh.model.EmergencyRequest();
                req1.setUser(user1);
                req1.setType(com.emergencymesh.model.EmergencyRequest.EmergencyType.MEDICAL);
                req1.setDescription("Alice needs medical assistance.");
                req1.setStatus(com.emergencymesh.model.EmergencyRequest.RequestStatus.PENDING);
                req1.setLatitude(41.0422);
                req1.setLongitude(29.0064);
                req1.setPriority(com.emergencymesh.model.EmergencyRequest.EmergencyPriority.HIGH);
                
                com.emergencymesh.model.EmergencyRequest req2 = new com.emergencymesh.model.EmergencyRequest();
                req2.setUser(user2);
                req2.setType(com.emergencymesh.model.EmergencyRequest.EmergencyType.FIRE);
                req2.setDescription("Bob reported a small fire.");
                req2.setStatus(com.emergencymesh.model.EmergencyRequest.RequestStatus.PENDING);
                req2.setLatitude(39.9208);
                req2.setLongitude(32.8541);
                req2.setPriority(com.emergencymesh.model.EmergencyRequest.EmergencyPriority.EMERGENCY);

                emergencyRequestRepository.save(req1);
                emergencyRequestRepository.save(req2);

                log.info("Sample users, locations, and requests loaded: {}, {}", user1.getUsername(), user2.getUsername());
            }
        } catch (Exception e) {
            log.warn("Failed to load sample data: {}", e.getMessage());
        }
    }
}
