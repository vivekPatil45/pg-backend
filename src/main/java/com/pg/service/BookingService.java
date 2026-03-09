package com.pg.service;

import com.pg.dto.request.CreateBookingRequest;
import com.pg.entity.Tenant;
import com.pg.entity.Booking;
import com.pg.entity.Room;
import com.pg.entity.Bill;
import com.pg.enums.PaymentStatus;
import com.pg.enums.BookingStatus;
import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.TenantRepository;
import com.pg.repository.BookingRepository;
import com.pg.repository.RoomRepository;
import com.pg.repository.BedRepository;

import com.pg.enums.BedStatus;
import com.pg.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final com.pg.repository.UserRepository userRepository;
    private final IdGenerator idGenerator;
    private final BillService billService;
    private final BedService bedService;

    @Transactional
    public Booking createBooking(String userId, CreateBookingRequest request) {
        // Find tenant by user ID, or create if not exists
        Tenant tenant = tenantRepository.findByUser_UserId(userId)
                .orElseGet(() -> {
                    com.pg.entity.User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Tenant newTenant = new Tenant();
                    newTenant.setTenantId(idGenerator.generateTenantId());
                    newTenant.setUser(user);
                    newTenant.setTotalBookings(0);
                    return tenantRepository.save(newTenant);
                });

        // Find room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        // Validate move-in date
        if (request.getMoveInDate() == null) {
            throw new InvalidRequestException("Move-in date is required.");
        }
        if (request.getMoveInDate().isBefore(LocalDate.now())) {
            throw new InvalidRequestException("Move-in date cannot be in the past.");
        }

        // Check room availability
        if (!room.getAvailability()) {
            throw new InvalidRequestException("Room is not available for booking.");
        }

        // Check for existing active bookings for this room
        var activeBookings = bookingRepository.findByRoomAndStatusIn(
                room, java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE));
        if (!activeBookings.isEmpty()) {
            throw new InvalidRequestException("Room is already occupied.");
        }

        // First month's rent is the total amount
        BigDecimal totalAmount = room.getPrice();

        // Create booking
        Booking booking = new Booking();
        booking.setBookingId(idGenerator.generateBookingId());
        booking.setTenant(tenant);
        booking.setRoom(room);
        booking.setMoveInDate(request.getMoveInDate());
        booking.setTotalAmount(totalAmount);
        booking.setRequestedRoomType(request.getRequestedRoomType());
        booking.setRequestedBedId(request.getBedId());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        // Increment tenant's total bookings
        tenant.setTotalBookings(tenant.getTotalBookings() + 1);
        tenantRepository.save(tenant);

        return savedBooking;
    }

    public Booking getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkCancellation(String bookingId) {
        Booking booking = getBookingById(bookingId);
        Map<String, Object> response = new HashMap<>();

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            response.put("allowed", false);
            response.put("message", "This booking cannot be cancelled as it is already cancelled or completed.");
            return response;
        }

        BigDecimal refundAmount = booking.getPaymentStatus() == PaymentStatus.PAID
                ? booking.getTotalAmount()
                : BigDecimal.ZERO;

        response.put("allowed", true);
        response.put("refundAmount", refundAmount);
        response.put("totalAmount", booking.getTotalAmount());
        response.put("message", "You can cancel this booking. Refund: ₹" + refundAmount);
        return response;
    }

    @Transactional
    public void cancelBooking(String bookingId, String cancellationReason) {
        Booking booking = getBookingById(bookingId);

        Map<String, Object> check = checkCancellation(bookingId);
        if (!(Boolean) check.get("allowed")) {
            throw new InvalidRequestException((String) check.get("message"));
        }

        BigDecimal refundAmount = (BigDecimal) check.get("refundAmount");

        booking.setStatus(BookingStatus.CANCELLED);
        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            booking.setPaymentStatus(refundAmount.compareTo(BigDecimal.ZERO) > 0
                    ? PaymentStatus.REFUNDED
                    : PaymentStatus.PAID);
        }
        booking.setCancellationReason(cancellationReason);
        booking.setCancellationDate(java.time.LocalDateTime.now());
        booking.setRefundAmount(refundAmount);

        bookingRepository.save(booking);

        // Free up the bed if assigned
        bedRepository.findByTenant_TenantId(booking.getTenant().getTenantId()).ifPresent(bed -> {
            if (bed.getRoom().getRoomId().equals(booking.getRoom().getRoomId())) {
                bed.setTenant(null);
                bed.setStatus(BedStatus.AVAILABLE);
                bedRepository.save(bed);
            }
        });
    }

    @Transactional
    public Booking confirmPayment(String bookingId, com.pg.enums.PaymentMethod paymentMethod, String transactionId) {
        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidRequestException("Cannot confirm payment for a cancelled booking.");
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaymentMethod(paymentMethod);
        booking.setTransactionId(transactionId);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        // Auto-assign requested bed in the room
        if (booking.getRequestedBedId() != null && !booking.getRequestedBedId().isEmpty()) {
            bedRepository.findById(booking.getRequestedBedId())
                    .ifPresent(bed -> {
                        if (bed.getStatus() == BedStatus.AVAILABLE
                                && bed.getRoom().getRoomId().equals(booking.getRoom().getRoomId())) {
                            bed.setTenant(booking.getTenant());
                            bed.setStatus(BedStatus.RESERVED);
                            bedRepository.save(bed);
                            bedService.syncRoomAvailability(bed.getRoom());
                        }
                    });
        }

        // Generate the bill for Admin tracking
        billService.generateBill(savedBooking.getBookingId());

        // Mark the generated bill as paid since it was just paid
        Bill bill = billService.getBillByBookingId(savedBooking.getBookingId());
        if (bill != null) {
            bill.setPaymentMethod(paymentMethod);
            bill.setTransactionId(transactionId);
            bill.setPaymentStatus(PaymentStatus.PAID);
            billService.getBillRepository().save(bill);
        }

        return savedBooking;
    }
}
