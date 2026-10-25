package com.mumbaimetro.api;

import com.mumbaimetro.threads.PeakHourMonitorThread;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
@CrossOrigin(origins = "*")
public class StatusApiController {

    private final PeakHourMonitorThread monitorThread;

    public StatusApiController(PeakHourMonitorThread monitorThread) {
        this.monitorThread = monitorThread;
    }

    @GetMapping
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("systemName", "Mumbai Metro One (MMOPL) Automated Ticketing System");
        status.put("version", "2.0.0 (Spring Boot + React SPA)");
        status.put("serverTime", LocalDateTime.now().toString());
        status.put("isPeakHour", monitorThread.isPeakHour());
        status.put("peakSurcharge", monitorThread.isPeakHour() ? 5.0 : 0.0);
        status.put("activeThreadName", monitorThread.getName());
        status.put("threadAlive", monitorThread.isAlive());
        return ResponseEntity.ok(status);
    }
}
