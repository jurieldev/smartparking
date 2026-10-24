package com.smartpark.auth;

public record LoginResponse(String token, String tokenType, long expiresIn) {}