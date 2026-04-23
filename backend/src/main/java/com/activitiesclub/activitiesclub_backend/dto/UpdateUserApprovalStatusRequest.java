package com.activitiesclub.activitiesclub_backend.dto;

import com.activitiesclub.activitiesclub_backend.ApprovalStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateUserApprovalStatusRequest(@NotNull ApprovalStatus approvalStatus) {}
