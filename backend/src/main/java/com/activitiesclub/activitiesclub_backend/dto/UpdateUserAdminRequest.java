package com.activitiesclub.activitiesclub_backend.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserAdminRequest(@NotNull Boolean isAdmin) {}
