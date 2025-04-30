package com.example.Import_Export_Data.controller;

import com.example.Import_Export_Data.DTO.DestinationDbConfig;
import com.example.Import_Export_Data.service.DatabaseCreationService;
import com.example.Import_Export_Data.service.TemporaryDatabaseStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
public class DatabaseConfigController {

    @Autowired
    private DatabaseCreationService databaseCreationService;

    @Autowired
    private TemporaryDatabaseStore temporaryDatabaseStore;

    @PostMapping("/create-db")
    public ResponseEntity<String> createDatabase(@RequestBody DestinationDbConfig config) {
        try {
            boolean created = databaseCreationService.createDatabase(config);

            // ✅ Always save the config — even if DB already exists
            temporaryDatabaseStore.save(config);

            if (created) {
                return ResponseEntity.ok("✅ New database created and configuration saved successfully.");
            } else {
                return ResponseEntity.ok("✅ Database configuration details saved successfully with existing DB name.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Error: " + e.getMessage());
        }
    }



    @GetMapping("/is-configured")
    public ResponseEntity<Boolean> isDatabaseConfigured() {
        return ResponseEntity.ok(temporaryDatabaseStore.isAvailable());
    }

    @PostMapping("/reset-db")
    public ResponseEntity<String> resetDbConfig() {
        temporaryDatabaseStore.clear();
        return ResponseEntity.ok("Database configuration has been reset.");
    }

}
