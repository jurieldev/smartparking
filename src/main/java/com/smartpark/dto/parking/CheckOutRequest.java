package com.smartpark.dto.parking;

import jakarta.validation.constraints.NotBlank;

public record CheckOutRequest(@NotBlank String licensePlate) {}