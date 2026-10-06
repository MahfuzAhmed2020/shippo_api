package com.example.shippo_api.controller;

import com.example.shippo_api.dto.LabelRequest;
import com.example.shippo_api.dto.LabelResponse;
import com.example.shippo_api.dto.RateResponse;
import com.example.shippo_api.dto.ShipmentRequest;
import com.example.shippo_api.dto.ShipmentResponse;
import com.example.shippo_api.entity.Shipment;
import com.example.shippo_api.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@Tag(name = "Shipments", description = "Shippo rate shopping and shipping-label operations")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping("/rates")
    @Operation(summary = "Get available shipping rates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid shipment request"),
            @ApiResponse(responseCode = "502", description = "Shippo integration failure")
    })
    public ResponseEntity<List<RateResponse>> getRates(@Valid @RequestBody ShipmentRequest request) {
        return ResponseEntity.ok(shipmentService.getRates(request));
    }

    @PostMapping("/label")
    @Operation(summary = "Create a shipping label using the selected Shippo rate")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Label created and shipment saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request or rate mismatch"),
            @ApiResponse(responseCode = "502", description = "Shippo integration failure")
    })
    public ResponseEntity<LabelResponse> createLabel(@Valid @RequestBody LabelRequest request) {
        LabelResponse response = shipmentService.createLabel(request);
        return ResponseEntity.created(URI.create("/api/shipments/" + response.shipmentId())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a saved shipment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipment found"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<ShipmentResponse> getById(
            @Parameter(description = "Database shipment ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ShipmentResponse.fromEntity(shipmentService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all saved shipments")
    @ApiResponse(responseCode = "200", description = "Shipments returned successfully")
    public ResponseEntity<List<ShipmentResponse>> getAll() {
        return ResponseEntity.ok(shipmentService.getAll().stream().map(ShipmentResponse::fromEntity).toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a saved shipment")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Shipment deleted"),
            @ApiResponse(responseCode = "404", description = "Shipment not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Database shipment ID", example = "1") @PathVariable Long id) {
        shipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
