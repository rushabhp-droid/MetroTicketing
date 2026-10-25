package com.mumbaimetro.api;

import com.mumbaimetro.data.DataStore;
import com.mumbaimetro.data.MetroDatabaseManager;
import com.mumbaimetro.model.Transaction;
import com.mumbaimetro.service.MetroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminApiController {

    private final MetroService metroService;

    public AdminApiController(MetroService metroService) {
        this.metroService = metroService;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStations", metroService.getStations().size());
        stats.put("totalCards", metroService.getAllCards().size());
        stats.put("totalTransactions", metroService.getAllTransactions().size());
        stats.put("totalRevenue", DataStore.getInstance().getTotalRevenue());
        stats.put("dbPath", MetroDatabaseManager.getDatabasePath());
        stats.put("traffic", DataStore.getInstance().getStationTraffic());
        stats.put("lines", metroService.getAvailableLines());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions() {
        return ResponseEntity.ok(metroService.getAllTransactions());
    }

    public static class SqlQueryRequest {
        public String sql;
    }

    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody SqlQueryRequest req) {
        try {
            MetroDatabaseManager.RawQueryResult res = MetroDatabaseManager.executeRawQuery(req.sql);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("columnNames", res.columnNames);
            resp.put("rows", res.rows);
            resp.put("rowCount", res.rowCount);
            resp.put("executionTimeMs", res.executionTimeMs);
            resp.put("message", res.message);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
