package com.stockanalyzer.dto.response;

import com.stockanalyzer.dto.StockDto;

import java.util.List;

public record SectorStockResponse(String sectorId, String sectorName, List<StockDto> stocks) {}
