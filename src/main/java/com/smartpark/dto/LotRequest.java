package com.smartpark.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LotRequest(
        @NotBlank @Size(max = 50) String lotId,
        @NotBlank @Size(max = 255) String location,
        @NotNull @Positive Integer capacity,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal costPerMinute) {
}
