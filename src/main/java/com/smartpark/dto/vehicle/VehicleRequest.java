package com.smartpark.dto.vehicle;

import com.smartpark.entity.vehicle.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VehicleRequest(
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "only letters, numbers and dashes are allowed")
        String licensePlate,

        @NotNull VehicleType type,

        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z ]+$", message = "only letters and spaces are allowed")
        String ownerName) {}
