package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Column(nullable = false, length = 30)
    private String username;

    @Getter
    @Setter
    @Column(nullable = false, length = 120)
    private String email;

    @Getter
    @Setter
    @Column(length = 30, name = "student_number")
    private String studentNumber;

    @Getter
    @Setter
    @Column(length = 30, name = "phone_number")
    private String phoneNumber;

    @Getter
    @Setter
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Getter
    @Setter
    @JsonIgnore
    @Column(nullable = false, length = 255, name = "password_hash")
    private String passwordHash;

    @Getter
    @CreationTimestamp
    @Column(nullable = false, name = "created_at", updatable = false)
    private Instant createdAt;

    @Getter
    @UpdateTimestamp
    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "user_type")
    private UserType userType;

    @Getter
    @Setter
    @Column(nullable = false, name = "is_admin")
    private boolean isAdmin;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @PrePersist
    @PreUpdate
    void normalize() {
        if (username != null) {
            username = username.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (studentNumber != null) {
            studentNumber = studentNumber.trim();
        }
        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();
        }
    }
}
