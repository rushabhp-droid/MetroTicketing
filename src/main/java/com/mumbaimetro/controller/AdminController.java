package com.mumbaimetro.controller;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.data.MetroDatabaseManager;
import com.mumbaimetro.service.MetroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final MetroService metroService;

    public AdminController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping
    public String admin(Model model) {
        model.addAttribute("stations", metroService.getStations());
        model.addAttribute("cards", metroService.getAllCards());
        model.addAttribute("transactions", metroService.getAllTransactions());
        model.addAttribute("revenue", DataStore.getInstance().getTotalRevenue());
        model.addAttribute("traffic", DataStore.getInstance().getStationTraffic());
        model.addAttribute("dbPath", MetroDatabaseManager.getDatabasePath());
        return "admin";
    }

    @PostMapping("/query")
    public String executeQuery(@RequestParam("sql") String sql, Model model, RedirectAttributes redirectAttributes) {
        try {
            MetroDatabaseManager.RawQueryResult result = MetroDatabaseManager.executeRawQuery(sql);
            model.addAttribute("stations", metroService.getStations());
            model.addAttribute("cards", metroService.getAllCards());
            model.addAttribute("transactions", metroService.getAllTransactions());
            model.addAttribute("revenue", DataStore.getInstance().getTotalRevenue());
            model.addAttribute("traffic", DataStore.getInstance().getStationTraffic());
            model.addAttribute("dbPath", MetroDatabaseManager.getDatabasePath());
            model.addAttribute("queryResult", result);
            model.addAttribute("executedSql", sql);
            return "admin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "SQL Error: " + e.getMessage());
            return "redirect:/admin";
        }
    }
}
