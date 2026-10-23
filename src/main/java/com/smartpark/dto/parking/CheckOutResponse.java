package com.smartpark.dto.parking;


import com.smartpark.entity.parking.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CheckOutResponse(Long sessionId, String licensePlate, String lotId, Instant checkInAt,
                               Instant checkOutAt, long durationMinutes, BigDecimal costPerMinute,
                               BigDecimal totalCost, SessionStatus status) {}