package com.emergencymesh.repository;

import com.emergencymesh.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m WHERE m.sender.id = :userId ORDER BY m.timestamp DESC")
    Page<Message> findByUserId(UUID userId, Pageable pageable);
}
