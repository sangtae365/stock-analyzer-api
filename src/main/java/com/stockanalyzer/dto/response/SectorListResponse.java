package com.stockanalyzer.dto.response;

import java.util.List;

public record SectorListResponse(List<SectorDto> sectors) {
    public record SectorDto(String sectorId, String name, int stockCount) {}
}
