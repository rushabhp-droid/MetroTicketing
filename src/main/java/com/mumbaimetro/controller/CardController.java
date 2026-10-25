package com.mumbaimetro.controller;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.Card;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.service.MetroService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cards")
public class CardController {

    private final MetroService metroService;

    public CardController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping
    public String cardsPage(Model model) {
        model.addAttribute("cards", metroService.getAllCards());
        model.addAttribute("stations", metroService.getStations());
        model.addAttribute("lines", metroService.getAvailableLines());
        return "cards";
    }

    @PostMapping("/issue")
    public String issueCard(@RequestParam("holderName") String holderName,
                           @RequestParam("contact") String contact,
                           @RequestParam("type") String type,
                           @RequestParam(value = "initialBalance", defaultValue = "100") double initialBalance,
                           @RequestParam(value = "college", required = false) String college,
                           @RequestParam(value = "rollNo", required = false) String rollNo,
                           @RequestParam(value = "validityDays", defaultValue = "30") int validityDays,
                           RedirectAttributes redirectAttributes) {
        try {
            Card card = metroService.issueCard(holderName, contact, type, initialBalance, college, rollNo, validityDays);
            redirectAttributes.addFlashAttribute("success", "Card issued: " + card.getCardId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cards";
    }

    @PostMapping("/recharge")
    public String rechargeCard(@RequestParam("cardId") String cardId,
                              @RequestParam("amount") double amount,
                              RedirectAttributes redirectAttributes) {
        try {
            double newBalance = metroService.rechargeCard(cardId, amount);
            redirectAttributes.addFlashAttribute("success", "Recharged " + cardId + ". New balance: ₹" + newBalance);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cards";
    }

    @PostMapping("/tap-in")
    public String tapIn(@RequestParam("cardId") String cardId,
                        @RequestParam("station") String stationName,
                        RedirectAttributes redirectAttributes) {
        try {
            Station station = DataStore.getInstance().getStationByName(stationName);
            metroService.processTapIn(cardId, station);
            redirectAttributes.addFlashAttribute("success", cardId + " tapped in at " + station.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cards";
    }

    @PostMapping("/tap-out")
    public String tapOut(@RequestParam("cardId") String cardId,
                         @RequestParam("station") String stationName,
                         @RequestParam(value = "peakHour", defaultValue = "false") boolean isPeakHour,
                         RedirectAttributes redirectAttributes) {
        try {
            Station station = DataStore.getInstance().getStationByName(stationName);
            MetroService.JourneyResult result = metroService.processTapOut(cardId, station, isPeakHour);
            redirectAttributes.addFlashAttribute("success", String.format("Tap-out successful! Fare: ₹%.2f, Balance: ₹%.2f", result.finalFare, result.remainingBalance));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cards";
    }

    @PostMapping("/toggle-block")
    public String toggleBlock(@RequestParam("cardId") String cardId,
                             RedirectAttributes redirectAttributes) {
        try {
            boolean blocked = metroService.toggleBlockStatus(cardId);
            redirectAttributes.addFlashAttribute("success", cardId + (blocked ? " blocked" : " unblocked"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cards";
    }
}
