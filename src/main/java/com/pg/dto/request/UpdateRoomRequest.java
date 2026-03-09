package com.pg.dto.request;

import com.pg.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {

    private String roomNumber;

    private RoomType roomType;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private List<String> amenities;

    @Size(max = 500)
    private String description;

    private Boolean availability;

    @Min(value = 1)
    @Max(value = 10)
    private Integer totalBeds;

    @Min(value = 1)
    @Max(value = 50)
    private Integer floor;

    @Min(value = 1)
    private Integer roomSize;

    private List<String> images;
}
