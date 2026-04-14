package com.emergencymesh.repository;

import com.emergencymesh.model.Responder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResponderRepository extends JpaRepository<Responder, UUID> {
    List<Responder> findByStatus(Responder.ResponderStatus status);
}
