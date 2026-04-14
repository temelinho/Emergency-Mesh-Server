package com.emergencymesh.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "responders")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Responder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponderStatus status;

    private Double latitude;
    private Double longitude;

    private String city;
    private String district;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum ResponderType {
        AMBULANCE, FIRE_TRUCK, POLICE_CAR, RESCUE_TEAM, OTHER
    }

    public enum ResponderStatus {
        AVAILABLE, BUSY, OFFLINE
    }
}
