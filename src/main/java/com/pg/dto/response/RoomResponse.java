package com.pg.dto.response;

import com.pg.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private String roomId;
    private String roomNumber;
    private RoomType roomType;
    private BigDecimal price; // monthly rent
    private List<String> amenities = new ArrayList<>();
    private Integer totalBeds; // total capacity
    private Integer availableBeds; // totalBeds minus active bookings
    private Boolean availability;
    private String description;
    private Integer floor;
    private Integer roomSize;
    private List<String> images = new ArrayList<>();

    // Calculated fields
    private String currentStatus; // AVAILABLE, OCCUPIED, MAINTENANCE
    private Boolean hasActiveReservations;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
