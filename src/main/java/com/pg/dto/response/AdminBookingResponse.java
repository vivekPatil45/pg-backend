package com.pg.dto.response;

import com.pg.enums.BookingStatus;
import com.pg.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminBookingResponse {
    private String bookingId;
    private String tenantName;
    private String tenantPhone;
    private String tenantEmail;
    private String roomNumber;
    private String bedNumber;
    private String requestedRoomType;
    private String requestedBedId;
    private LocalDate moveInDate;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String adminNotes;
    private LocalDateTime createdAt;
}
