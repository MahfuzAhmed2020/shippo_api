package com.example.shippo_api.exception;

public class ShippoIntegrationException extends RuntimeException {
    public ShippoIntegrationException(String message) {
        super(message);
    }

    public ShippoIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
