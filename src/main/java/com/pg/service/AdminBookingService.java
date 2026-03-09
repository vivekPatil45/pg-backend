package com.pg.service;

import com.pg.dto.response.AdminBookingResponse;
import com.pg.entity.Bed;
import com.pg.entity.Booking;
import com.pg.entity.Room;
import com.pg.enums.BedStatus;
import com.pg.enums.BookingStatus;
import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.BedRepository;
import com.pg.repository.BookingRepository;
import com.pg.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BedService bedService;

    public Page<AdminBookingResponse> getAllBookings(BookingStatus status, String search, Pageable pageable) {
        // Simple search and filter implementation
        // In a real app, this would use a Specification or QueryDSL
        Page<Booking> bookings;
        if (status != null && search != null && !search.isEmpty()) {
            bookings = bookingRepository.findByStatusAndTenant_User_NameContainingIgnoreCase(status, search, pageable);
        } else if (status != null) {
            bookings = bookingRepository.findByStatus(status, pageable);
        } else if (search != null && !search.isEmpty()) {
            bookings = bookingRepository.findByTenant_User_NameContainingIgnoreCase(search, pageable);
        } else {
            bookings = bookingRepository.findAll(pageable);
        }

        return bookings.map(this::convertToAdminResponse);
    }

    @Transactional
    public AdminBookingResponse approveBooking(String bookingId, String roomId, String bedId, String notes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidRequestException("Only pending bookings can be approved");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        if (bed.getStatus() != BedStatus.AVAILABLE) {
            throw new InvalidRequestException("Selected bed is not available");
        }

        // Assign bed to tenant
        bed.setTenant(booking.getTenant());
        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);
        bedService.syncRoomAvailability(room);

        // Update booking
        booking.setRoom(room);
        booking.setStatus(BookingStatus.APPROVED);
        booking.setAdminNotes(notes);
        bookingRepository.save(booking);

        return convertToAdminResponse(booking);
    }

    @Transactional
    public AdminBookingResponse rejectBooking(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidRequestException("Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setAdminNotes(reason);
        bookingRepository.save(booking);

        return convertToAdminResponse(booking);
    }

    @Transactional
    public AdminBookingResponse cancelBooking(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAdminNotes(reason);
        booking.setCancellationDate(LocalDateTime.now());
        bookingRepository.save(booking);

        // Release bed if assigned
        bedRepository.findByTenant_TenantId(booking.getTenant().getTenantId()).ifPresent(bed -> {
            if (bed.getRoom().getRoomId().equals(booking.getRoom().getRoomId())) {
                bed.setTenant(null);
                bed.setStatus(BedStatus.AVAILABLE);
                bedRepository.save(bed);
                bedService.syncRoomAvailability(bed.getRoom());
            }
        });

        return convertToAdminResponse(booking);
    }

    public AdminBookingResponse convertToAdminResponse(Booking booking) {
        AdminBookingResponse response = new AdminBookingResponse();
        response.setBookingId(booking.getBookingId());
        response.setTenantName(booking.getTenant().getUser().getName());
        response.setTenantEmail(booking.getTenant().getUser().getEmail());
        response.setTenantPhone(booking.getTenant().getUser().getPhone());
        response.setRoomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : null);

        // Find assigned bed if any
        bedRepository.findByTenant_TenantId(booking.getTenant().getTenantId()).ifPresent(bed -> {
            if (booking.getRoom() != null && bed.getRoom().getRoomId().equals(booking.getRoom().getRoomId())) {
                response.setBedNumber(bed.getBedNumber().toString());
            }
        });

        response.setRequestedRoomType(booking.getRequestedRoomType());
        response.setRequestedBedId(booking.getRequestedBedId());
        response.setMoveInDate(booking.getMoveInDate());
        response.setTotalAmount(booking.getTotalAmount());
        response.setStatus(booking.getStatus());
        response.setPaymentStatus(booking.getPaymentStatus());
        response.setAdminNotes(booking.getAdminNotes());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}
