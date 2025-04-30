package com.example.Import_Export_Data.controller;

import com.example.Import_Export_Data.DTO.DestinationDbConfig;
import com.example.Import_Export_Data.service.DataTransferService;
import com.example.Import_Export_Data.service.DynamicDataTransferService;
import com.example.Import_Export_Data.service.TemporaryDatabaseStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/transfer")
public class DataTransferController {

    @Autowired
    private DataTransferService dataTransferService;

    @Autowired
    private DynamicDataTransferService dynamicDataTransferService;

    @Autowired
    private TemporaryDatabaseStore temporaryDatabaseStore;


//    @PostMapping("/{id}")
//    public ResponseEntity<String> transferRecords(@PathVariable Integer id){
//        dataTransferService.transferById(id);
//        return ResponseEntity.ok("Data Transferes successfully!");
//    }

    // ✅ NEW: Dynamic Transfer using runtime DB config
    @PostMapping("/dynamic-transfer/{id}")
    public ResponseEntity<String> transferToDynamicDb(@PathVariable Integer id) {
        if (!temporaryDatabaseStore.isAvailable()) {
            return ResponseEntity.badRequest().body("No dynamic DB config found. Please configure it in Settings first.");
        }

        try {
            DestinationDbConfig config = temporaryDatabaseStore.get();
            dynamicDataTransferService.transferToDynamicDestination(id, config);
            return ResponseEntity.ok("Data transferred to dynamic DB successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Transfer failed: " + e.getMessage());
        }
    }
}
