package com.example.shippo_api.exception;

import java.time.LocalDateTime;

public record ErrorResponse(String status, String message, LocalDateTime timestamp) {
}
