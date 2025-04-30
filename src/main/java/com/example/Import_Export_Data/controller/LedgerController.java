package com.example.Import_Export_Data.controller;

import com.example.Import_Export_Data.DTO.FullLedgerInfoDTO;
import com.example.Import_Export_Data.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping()
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/ledgers")
    public String getAllLedgers(Model model){
        List<FullLedgerInfoDTO> ledgers = ledgerService.getAllLedger();
        List<String> apVersions = ledgerService.getDistinctApVersions();

        model.addAttribute("ledgers", ledgers);
        model.addAttribute("apVersions", apVersions);

        return "ledgerData";
    }

    @PostMapping("/api/ledger/update")
    @ResponseBody
    public ResponseEntity<?> updateLedgerField(@RequestBody Map<String, String> request) {
        System.out.println("Received update request: " + request);
        try {
            if (request.get("id") == null || request.get("column") == null || request.get("value") == null) {
                System.out.println("Missing required fields in request");
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing required fields"));
            }

            Integer id = Integer.parseInt(request.get("id"));
            String column = request.get("column");
            String value = request.get("value");

            System.out.println("Processing update - ID: " + id + ", Column: " + column + ", Value: " + value);
            boolean success = ledgerService.updateLedgerField(id, column, value);
            
            if (success) {
                System.out.println("Update successful");
                return ResponseEntity.ok().body(Map.of("success", true));
            } else {
                System.out.println("Update failed - invalid column or value");
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to update ledger - invalid column or value"));
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid ID format"));
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/ledger/create")
    @ResponseBody
    public ResponseEntity<?> createLedger(@RequestBody Map<String, Object> request) {
        try {
            FullLedgerInfoDTO newLedger = ledgerService.createLedger(request);
            return ResponseEntity.ok().body(Map.of("success", true, "message", "Ledger created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
