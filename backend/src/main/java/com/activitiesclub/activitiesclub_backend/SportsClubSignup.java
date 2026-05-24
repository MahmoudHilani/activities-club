package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Table(name = "sports_club_signups")
public class SportsClubSignup {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(nullable = false, length = 120)
    private String name;

    @Getter
    @Setter
    @Column(nullable = false, length = 120)
    private String email;

    @Getter
    @Setter
    @Column(nullable = false, length = 30, name = "phone_number")
    private String phoneNumber;

    @Getter
    @Setter
    @Column(nullable = false, length = 30, name = "student_number")
    private String studentNumber;

    @Getter
    @Setter
    @Column(nullable = false, length = 120)
    private String course;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gender gender;

    @Getter
    @Setter
    @ElementCollection(targetClass = SportsClub.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "sports_club_signup_clubs",
        joinColumns = @JoinColumn(name = "signup_id", nullable = false)
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "sports_club", nullable = false, length = 20)
    private Set<SportsClub> sportsClubs = EnumSet.noneOf(SportsClub.class);

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Getter
    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;
}
