package com.example.shippo_api.dto;

import jakarta.validation.constraints.NotBlank;

public class LabelRequest extends ShipmentRequest {

    @NotBlank
    private String shippoShipmentId;

    @NotBlank
    private String rateId;

    public String getShippoShipmentId() {
        return shippoShipmentId;
    }

    public void setShippoShipmentId(String shippoShipmentId) {
        this.shippoShipmentId = shippoShipmentId;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
    }
}
