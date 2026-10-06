package com.example.shippo_api.repository;

import com.example.shippo_api.entity.Shipment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class ShipmentRepositoryTest {

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Test
    void savesAndLoadsShipment() {
        Shipment shipment = new Shipment();
        shipment.setFromName("Sender");
        shipment.setFromStreet("1 Main St");
        shipment.setFromCity("Buffalo");
        shipment.setFromState("NY");
        shipment.setFromZip("14201");
        shipment.setFromCountry("US");
        shipment.setToName("Customer");
        shipment.setToStreet("2 Broadway");
        shipment.setToCity("New York");
        shipment.setToState("NY");
        shipment.setToZip("10007");
        shipment.setToCountry("US");
        shipment.setLength(new BigDecimal("5"));
        shipment.setWidth(new BigDecimal("5"));
        shipment.setHeight(new BigDecimal("5"));
        shipment.setWeight(new BigDecimal("2"));
        shipment.setShippingCost(new BigDecimal("9.54"));
        shipment.setCurrency("USD");
        shipment.setStatus("SUCCESS");

        Shipment saved = shipmentRepository.saveAndFlush(shipment);

        assertNotNull(saved.getId());
        Shipment loaded = shipmentRepository.findById(saved.getId()).orElseThrow();
        assertEquals("SUCCESS", loaded.getStatus());
        assertEquals(new BigDecimal("9.54"), loaded.getShippingCost());
    }
}
