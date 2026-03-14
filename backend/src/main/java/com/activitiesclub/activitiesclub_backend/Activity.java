package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Table(name = "activities")
public class Activity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Getter
    @Setter
    @Column(nullable = false, length = 120) private String title;

    @Getter
    @Setter
    @Column(columnDefinition = "text") private String description;

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "organizer_id") private User organizer;

    @Getter
    @Setter
    @Column(name = "start_at") private Instant startAt;

    @Getter
    @Setter
    @Column(name = "end_at") private Instant endAt;

    @Getter
    @Setter
    @Column(name = "location_name", length = 160) private String locationName;

    @Getter
    @Setter
    @Column(name = "location_address", length = 255) private String locationAddress;

    @Getter
    @Setter
    private Integer capacity;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.DRAFT;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityVisibility visibility = ActivityVisibility.PUBLIC;

    @Getter
    @Setter
    @Column(name = "reservation_opens_at") private Instant reservationOpensAt;

    @Getter
    @Setter
    @Column(name = "reservation_closes_at") private Instant reservationClosesAt;

    @Getter
    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false) private Instant createdAt;

    @Getter
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at") private Instant updatedAt;
}
