package com.mumbaimetro.controller;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.service.MetroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final MetroService metroService;

    public HomeController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping({"/", "/app", "/react"})
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping({"/classic", "/dashboard", "/home"})
    public String dashboard(Model model) {
        model.addAttribute("stations", metroService.getStations());
        model.addAttribute("lines", metroService.getAvailableLines());
        model.addAttribute("cards", metroService.getAllCards());
        model.addAttribute("transactions", metroService.getAllTransactions());
        model.addAttribute("revenue", DataStore.getInstance().getTotalRevenue());
        model.addAttribute("traffic", DataStore.getInstance().getStationTraffic());
        return "dashboard";
    }
}
