package com.pg.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotNull(message = "Move-in date is required")
    @FutureOrPresent(message = "Move-in date cannot be in the past.")
    private LocalDate moveInDate;

    @NotBlank(message = "Bed ID is required")
    private String bedId;

    private String requestedRoomType;
}
