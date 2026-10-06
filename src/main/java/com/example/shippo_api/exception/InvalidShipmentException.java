package com.example.shippo_api.exception;

public class InvalidShipmentException extends RuntimeException {
    public InvalidShipmentException(String message) {
        super(message);
    }
}
