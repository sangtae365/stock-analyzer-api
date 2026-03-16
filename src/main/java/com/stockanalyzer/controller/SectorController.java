package com.stockanalyzer.controller;

import com.stockanalyzer.dto.response.SectorListResponse;
import com.stockanalyzer.dto.response.SectorStockResponse;
import com.stockanalyzer.service.SectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sectors")
public class SectorController {

    private final SectorService sectorService;

    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping
    public ResponseEntity<SectorListResponse> getSectors() {
        return ResponseEntity.ok(sectorService.getSectors());
    }

    @GetMapping("/{sectorId}/stocks")
    public ResponseEntity<SectorStockResponse> getSectorStocks(
            @PathVariable                              String sectorId,
            @RequestParam(defaultValue = "marketCap") String sort,
            @RequestParam(defaultValue = "desc")      String order
    ) {
        return ResponseEntity.ok(sectorService.getSectorStocks(sectorId, sort, order));
    }
}
