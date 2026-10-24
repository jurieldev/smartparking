package com.smartpark.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingFeeCalculatorTest {

    private final ParkingFeeCalculator calc = new ParkingFeeCalculator(15);
    private final Instant t0 = Instant.parse("2026-10-24T08:00:00Z"); // 8:00

    @Test
    void roundsPartialMinuteUp() {
        assertEquals(2, calc.billedMinutes(t0, t0.plusSeconds(61)));        // 1m01s → 2
    }

    @Test
    void chargesAtLeastOneMinute() {
        assertEquals(1, calc.billedMinutes(t0, t0.plusSeconds(5)));         // 5s → 1
    }

    @Test
    void exactMinutesStayTheSame() {
        assertEquals(10, calc.billedMinutes(t0, t0.plusSeconds(600)));      // 10m → 10
    }

    @Test
    void billingStopsAtLeaveTimeWhenUnderLimit() {
        Instant leftAt = t0.plusSeconds(7 * 60);                            // left at 08:07
        assertEquals(leftAt, calc.billingEnd(t0, leftAt));
    }

    @Test
    void billingStopsAtLimitWhenOver() {
        Instant leftAt = t0.plusSeconds(40 * 60);                           // left at 08:40
        assertEquals(t0.plusSeconds(15 * 60), calc.billingEnd(t0, leftAt)); // billed until 08:15
    }

    @Test
    void costIsMinutesTimesRate() {
        assertEquals(new BigDecimal("20.00"), calc.cost(new BigDecimal("2.50"), 8));
    }
}