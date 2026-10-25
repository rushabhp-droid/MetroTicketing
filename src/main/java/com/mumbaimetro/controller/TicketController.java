package com.mumbaimetro.controller;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.model.Ticket;
import com.mumbaimetro.service.MetroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private final MetroService metroService;

    public TicketController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping
    public String ticketsPage(Model model) {
        model.addAttribute("stations", metroService.getStations());
        model.addAttribute("lines", metroService.getAvailableLines());
        model.addAttribute("transactions", metroService.getAllTransactions());
        return "tickets";
    }

    @PostMapping("/book")
    public String bookTicket(@RequestParam("source") String sourceName,
                            @RequestParam("destination") String destName,
                            @RequestParam(value = "passengers", defaultValue = "1") int passengers,
                            @RequestParam(value = "returnTicket", defaultValue = "false") boolean isReturn,
                            @RequestParam(value = "peakHour", defaultValue = "false") boolean isPeakHour,
                            RedirectAttributes redirectAttributes) {
        try {
            Station src = DataStore.getInstance().getStationByName(sourceName);
            Station dest = DataStore.getInstance().getStationByName(destName);
            if (src == null || dest == null) {
                redirectAttributes.addFlashAttribute("error", "Please select valid origin and destination stations.");
                return "redirect:/tickets";
            }
            if (!src.getLineCode().equals(dest.getLineCode())) {
                redirectAttributes.addFlashAttribute("error", "Cannot select stations from two different lines! Both stations must be on the same Metro line.");
                return "redirect:/tickets";
            }
            Ticket ticket = metroService.bookSingleJourneyToken(src, dest, passengers, isReturn, isPeakHour);
            redirectAttributes.addFlashAttribute("success", "Ticket booked successfully!");
            redirectAttributes.addFlashAttribute("receipt", ticket.generateReceipt());
            redirectAttributes.addFlashAttribute("qr", com.mumbaimetro.qr.QRCodeGenerator.generateQRCodeBase64(ticket.getQrData(), 6));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tickets";
    }
}
