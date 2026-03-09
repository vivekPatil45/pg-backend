package com.pg.dto.response;

import com.pg.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailResponse {

    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private BigDecimal price; // monthly rent
    private Integer totalBeds; // total capacity
    private Integer availableBeds; // beds with status = AVAILABLE
    private Boolean availability;
    private String description;
    private Integer floor;
    private Integer roomSize; // in sq ft
    @lombok.Builder.Default
    private List<String> amenities = new ArrayList<>();
    @lombok.Builder.Default
    private List<String> images = new ArrayList<>();
}
