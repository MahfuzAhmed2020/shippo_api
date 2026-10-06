package com.example.shippo_api.dto;

public record RateResponse(
                String rateId,
                String shippoShipmentId,
                String carrier,
                String service,
                String amount,
                String currency,
                String estimatedDays) {
}
