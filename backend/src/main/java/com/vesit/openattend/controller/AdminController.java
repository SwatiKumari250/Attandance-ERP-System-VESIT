package com.vesit.openattend.controller;

import com.vesit.openattend.dto.admin.*;
import com.vesit.openattend.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/roster/preview")
    public ResponseEntity<RosterPreviewResponse> previewRoster(@RequestBody List<RosterRowDto> rows) {
        RosterPreviewResponse response = adminService.previewRoster(rows);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/roster/commit")
    public ResponseEntity<Map<String, Object>> commitRoster(@RequestBody List<RosterRowDto> rows) {
        int committed = adminService.commitRoster(rows);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Roster committed successfully",
                "count", committed
        ));
    }

    @GetMapping("/sync/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<SyncLogResponse> logs = adminService.getSyncLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
                "success", true,
                "logs", logs
        ));
    }
}
