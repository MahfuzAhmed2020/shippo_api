package com.example.shippo_api.service;

import com.example.shippo_api.dto.ShipmentRequest;
import com.example.shippo_api.exception.InvalidShipmentException;
import com.example.shippo_api.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Test
    void rejectsInvalidCountryBeforeCallingShippo() {
        ShipmentService service = new ShipmentService(shipmentRepository);
        ShipmentRequest request = new ShipmentRequest();
        request.setFromCountry("USA");
        request.setToCountry("US");

        assertThrows(InvalidShipmentException.class, () -> service.getRates(request));
        verifyNoInteractions(shipmentRepository);
    }
}
