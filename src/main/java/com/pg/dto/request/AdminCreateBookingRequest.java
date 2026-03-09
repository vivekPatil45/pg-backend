package com.pg.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class AdminCreateBookingRequest {

    @NotBlank(message = "Tenant name is required")
    private String tenantName;

    @NotBlank(message = "Tenant email is required")
    private String tenantEmail;

    private String tenantPhone;

    @NotBlank(message = "Room ID is required")
    private String roomId;

    @NotNull(message = "Move-in date is required")
    @FutureOrPresent(message = "Move-in date cannot be in the past")
    private LocalDate moveInDate;

    private String paymentMethod;
}
