package com.example.shippo_api.dto;

import com.example.shippo_api.entity.Shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        String fromName,
        String fromStreet,
        String fromCity,
        String fromState,
        String fromZip,
        String fromCountry,
        String toName,
        String toStreet,
        String toCity,
        String toState,
        String toZip,
        String toCountry,
        BigDecimal length,
        BigDecimal width,
        BigDecimal height,
        BigDecimal weight,
        String carrier,
        String service,
        BigDecimal shippingCost,
        String currency,
        String trackingNumber,
        String labelUrl,
        String status,
        LocalDateTime createdAt) {
    public static ShipmentResponse fromEntity(Shipment s) {
        return new ShipmentResponse(
                s.getId(), s.getFromName(), s.getFromStreet(), s.getFromCity(), s.getFromState(), s.getFromZip(),
                s.getFromCountry(),
                s.getToName(), s.getToStreet(), s.getToCity(), s.getToState(), s.getToZip(), s.getToCountry(),
                s.getLength(), s.getWidth(), s.getHeight(), s.getWeight(), s.getCarrier(), s.getService(),
                s.getShippingCost(), s.getCurrency(), s.getTrackingNumber(), s.getLabelUrl(), s.getStatus(),
                s.getCreatedAt());
    }
}
