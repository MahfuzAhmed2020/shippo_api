package com.example.shippo_api.service;

import com.example.shippo_api.dto.LabelRequest;
import com.example.shippo_api.dto.LabelResponse;
import com.example.shippo_api.dto.RateResponse;
import com.example.shippo_api.dto.ShipmentRequest;
import com.example.shippo_api.entity.Shipment;
import com.example.shippo_api.exception.InvalidShipmentException;
import com.example.shippo_api.exception.ShipmentNotFoundException;
import com.example.shippo_api.exception.ShippoIntegrationException;
import com.example.shippo_api.repository.ShipmentRepository;
import com.shippo.exception.ShippoException;
import com.shippo.model.Address;
import com.shippo.model.Parcel;
import com.shippo.model.Rate;
import com.shippo.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShipmentService {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipmentRepository;

    public ShipmentService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public List<RateResponse> getRates(ShipmentRequest request) {
        validateRequest(request);

        try {
            Address fromAddress = Address.create(buildAddressMap(
                    request.getFromName(), request.getFromCompany(), request.getFromStreet(), request.getFromCity(),
                    request.getFromState(), request.getFromZip(), request.getFromCountry(), request.getFromPhone(), request.getFromEmail()));

            Address toAddress = Address.create(buildAddressMap(
                    request.getToName(), request.getToCompany(), request.getToStreet(), request.getToCity(),
                    request.getToState(), request.getToZip(), request.getToCountry(), request.getToPhone(), request.getToEmail()));

            Parcel parcel = Parcel.create(buildParcelMap(request));

            Map<String, Object> shipmentMap = new HashMap<>();
            shipmentMap.put("address_from", fromAddress.getObjectId());
            shipmentMap.put("address_to", toAddress.getObjectId());
            shipmentMap.put("parcels", List.of(parcel.getObjectId()));
            shipmentMap.put("async", false);

            com.shippo.model.Shipment shippoShipment = com.shippo.model.Shipment.create(shipmentMap);
            List<Rate> rates = shippoShipment.getRates();

            if (rates == null || rates.isEmpty()) {
                throw new ShippoIntegrationException("Shippo returned no shipping rates for this shipment.");
            }

            return rates.stream()
                    .filter(rate -> rate.getObjectId() != null)
                    .map(rate -> toRateResponse(rate, shippoShipment.getObjectId()))
                    .toList();
        } catch (ShippoException | RuntimeException ex) {
            if (ex instanceof ShippoIntegrationException integrationException) {
                throw integrationException;
            }
            throw new ShippoIntegrationException("Unable to retrieve shipping rates from Shippo.", ex);
        }
    }

    @Transactional
    public LabelResponse createLabel(LabelRequest request) {
        validateRequest(request);
        if (request.getRateId() == null || request.getRateId().isBlank()) {
            throw new InvalidShipmentException("A Shippo rate ID is required.");
        }
        if (request.getShippoShipmentId() == null || request.getShippoShipmentId().isBlank()) {
            throw new InvalidShipmentException("A Shippo shipment ID is required. Get rates first.");
        }

        Rate selectedRate;
        Transaction transaction;
        try {
            // The rate ID is generated for the Shippo shipment returned by /rates.
            // We intentionally do not recreate the shipment here.
            selectedRate = Rate.retrieve(request.getRateId());
            if (selectedRate == null || selectedRate.getObjectId() == null) {
                throw new InvalidShipmentException("The selected Shippo rate could not be found.");
            }

            String rateShipmentId = extractObjectId(selectedRate.getShipment());
            if (rateShipmentId != null && !request.getShippoShipmentId().equals(rateShipmentId)) {
                throw new InvalidShipmentException("The selected rate does not belong to the requested Shippo shipment.");
            }

            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("rate", request.getRateId());
            transactionMap.put("async", false);
            transactionMap.put("label_file_type", "PDF");

            transaction = Transaction.createSync(transactionMap);
        } catch (ShippoException | RuntimeException ex) {
            if (ex instanceof InvalidShipmentException invalidShipmentException) {
                throw invalidShipmentException;
            }
            if (ex instanceof ShippoIntegrationException integrationException) {
                throw integrationException;
            }
            logger.error("Shippo API call failed while creating a shipping label.", ex);
            throw new ShippoIntegrationException("Unable to create a shipping label with Shippo.", ex);
        }

        String status = transaction.getStatus();
        if (!"SUCCESS".equalsIgnoreCase(status)) {
            String messages = describeTransactionMessages(transaction.getMessages());
            logger.error("Shippo label transaction {} did not succeed. Status: {}. Messages: {}",
                    transaction.getObjectId(), status, messages.isBlank() ? "(none returned)" : messages);
            String detail = messages.isBlank() ? "" : " Details: " + messages;
            throw new ShippoIntegrationException(
                    "Shippo could not generate the label. Status: " + status + "." + detail);
        }

        String carrier = extractString(selectedRate.getProvider());
        String service = extractServiceName(selectedRate.getServicelevel());
        String amount = extractString(selectedRate.getAmount());
        String currency = extractString(selectedRate.getCurrency());
        String trackingNumber = extractString(transaction.getTrackingNumber());
        String labelUrl = extractString(transaction.getLabelUrl());

        Shipment saved = new Shipment();
        copyRequestToEntity(request, saved);
        saved.setCarrier(carrier);
        saved.setService(service);
        saved.setShippingCost(toBigDecimal(amount));
        saved.setCurrency(currency);
        saved.setTrackingNumber(trackingNumber);
        saved.setLabelUrl(labelUrl);
        saved.setStatus(status);
        saved.setShippoShipmentId(request.getShippoShipmentId());
        saved.setShippoRateId(request.getRateId());
        saved.setShippoTransactionId(transaction.getObjectId());

        Shipment persisted = shipmentRepository.save(saved);

        return new LabelResponse(
                persisted.getId(), status, trackingNumber, carrier, service,
                amount, currency, labelUrl
        );
    }

    public Shipment getById(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ShipmentNotFoundException(id));
    }

    public List<Shipment> getAll() {
        return shipmentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void delete(Long id) {
        if (!shipmentRepository.existsById(id)) {
            throw new ShipmentNotFoundException(id);
        }
        shipmentRepository.deleteById(id);
    }

    private void validateRequest(ShipmentRequest request) {
        if (request == null) {
            throw new InvalidShipmentException("Shipment request is required.");
        }
        if (request.getFromCountry() != null && request.getFromCountry().length() != 2) {
            throw new InvalidShipmentException("FROM country must be a 2-letter ISO country code such as US.");
        }
        if (request.getToCountry() != null && request.getToCountry().length() != 2) {
            throw new InvalidShipmentException("TO country must be a 2-letter ISO country code such as US.");
        }
    }

    private Map<String, Object> buildAddressMap(String name, String company, String street, String city,
                                                  String state, String zip, String country, String phone, String email) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        if (company != null && !company.isBlank()) map.put("company", company);
        map.put("street1", street);
        map.put("city", city);
        map.put("state", state);
        map.put("zip", zip);
        map.put("country", country.toUpperCase());
        if (phone != null && !phone.isBlank()) map.put("phone", phone);
        if (email != null && !email.isBlank()) map.put("email", email);
        return map;
    }

    private Map<String, Object> buildParcelMap(ShipmentRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("length", request.getLength().toPlainString());
        map.put("width", request.getWidth().toPlainString());
        map.put("height", request.getHeight().toPlainString());
        map.put("distance_unit", "in");
        map.put("weight", request.getWeight().toPlainString());
        map.put("mass_unit", "lb");
        return map;
    }

    private RateResponse toRateResponse(Rate rate, String shippoShipmentId) {
        return new RateResponse(
                rate.getObjectId(),
                shippoShipmentId,
                extractString(rate.getProvider()),
                extractServiceName(rate.getServicelevel()),
                extractString(rate.getAmount()),
                extractString(rate.getCurrency()),
                extractString(rate.getDays())
        );
    }

    private String extractObjectId(Object value) {
        if (value instanceof Map<?, ?> map) {
            Object objectId = map.get("object_id");
            if (objectId == null) objectId = map.get("objectId");
            return objectId == null ? null : objectId.toString();
        }
        return extractString(value);
    }

    private String describeTransactionMessages(Object messages) {
        if (messages instanceof Iterable<?> iterable) {
            List<String> details = new ArrayList<>();
            for (Object message : iterable) {
                if (message instanceof Map<?, ?> map) {
                    Object text = map.get("text");
                    Object code = map.get("code");
                    Object source = map.get("source");
                    StringBuilder detail = new StringBuilder(
                            text == null ? map.toString() : text.toString());
                    if (code != null) detail.append(" (code: ").append(code).append(')');
                    if (source != null) detail.append(" (source: ").append(source).append(')');
                    details.add(detail.toString());
                } else if (message != null) {
                    details.add(message.toString());
                }
            }
            return String.join("; ", details);
        }
        return messages == null ? "" : messages.toString();
    }

    private String extractServiceName(Object servicelevel) {
        if (servicelevel instanceof Map<?, ?> map) {
            Object name = map.get("name");
            if (name != null) return name.toString();
        }
        return extractString(servicelevel);
    }

    private String extractString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal toBigDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private void copyRequestToEntity(ShipmentRequest request, Shipment entity) {
        entity.setFromName(request.getFromName());
        entity.setFromCompany(request.getFromCompany());
        entity.setFromStreet(request.getFromStreet());
        entity.setFromCity(request.getFromCity());
        entity.setFromState(request.getFromState());
        entity.setFromZip(request.getFromZip());
        entity.setFromCountry(request.getFromCountry());
        entity.setFromPhone(request.getFromPhone());
        entity.setFromEmail(request.getFromEmail());
        entity.setToName(request.getToName());
        entity.setToCompany(request.getToCompany());
        entity.setToStreet(request.getToStreet());
        entity.setToCity(request.getToCity());
        entity.setToState(request.getToState());
        entity.setToZip(request.getToZip());
        entity.setToCountry(request.getToCountry());
        entity.setToPhone(request.getToPhone());
        entity.setToEmail(request.getToEmail());
        entity.setLength(request.getLength());
        entity.setWidth(request.getWidth());
        entity.setHeight(request.getHeight());
        entity.setWeight(request.getWeight());
    }
}
