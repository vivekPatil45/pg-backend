package com.pg.service;

import com.pg.entity.Bill;
import com.pg.entity.BillItem;
import com.pg.entity.Booking;
import com.pg.enums.BillItemCategory;
import com.pg.enums.PaymentMethod;
import com.pg.enums.PaymentStatus;
import com.pg.enums.BookingStatus;
import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.BillRepository;
import com.pg.repository.BookingRepository;
import com.pg.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillService {

    private final BillRepository billRepository;
    private final BookingRepository bookingRepository;
    private final IdGenerator idGenerator;

    public BillRepository getBillRepository() {
        return billRepository;
    }

    @Transactional
    public Bill generateBill(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Avoid duplicate bill for the initial booking bill
        if (billRepository.findFirstByBooking_BookingIdOrderByCreatedAtAsc(bookingId).isPresent()) {
            return billRepository.findFirstByBooking_BookingIdOrderByCreatedAtAsc(bookingId).get();
        }

        Bill bill = new Bill();
        bill.setBillId(idGenerator.generateBillId());
        bill.setBooking(booking);
        bill.setTenant(booking.getTenant());
        bill.setBillDate(LocalDate.now());
        bill.setDueDate(LocalDate.now().plusDays(7));
        bill.setPaymentStatus(PaymentStatus.PENDING);

        List<BillItem> items = new ArrayList<>();

        // Monthly rent bill item
        BillItem roomCharge = new BillItem();
        roomCharge.setItemId(idGenerator.generateItemId());
        roomCharge.setDescription("Monthly Rent - Room " + booking.getRoom().getRoomNumber());
        roomCharge.setCategory(BillItemCategory.ROOM);
        roomCharge.setQuantity(1);
        roomCharge.setUnitPrice(booking.getRoom().getPrice());
        roomCharge.setTotalPrice(booking.getRoom().getPrice());
        items.add(roomCharge);

        bill.setItems(items);
        bill.setSubtotal(booking.getTotalAmount());
        bill.setTaxRate(BigDecimal.ZERO);
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setTotalAmount(booking.getTotalAmount());
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(booking.getTotalAmount());

        return billRepository.save(bill);
    }

    public Bill getBillByBookingId(String bookingId) {
        return billRepository.findFirstByBooking_BookingIdOrderByCreatedAtAsc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found for booking: " + bookingId));
    }

    /**
     * Generates a BRAND NEW monthly rent bill for recurring payment.
     * Unlike generateBill(), this always creates a new bill entry.
     */
    @Transactional
    public Bill generateMonthlyRentBill(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        Bill bill = new Bill();
        bill.setBillId(idGenerator.generateBillId());
        bill.setBooking(booking);
        bill.setTenant(booking.getTenant());
        bill.setBillDate(LocalDate.now());
        bill.setDueDate(LocalDate.now().plusDays(7));
        bill.setPaymentStatus(PaymentStatus.PENDING);

        List<BillItem> items = new ArrayList<>();
        BillItem roomCharge = new BillItem();
        roomCharge.setItemId(idGenerator.generateItemId());
        roomCharge.setDescription("Monthly Rent - Room " + booking.getRoom().getRoomNumber()
                + " (" + LocalDate.now().getMonth() + " " + LocalDate.now().getYear() + ")");
        roomCharge.setCategory(BillItemCategory.ROOM);
        roomCharge.setQuantity(1);
        roomCharge.setUnitPrice(booking.getRoom().getPrice());
        roomCharge.setTotalPrice(booking.getRoom().getPrice());
        items.add(roomCharge);

        bill.setItems(items);
        bill.setSubtotal(booking.getRoom().getPrice());
        bill.setTaxRate(BigDecimal.ZERO);
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setDiscountAmount(BigDecimal.ZERO);
        bill.setTotalAmount(booking.getRoom().getPrice());
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(booking.getRoom().getPrice());

        return billRepository.save(bill);
    }

    public Bill getBillByReservationId(String bookingId) {
        return getBillByBookingId(bookingId);
    }

    public Bill getBillById(String billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
    }

    @Transactional
    public Bill updatePayment(String billId, BigDecimal amount, PaymentMethod paymentMethod) {
        Bill bill = getBillById(billId);

        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Bill is already paid");
        }

        BigDecimal newPaidAmount = bill.getPaidAmount().add(amount);

        if (newPaidAmount.compareTo(bill.getTotalAmount()) > 0) {
            throw new InvalidRequestException("Payment amount exceeds remaining balance");
        }

        bill.setPaidAmount(newPaidAmount);
        bill.setBalanceAmount(bill.getTotalAmount().subtract(newPaidAmount));
        bill.setPaymentMethod(paymentMethod);
        bill.setTransactionId(idGenerator.generateTransactionId());

        // If this is the final payment and there's no balance left, mark the associated
        // booking as completed
        if (bill.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            bill.setPaymentStatus(PaymentStatus.PAID);
            Booking booking = bill.getBooking();
            booking.setPaymentStatus(PaymentStatus.PAID);
            if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CONFIRMED);
            }
            bookingRepository.save(booking);
        } else {
            bill.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        return billRepository.save(bill);
    }

    @Transactional
    public Bill addItemToBill(String billId, BillItem item) {
        Bill bill = getBillById(billId);
        if (bill.getPaymentStatus() == PaymentStatus.PAID) {
            throw new InvalidRequestException("Cannot add items to a paid bill");
        }

        item.setItemId(idGenerator.generateItemId());
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        bill.getItems().add(item);

        BigDecimal additionalAmount = item.getTotalPrice();
        bill.setSubtotal(bill.getSubtotal().add(additionalAmount));
        bill.setTotalAmount(bill.getTotalAmount().add(additionalAmount));
        bill.setBalanceAmount(bill.getTotalAmount().subtract(bill.getPaidAmount()));

        return billRepository.save(bill);
    }

    @Transactional
    public void updateBillStatus(String billId, PaymentStatus status) {
        Bill bill = getBillById(billId);
        bill.setPaymentStatus(status);
        billRepository.save(bill);
    }

    @Transactional
    public Bill syncBillWithReservation(Booking booking) {
        Bill bill;
        try {
            bill = getBillByBookingId(booking.getBookingId());
        } catch (ResourceNotFoundException e) {
            return null;
        }

        bill.setSubtotal(booking.getTotalAmount());
        bill.setTotalAmount(booking.getTotalAmount());
        bill.setBalanceAmount(booking.getTotalAmount().subtract(bill.getPaidAmount()));

        if (bill.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            bill.setPaymentStatus(PaymentStatus.PAID);
        } else if (bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            bill.setPaymentStatus(PaymentStatus.PARTIAL);
        } else {
            bill.setPaymentStatus(PaymentStatus.PENDING);
        }

        return billRepository.save(bill);
    }
}
