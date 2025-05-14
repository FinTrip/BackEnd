package org.example.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StaticController {

    @GetMapping("/payment-mock")
    public String paymentMockPage(@RequestParam("id") String transactionId, Model model) {
        model.addAttribute("transactionId", transactionId);
        return "payment-mock";
    }
} 