package com.smartpark.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String error, String message, String path) {
        return new ApiError(Instant.now(), status.value(), error, message, path, null);
    }
}