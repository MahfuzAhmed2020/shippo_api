package com.example.shippo_api.dto;

public record LabelResponse(
                Long shipmentId,
                String status,
                String trackingNumber,
                String carrier,
                String service,
                String amount,
                String currency,
                String labelUrl) {
}
