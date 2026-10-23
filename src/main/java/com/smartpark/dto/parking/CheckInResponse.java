package com.smartpark.dto.parking;

import java.time.Instant;

public record CheckInResponse(Long sessionId, String licensePlate, String lotId, Instant checkInAt) {}
