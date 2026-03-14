package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Table(
    name = "activity_reservations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "user_id"})
)
public class ActivityReservation {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Getter
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "activity_id", nullable = false) private Activity activity;

    @Getter
    @Setter
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.RESERVED;

    @Getter
    @Setter
    @Column(name = "reserved_at", nullable = false) private Instant reservedAt = Instant.now();

    @Getter
    @Setter
    @Column(name = "cancelled_at") private Instant cancelledAt;

    @Getter
    @Setter
    @Column(columnDefinition = "text") private String notes;

    @Getter
    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false) private Instant createdAt;

    @Getter
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at") private Instant updatedAt;
}
