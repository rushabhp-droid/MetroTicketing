package com.mumbaimetro.api;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.*;
import com.mumbaimetro.service.MetroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "*")
public class CardApiController {

    private final MetroService metroService;

    public CardApiController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping
    public ResponseEntity<List<Card>> getAllCards() {
        return ResponseEntity.ok(metroService.getAllCards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCard(@PathVariable String id) {
        Card card = metroService.getCard(id);
        if (card == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(card);
    }

    public static class IssueCardRequest {
        public String holderName;
        public String contact;
        public String type = "Regular Card";
        public double initialBalance = 100.0;
        public String college;
        public String rollNo;
        public int validityDays = 30;
    }

    @PostMapping("/issue")
    public ResponseEntity<?> issueCard(@RequestBody IssueCardRequest req) {
        try {
            Card card = metroService.issueCard(
                    req.holderName, req.contact, req.type, req.initialBalance,
                    req.college, req.rollNo, req.validityDays
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card issued successfully! ID: " + card.getCardId(),
                    "card", card
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public static class RechargeRequest {
        public String cardId;
        public double amount;
    }

    @PostMapping("/recharge")
    public ResponseEntity<?> rechargeCard(@RequestBody RechargeRequest req) {
        try {
            double newBalance = metroService.rechargeCard(req.cardId, req.amount);
            Card card = metroService.getCard(req.cardId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Recharged ₹%.2f successfully! New Balance: ₹%.2f", req.amount, newBalance),
                    "balance", newBalance,
                    "card", card
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public static class TapInRequest {
        public String cardId;
        public String station;
    }

    @PostMapping("/tap-in")
    public ResponseEntity<?> tapIn(@RequestBody TapInRequest req) {
        try {
            Station station = DataStore.getInstance().getStationByName(req.station);
            if (station == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid entry station"));
            }
            metroService.processTapIn(req.cardId, station);
            Card card = metroService.getCard(req.cardId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "action", "TAP_IN",
                    "cardId", card.getCardId(),
                    "holderName", card.getHolderName(),
                    "stationName", station.getName(),
                    "lineName", station.getLineName(),
                    "balance", card.getBalance(),
                    "message", "AFC Gate Open! Welcome to Mumbai Metro at " + station.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public static class TapOutRequest {
        public String cardId;
        public String station;
        public boolean peakHour = false;
    }

    @PostMapping("/tap-out")
    public ResponseEntity<?> tapOut(@RequestBody TapOutRequest req) {
        try {
            Station station = DataStore.getInstance().getStationByName(req.station);
            if (station == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid exit station"));
            }
            MetroService.JourneyResult result = metroService.processTapOut(req.cardId, station, req.peakHour);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("action", "TAP_OUT");
            resp.put("cardId", result.card.getCardId());
            resp.put("holderName", result.card.getHolderName());
            resp.put("cardType", result.card.getClass().getSimpleName());
            resp.put("origin", result.origin.getName());
            resp.put("destination", result.destination.getName());
            resp.put("distanceKm", result.distanceKm);
            resp.put("baseFare", result.baseFare);
            resp.put("discount", result.discount);
            resp.put("finalFare", result.finalFare);
            resp.put("balanceAfter", result.remainingBalance);
            resp.put("message", String.format("Gate Open! Deducted ₹%.2f (Discount: ₹%.2f). Remaining Balance: ₹%.2f",
                    result.finalFare, result.discount, result.remainingBalance));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    public static class ToggleBlockRequest {
        public String cardId;
    }

    @PostMapping("/toggle-block")
    public ResponseEntity<?> toggleBlock(@RequestBody ToggleBlockRequest req) {
        try {
            boolean blocked = metroService.toggleBlockStatus(req.cardId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "cardId", req.cardId,
                    "isBlocked", blocked,
                    "message", blocked ? "Card has been BLOCKED." : "Card has been UNBLOCKED."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
