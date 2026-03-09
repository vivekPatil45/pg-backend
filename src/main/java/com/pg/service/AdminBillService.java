package com.pg.service;

import com.pg.dto.response.AdminBillResponse;
import com.pg.entity.Bill;
import com.pg.enums.PaymentMethod;
import com.pg.enums.PaymentStatus;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdminBillService {

    private final BillRepository billRepository;
    private final BillService billService;

    public Page<AdminBillResponse> getAllBills(PaymentStatus status, String search, Pageable pageable) {
        Page<Bill> bills;
        if (status != null && search != null && !search.isEmpty()) {
            bills = billRepository.findByPaymentStatusAndTenant_User_NameContainingIgnoreCase(status, search, pageable);
        } else if (status != null) {
            bills = billRepository.searchBills(status, null, null, pageable);
        } else if (search != null && !search.isEmpty()) {
            bills = billRepository.findByTenant_User_NameContainingIgnoreCase(search, pageable);
        } else {
            bills = billRepository.findAll(pageable);
        }

        return bills.map(this::convertToAdminResponse);
    }

    @Transactional
    public AdminBillResponse markAsReceived(String billId, BigDecimal amount, PaymentMethod paymentMethod) {
        Bill bill = billService.updatePayment(billId, amount, paymentMethod);
        return convertToAdminResponse(bill);
    }

    @Transactional
    public AdminBillResponse generateReceipt(String billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        bill.setInvoiceGenerated(true);
        // In a real app, logic to generate PDF and set invoiceUrl would go here
        bill.setInvoiceUrl("/api/v1/admin/payments/" + billId + "/receipt/download");
        return convertToAdminResponse(billRepository.save(bill));
    }

    public AdminBillResponse convertToAdminResponse(Bill bill) {
        AdminBillResponse response = new AdminBillResponse();
        response.setBillId(bill.getBillId());
        response.setTenantName(bill.getTenant().getUser().getName());
        response.setTenantPhone(bill.getTenant().getUser().getPhone());
        response.setRoomNumber(bill.getBooking().getRoom().getRoomNumber());
        response.setTotalAmount(bill.getTotalAmount());
        response.setPaidAmount(bill.getPaidAmount());
        response.setBalanceAmount(bill.getBalanceAmount());
        response.setBillDate(bill.getBillDate());
        response.setDueDate(bill.getDueDate());
        response.setPaymentStatus(bill.getPaymentStatus());
        response.setTransactionId(bill.getTransactionId());
        response.setCreatedAt(bill.getCreatedAt());
        return response;
    }
}
