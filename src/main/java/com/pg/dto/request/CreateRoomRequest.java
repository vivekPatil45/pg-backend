package com.pg.dto.request;

import com.pg.enums.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 20, message = "Room number cannot exceed 20 characters")
    private String roomNumber;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Monthly price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    private BigDecimal price;

    private List<String> amenities = new ArrayList<>();

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean availability;

    @NotNull(message = "Total beds is required")
    @Min(value = 1, message = "Total beds must be at least 1")
    @Max(value = 10, message = "Total beds cannot exceed 10")
    private Integer totalBeds;

    @NotNull(message = "Floor is required")
    @Min(value = 1, message = "Floor must be at least 1")
    @Max(value = 50, message = "Floor cannot exceed 50")
    private Integer floor;

    @NotNull(message = "Room size is required")
    @Min(value = 1, message = "Room size must be at least 1 sq ft")
    private Integer roomSize;

    private List<String> images = new ArrayList<>();
}
