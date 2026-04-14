package com.emergencymesh.service;

import com.emergencymesh.dto.LocationDto;
import com.emergencymesh.exception.ResourceNotFoundException;
import com.emergencymesh.model.Location;
import com.emergencymesh.model.User;
import com.emergencymesh.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final UserService userService;

    @Transactional
    public LocationDto.Response updateLocation(LocationDto.Request request) {
        User user = userService.getUserEntityById(request.getUserId());

        Location location = locationRepository.findTopByUserIdOrderByTimestampDesc(request.getUserId())
                .orElseGet(() -> {
                    Location newLocation = new Location();
                    newLocation.setUser(user);
                    return newLocation;
                });

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());

        Location saved = locationRepository.save(location);
        return LocationDto.Response.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public LocationDto.Response getLocation(UUID userId) {
        Location location = locationRepository.findTopByUserIdOrderByTimestampDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Location", "userId", userId));
        return LocationDto.Response.fromEntity(location);
    }
}
