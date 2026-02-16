package com.supermarket.pos.controller;

import com.supermarket.common.dto.ApiResponse;
import com.supermarket.pos.entity.POSTerminal;
import com.supermarket.pos.service.POSTerminalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pos/terminals")
public class POSTerminalController {

    private final POSTerminalService posTerminalService;

    public POSTerminalController(POSTerminalService posTerminalService) {
        this.posTerminalService = posTerminalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<POSTerminal>> createTerminal(
            @RequestBody POSTerminal terminal,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        POSTerminal created = posTerminalService.createTerminal(terminal, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Terminal created", created, null));
    }

    @GetMapping("/{terminalCode}")
    public ResponseEntity<ApiResponse<POSTerminal>> getTerminal(
            @PathVariable String terminalCode,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        POSTerminal terminal = posTerminalService.getTerminalByCode(terminalCode, tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Terminal retrieved", terminal, null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<POSTerminal>>> getAllTerminals(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<POSTerminal> terminals = posTerminalService.getAllTerminals(tenantId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Terminals retrieved", terminals, null));
    }
}
