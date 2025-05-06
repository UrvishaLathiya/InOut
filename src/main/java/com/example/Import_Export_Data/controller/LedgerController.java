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
    public ResponseEntity<?> updateLedger(@RequestBody Map<String, Object> request) {
        System.out.println("Received update request: " + request);
        try {
            if (request.get("id") == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing id"));
            }
            boolean success = ledgerService.updateLedger(request);
            if (success) {
                return ResponseEntity.ok().body(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Failed to update ledger"));
            }
        } catch (Exception e) {
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
