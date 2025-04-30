package com.example.Import_Export_Data.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model){
        return "index";
    }

    @GetMapping("/transfer-page")
    public String loadTransferDataFragment() {
        return "transferData";
    }

    @GetMapping("/settings")
    public String loadSettingsPage() {
        return "settings"; // returns settings.html
    }



}
