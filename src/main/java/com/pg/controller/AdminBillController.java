package com.pg.controller;

import com.pg.dto.response.AdminBillResponse;
import com.pg.dto.response.ApiResponse;
import com.pg.entity.Bill;
import com.pg.entity.BillItem;
import com.pg.enums.PaymentMethod;
import com.pg.enums.PaymentStatus;
import com.pg.service.AdminBillService;
import com.pg.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillController {

    private final AdminBillService adminBillService;
    private final BillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBillResponse>>> getAllPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminBillResponse> payments = adminBillService.getAllBills(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", payments));
    }

    @PutMapping("/{billId}/receive")
    public ResponseEntity<ApiResponse<AdminBillResponse>> markAsReceived(
            @PathVariable String billId,
            @RequestBody Map<String, Object> request) {

        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        PaymentMethod method = PaymentMethod.valueOf(request.get("paymentMethod").toString());

        AdminBillResponse response = adminBillService.markAsReceived(billId, amount, method);
        return ResponseEntity.ok(ApiResponse.success("Payment marked as received", response));
    }

    @PostMapping("/receipt/{billId}")
    public ResponseEntity<ApiResponse<AdminBillResponse>> generateReceipt(@PathVariable String billId) {
        AdminBillResponse response = adminBillService.generateReceipt(billId);
        return ResponseEntity.ok(ApiResponse.success("Receipt generated successfully", response));
    }

    @PostMapping("/generate/{bookingId}")
    public ResponseEntity<ApiResponse<AdminBillResponse>> generateBill(@PathVariable String bookingId) {
        Bill bill = billService.generateBill(bookingId);
        return ResponseEntity
                .ok(ApiResponse.success("Bill generated successfully", adminBillService.convertToAdminResponse(bill)));
    }

    @PostMapping("/{billId}/items")
    public ResponseEntity<ApiResponse<AdminBillResponse>> addBillItem(
            @PathVariable String billId,
            @RequestBody BillItem item) {
        Bill bill = billService.addItemToBill(billId, item);
        return ResponseEntity.ok(
                ApiResponse.success("Item added to bill successfully", adminBillService.convertToAdminResponse(bill)));
    }
}
