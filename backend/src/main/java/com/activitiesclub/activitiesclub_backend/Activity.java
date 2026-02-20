package com.activitiesclub.activitiesclub_backend;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activities")
public class Activity {

    public Activity() {

    }

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Getter
    @Setter
    @Column(nullable = false, length = 120) private String title;
}
