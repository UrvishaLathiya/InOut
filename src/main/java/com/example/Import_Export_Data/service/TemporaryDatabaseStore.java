package com.example.Import_Export_Data.service;

import com.example.Import_Export_Data.DTO.DestinationDbConfig;
import org.springframework.stereotype.Component;

@Component
public class TemporaryDatabaseStore {

    private DestinationDbConfig config;

    public void save(DestinationDbConfig config) {
        this.config = config;
    }

    public DestinationDbConfig get() {
        return config;
    }

    public boolean isAvailable() {
        return config != null;
    }

    public void clear() {
        this.config = null;
    }
}

