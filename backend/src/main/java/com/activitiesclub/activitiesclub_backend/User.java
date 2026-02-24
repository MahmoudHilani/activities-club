package com.activitiesclub.activitiesclub_backend;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Getter 
    @Setter
    @Column(nullable = false, length = 30) private String username;

    @Getter 
    @Setter
    @Column(nullable = false, length = 120) private String email;

    @Getter 
    @Setter
    @JsonIgnore
    @Column(nullable = false, length = 255, name = "password_hash") private String passwordHash;

    @Getter 
    @Column(nullable = false, name = "created_at", updatable = false, insertable = false) private Instant createdAt;

    @Getter
    @Setter 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private Role role;

}



