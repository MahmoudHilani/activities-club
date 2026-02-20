package com.activitiesclub.activitiesclub_backend;

import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityRepository activityRepository;

    public ActivityController(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }
    @GetMapping
    public List<Activity> list() {
    return activityRepository.findAll();
    }

    @PostMapping
    public Activity create(@RequestBody Activity body) {
        return activityRepository.save(body);
    }
}
