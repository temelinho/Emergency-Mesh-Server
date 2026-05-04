package com.emergencymesh.repository;

import com.emergencymesh.model.EmergencyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, UUID> {
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT e FROM EmergencyRequest e JOIN FETCH e.user",
            countQuery = "SELECT count(e) FROM EmergencyRequest e")
    Page<EmergencyRequest> findAllWithUser(Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT e FROM EmergencyRequest e JOIN FETCH e.user WHERE e.status = :status",
            countQuery = "SELECT count(e) FROM EmergencyRequest e WHERE e.status = :status")
    Page<EmergencyRequest> findByStatusWithUser(EmergencyRequest.RequestStatus status, Pageable pageable);

    Page<EmergencyRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT e FROM EmergencyRequest e JOIN FETCH e.user WHERE e.assignedResponder.id = :responderId",
            countQuery = "SELECT count(e) FROM EmergencyRequest e WHERE e.assignedResponder.id = :responderId")
    Page<EmergencyRequest> findByAssignedResponderIdOrderByCreatedAtDesc(UUID responderId, Pageable pageable);
}
