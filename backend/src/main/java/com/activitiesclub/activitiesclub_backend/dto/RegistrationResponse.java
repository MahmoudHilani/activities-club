package com.activitiesclub.activitiesclub_backend.dto;

import com.activitiesclub.activitiesclub_backend.ApprovalStatus;

public record RegistrationResponse(ApprovalStatus approvalStatus, String message) {}
