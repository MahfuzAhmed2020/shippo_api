package com.example.shippo_api.config;

import com.shippo.Shippo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "shippo.api-key", matchIfMissing = false)
public class ShippoConfig {

    private final String apiKey;

    public ShippoConfig(@Value("${shippo.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @PostConstruct
    public void configureShippo() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("SHIPPO_API_KEY is not configured.");
        }

        // Shippo 3.2.0 uses the 2018-02-08 API version.
        Shippo.setApiKey(apiKey);
        Shippo.setApiVersion("2018-02-08");
    }
}
