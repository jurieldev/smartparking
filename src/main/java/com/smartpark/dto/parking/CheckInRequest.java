package com.smartpark.dto.parking;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(@NotBlank String licensePlate, @NotBlank String lotId) {}
