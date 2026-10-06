package com.example.shippo_api.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(Long id) {
        super("Shipment was not found: " + id);
    }
}
