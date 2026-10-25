package com.mumbaimetro.api;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.model.Ticket;
import com.mumbaimetro.qr.QRCodeGenerator;
import com.mumbaimetro.service.MetroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketApiController {

    private final MetroService metroService;

    public TicketApiController(MetroService metroService) {
        this.metroService = metroService;
    }

    public static class TicketRequest {
        public String source;
        public String destination;
        public int passengers = 1;
        public boolean returnTrip = false;
        public boolean peakHour = false;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookTicket(@RequestBody TicketRequest req) {
        try {
            Station src = DataStore.getInstance().getStationByName(req.source);
            Station dst = DataStore.getInstance().getStationByName(req.destination);
            if (src == null || dst == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Source or destination station is invalid."));
            }
            if (!src.getLineCode().equals(dst.getLineCode())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot select stations from two different lines! Both stations must be on the same Metro line."));
            }

            Ticket ticket = metroService.bookSingleJourneyToken(src, dst, req.passengers, req.returnTrip, req.peakHour);
            String qrBase64 = QRCodeGenerator.generateQRCodeBase64(ticket.getQrData(), 6);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("ticketId", ticket.getTicketId());
            resp.put("source", ticket.getSource().getName());
            resp.put("sourceLine", ticket.getSource().getLineName());
            resp.put("destination", ticket.getDestination().getName());
            resp.put("destinationLine", ticket.getDestination().getLineName());
            resp.put("distanceKm", ticket.getDistanceKm());
            resp.put("unitFare", ticket.getUnitFare());
            resp.put("passengers", ticket.getPassengerCount());
            resp.put("isReturn", ticket.isReturnTrip());
            resp.put("totalAmount", ticket.getTotalAmount());
            resp.put("ticketType", ticket.getTicketType());
            resp.put("bookingTime", ticket.getBookingTime());
            resp.put("receipt", ticket.generateReceipt());
            resp.put("qrBase64", qrBase64);
            resp.put("qrData", ticket.getQrData());

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
