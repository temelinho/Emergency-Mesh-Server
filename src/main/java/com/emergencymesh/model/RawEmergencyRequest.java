package com.emergencymesh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "raw_emergency_requests")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawEmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // No constraints to avoid failing silently
    private String userId;

    private String type;

    @Column(length = 2000)
    private String description;

    private Double latitude;
    private Double longitude;

    private String priority;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
