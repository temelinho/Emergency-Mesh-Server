package com.emergencymesh.repository;

import com.emergencymesh.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
    Optional<Location> findTopByUserIdOrderByTimestampDesc(UUID userId);
}
