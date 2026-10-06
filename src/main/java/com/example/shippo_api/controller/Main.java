
package com.example.shippo_api.controller;

import com.shippo.Shippo;
import com.shippo.exception.ShippoException;
import com.shippo.model.Address;
import com.shippo.model.Parcel;
import com.shippo.model.Rate;
import com.shippo.model.Shipment;
import com.shippo.model.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a simple Java application that demonstrates how to use the Shippo API
 * to create shipping labels.
 * USING MAIN METHOD FOR DEMONSTRATION PURPOSES ONLY.
 * RUN WITH JAVA ONLY. DO NOT USE IN SPRING BOOT APPLICATIONS.
 * ONLY THIS CLASS.
 * 
 * 
 */
public class Main {

    // YOUR API KEY
    private static final String API_KEY = "";

    public static void main(String[] args) {
        // Initialize the client
        Shippo.setApiKey(API_KEY);
        // Optional: Set to true for detailed debugging output
        Shippo.setDEBUG(false);

        try {
            System.out.println("--- 1. Creating Addresses ---");

            // Create the 'From' Address
            Map<String, Object> fromAddressMap = new HashMap<>();
            fromAddressMap.put("name", "MAHFUZ AHMED");
            fromAddressMap.put("company", "Shippo");
            fromAddressMap.put("street1", "115 Clayton St.");
            fromAddressMap.put("city", "San Francisco");
            fromAddressMap.put("state", "CA");
            fromAddressMap.put("zip", "94117");
            fromAddressMap.put("country", "US");
            fromAddressMap.put("phone", "+1 555 341 9393");
            fromAddressMap.put("email", "test@goshippo.com");

            Address addressFrom = Address.create(fromAddressMap);
            System.out.println("From Address ID: " + addressFrom.getObjectId());

            // Create the 'To' Address
            Map<String, Object> toAddressMap = new HashMap<>();
            toAddressMap.put("name", "BUFFALO BILL");
            toAddressMap.put("street1", "Broadway 9");
            toAddressMap.put("city", "New York");
            toAddressMap.put("state", "NY");
            toAddressMap.put("zip", "10007");
            toAddressMap.put("country", "US");
            toAddressMap.put("phone", "+1 555 341 9393");
            toAddressMap.put("email", "mrhippo@shippo.com");

            Address addressTo = Address.create(toAddressMap);
            System.out.println("To Address ID: " + addressTo.getObjectId());

            System.out.println("\n--- 2. Creating Parcel ---");

            // Create the Parcel (a simple 5x5x5 box weighing 2 lbs)
            Map<String, Object> parcelMap = new HashMap<>();
            parcelMap.put("length", "5");
            parcelMap.put("width", "5");
            parcelMap.put("height", "5");
            parcelMap.put("distance_unit", "in");
            parcelMap.put("weight", "2");
            parcelMap.put("mass_unit", "lb");

            Parcel parcel = Parcel.create(parcelMap);
            System.out.println("Parcel ID: " + parcel.getObjectId());

            System.out.println("\n--- 3. Creating Shipment & Getting Rates ---");

            // Create the Shipment object
            Map<String, Object> shipmentMap = new HashMap<>();
            shipmentMap.put("address_from", addressFrom.getObjectId());
            shipmentMap.put("address_to", addressTo.getObjectId());
            shipmentMap.put("parcels", Arrays.asList(parcel.getObjectId()));
            shipmentMap.put("async", false);

            Shipment shipment = Shipment.create(shipmentMap);
            System.out.println("Shipment ID: " + shipment.getObjectId());

            // Retrieve the list of available rates
            List<Rate> rates = shipment.getRates();
            if (rates == null || rates.isEmpty()) {
                System.out.println("No rates returned. Check the addresses and parcel details.");
                return;
            }

            System.out.println("Available Rates:");
            for (Rate rate : rates) {
                System.out.printf("  - %s : %s %s (ID: %s)%n",
                        rate.getProvider(),
                        rate.getAmount(),
                        rate.getCurrency(),
                        rate.getObjectId());
            }

            // Select the first available rate
            Rate selectedRate = rates.get(0);
            System.out.println("\nSelected Rate: " + selectedRate.getProvider() + " - " + selectedRate.getAmount());

            System.out.println("\n--- 4. Purchasing Shipping Label ---");

            // Create the Transaction (The Label)
            Map<String, Object> transactionMap = new HashMap<>();
            transactionMap.put("rate", selectedRate.getObjectId());
            transactionMap.put("label_file_type", "PDF"); // Get it as a PDF
            transactionMap.put("async", false);

            Transaction transaction = Transaction.create(transactionMap);

            // Check the status
            if ("SUCCESS".equals(transaction.getStatus())) {
                System.out.println("\n✅ Label Created Successfully!");
                System.out.println("Tracking Number: " + transaction.getTrackingNumber());
                System.out.println("Label URL: " + transaction.getLabelUrl());
                System.out.println("\nYou can open the Label URL in your browser to print/download it.");
            } else {
                System.out.println("\n❌ Label creation failed.");
                System.out.println("Status: " + transaction.getStatus());
                // In a real app, you would inspect transaction.getMessages() for details
            }

        } catch (ShippoException e) {
            System.err.println("An error occurred while communicating with Shippo:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred:");
            e.printStackTrace();
        }
    }
}