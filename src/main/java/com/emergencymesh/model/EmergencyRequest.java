package com.emergencymesh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "emergency_requests")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyType type;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private EmergencyPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_responder_id")
    private Responder assignedResponder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum EmergencyType {
        SAFE, MEDICAL, RESCUE_DEBRIS, FIRE, SUPPLY_WATER, SHELTER, DANGER, POWER_OUTAGE, OTHER,
        FOOD, RESCUE, ACCIDENT, POLICE
    }

    public enum RequestStatus {
        PENDING, IN_PROGRESS, RESOLVED, CANCELLED
    }

    public enum EmergencyPriority {
        LOW, MEDIUM, HIGH, EMERGENCY, RESOLVED
    }
}
