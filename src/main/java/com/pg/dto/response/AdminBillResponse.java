package com.pg.dto.response;

import com.pg.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdminBillResponse {
    private String billId;
    private String tenantName;
    private String tenantPhone;
    private String roomNumber;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private LocalDate billDate;
    private LocalDate dueDate;
    private PaymentStatus paymentStatus;
    private String transactionId;
    private LocalDateTime createdAt;
}
