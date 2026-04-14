package com.emergencymesh.repository;

import com.emergencymesh.model.RawEmergencyRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RawEmergencyRequestRepository extends JpaRepository<RawEmergencyRequest, UUID> {
}
