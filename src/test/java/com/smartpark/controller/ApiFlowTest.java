package com.smartpark.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "parking.auto-checkout-interval-ms=3600000")   // keep the scheduler quiet
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired MockMvc mvc;

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        token = login("admin", "admin123");
    }

    // ---------- security ----------

    @Test
    @DisplayName("No token → 401 JSON")
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/api/lots"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Tampered token → 401 JSON")
    void badTokenReturns401() throws Exception {
        mvc.perform(get("/api/lots").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Wrong password → 401 INVALID_CREDENTIALS")
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Correct login → 200 with a Bearer token")
    void loginReturnsToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // ---------- validation ----------

    @Test
    @DisplayName("Plate with spaces/symbols → 400 VALIDATION_FAILED")
    void invalidPlateReturns400() throws Exception {
        postJson("/api/vehicles", """
                {"licensePlate":"AB 12!","type":"CAR","ownerName":"Ana Cruz"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.licensePlate").exists());
    }

    @Test
    @DisplayName("Owner name with digits → 400 VALIDATION_FAILED")
    void invalidOwnerNameReturns400() throws Exception {
        postJson("/api/vehicles", """
                {"licensePlate":"%s","type":"CAR","ownerName":"Ana 123"}""".formatted(newPlate()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.ownerName").exists());
    }

    @Test
    @DisplayName("Unknown vehicle type → 400 MALFORMED_REQUEST")
    void unknownVehicleTypeReturns400() throws Exception {
        postJson("/api/vehicles", """
                {"licensePlate":"%s","type":"BUS","ownerName":"Ana Cruz"}""".formatted(newPlate()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Lot with zero capacity → 400 VALIDATION_FAILED")
    void zeroCapacityReturns400() throws Exception {
        postJson("/api/lots", """
                {"lotId":"%s","location":"Test","capacity":0,"costPerMinute":1.00}""".formatted(newLotId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.capacity").exists());
    }

    // ---------- not found / conflicts ----------

    @Test
    @DisplayName("Occupancy of unknown lot → 404")
    void unknownLotReturns404() throws Exception {
        mvc.perform(get("/api/lots/{id}/occupancy", "NO-SUCH-LOT").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Registering the same lot twice → 409 LOT_EXISTS")
    void duplicateLotReturns409() throws Exception {
        String lotId = createLot(5);

        postJson("/api/lots", """
                {"lotId":"%s","location":"Again","capacity":5,"costPerMinute":1.00}""".formatted(lotId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LOT_EXISTS"));
    }

    @Test
    @DisplayName("Check-in of unregistered vehicle → 404")
    void unregisteredVehicleCheckInReturns404() throws Exception {
        String lotId = createLot(5);

        checkIn(newPlate(), lotId)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Check-in to a full lot → 409 LOT_FULL")
    void fullLotReturns409() throws Exception {
        String lotId = createLot(1);
        checkIn(createVehicle(), lotId).andExpect(status().isCreated());   // takes the only space

        checkIn(createVehicle(), lotId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LOT_FULL"));
    }

    @Test
    @DisplayName("Same vehicle into a second lot → 409 ALREADY_PARKED")
    void alreadyParkedReturns409() throws Exception {
        String plate = createVehicle();
        checkIn(plate, createLot(5)).andExpect(status().isCreated());

        checkIn(plate, createLot(5))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_PARKED"));
    }

    @Test
    @DisplayName("Check-out of a vehicle that isn't parked → 409 NOT_PARKED")
    void checkOutNotParkedReturns409() throws Exception {
        postJson("/api/parking/check-out", """
                {"licensePlate":"%s"}""".formatted(createVehicle()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("NOT_PARKED"));
    }

    // ---------- happy path ----------

    @Test
    @DisplayName("Full flow: register, check in, occupancy, list, check out")
    void fullParkingFlow() throws Exception {
        String lotId = createLot(3);
        String plate = createVehicle();

        checkIn(plate, lotId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(plate));

        mvc.perform(get("/api/lots/{id}/occupancy", lotId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(3))
                .andExpect(jsonPath("$.occupiedSpaces").value(1))
                .andExpect(jsonPath("$.availableSpaces").value(2));

        mvc.perform(get("/api/lots/{id}/vehicles", lotId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].licensePlate").value(plate));

        postJson("/api/parking/check-out", """
                {"licensePlate":"%s"}""".formatted(plate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.durationMinutes").value(1))      // under a minute → billed 1
                .andExpect(jsonPath("$.totalCost").exists());

        mvc.perform(get("/api/lots/{id}/occupancy", lotId).header("Authorization", token))
                .andExpect(jsonPath("$.occupiedSpaces").value(0));      // space freed
    }

    // ---------- helpers ----------

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** POSTs JSON with the logged-in token. */
    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions checkIn(String plate, String lotId) throws Exception {
        return postJson("/api/parking/check-in", """
                {"licensePlate":"%s","lotId":"%s"}""".formatted(plate, lotId));
    }

    private String createLot(int capacity) throws Exception {
        String lotId = newLotId();
        postJson("/api/lots", """
                {"lotId":"%s","location":"API test","capacity":%d,"costPerMinute":2.00}"""
                .formatted(lotId, capacity))
                .andExpect(status().isCreated());
        return lotId;
    }

    private String createVehicle() throws Exception {
        String plate = newPlate();
        postJson("/api/vehicles", """
                {"licensePlate":"%s","type":"CAR","ownerName":"Api Tester"}""".formatted(plate))
                .andExpect(status().isCreated());
        return plate;
    }

    // Unique IDs, because all test classes share one in-memory database
    private String newLotId() { return "API-" + UUID.randomUUID().toString().substring(0, 8); }
    private String newPlate() { return "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }
}