package com.mumbaimetro.api;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.model.Station;
import com.mumbaimetro.service.FareCalculator;
import com.mumbaimetro.service.MetroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/stations")
@CrossOrigin(origins = "*")
public class StationApiController {

    private final MetroService metroService;

    public StationApiController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(metroService.getStations());
    }

    @GetMapping("/lines")
    public ResponseEntity<Map<String, String>> getLines() {
        return ResponseEntity.ok(metroService.getAvailableLines());
    }

    @GetMapping("/by-line/{lineCode}")
    public ResponseEntity<List<Station>> getStationsByLine(@PathVariable String lineCode) {
        return ResponseEntity.ok(metroService.getStationsByLine(lineCode));
    }

    @GetMapping("/fare-preview")
    public ResponseEntity<?> getFarePreview(@RequestParam String source,
                                            @RequestParam String dest,
                                            @RequestParam(defaultValue = "1") int passengers,
                                            @RequestParam(defaultValue = "false") boolean isReturn,
                                            @RequestParam(defaultValue = "false") boolean isPeakHour) {
        try {
            Station src = DataStore.getInstance().getStationByName(source);
            Station dst = DataStore.getInstance().getStationByName(dest);
            if (src == null || dst == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid station identifier"));
            }
            if (!src.getLineCode().equals(dst.getLineCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot select stations from two different lines. Both stations must be on the same Metro line."));
            }
            FareCalculator calc = metroService.getFareCalculator();
            double distance = calc.calculateDistance(src, dst);
            double baseUnit = calc.calculateBaseFare(distance);
            double totalFare = calc.calculateTokenFare(src, dst, passengers, isReturn, isPeakHour);
            double studentUnit = baseUnit * 0.80; // 20% concession preview

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("source", src.getName());
            resp.put("destination", dst.getName());
            resp.put("distanceKm", Math.round(distance * 10.0) / 10.0);
            resp.put("baseFare", baseUnit);
            resp.put("studentFare", studentUnit);
            resp.put("totalFare", totalFare);
            resp.put("isPeakHour", isPeakHour);
            resp.put("isReturn", isReturn);
            resp.put("passengers", passengers);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
