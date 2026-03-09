package com.pg.controller;

import com.pg.dto.response.AdminBookingResponse;
import com.pg.dto.request.AdminCreateBookingRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.entity.Booking;
import com.pg.enums.BookingStatus;
import com.pg.service.AdminBookingService;
import com.pg.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminBookingResponse>>> getAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminBookingResponse> bookings = adminBookingService.getAllBookings(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched successfully", bookings));
    }

    @PutMapping("/{bookingId}/approve")
    public ResponseEntity<ApiResponse<AdminBookingResponse>> approveBooking(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> request) {

        String roomId = request.get("roomId");
        String bedId = request.get("bedId");
        String notes = request.get("notes");

        AdminBookingResponse response = adminBookingService.approveBooking(bookingId, roomId, bedId, notes);
        return ResponseEntity.ok(ApiResponse.success("Booking approved successfully", response));
    }

    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<ApiResponse<AdminBookingResponse>> rejectBooking(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> request) {

        String reason = request.get("reason");
        AdminBookingResponse response = adminBookingService.rejectBooking(bookingId, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking rejected successfully", response));
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<AdminBookingResponse>> cancelBooking(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> request) {

        String reason = request.get("reason");
        AdminBookingResponse response = adminBookingService.cancelBooking(bookingId, reason);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminBookingResponse>> createAdminBooking(
            @Valid @RequestBody AdminCreateBookingRequest request) {
        Booking booking = adminService.createAdminBooking(
                request.getTenantEmail(), request.getRoomId(),
                request.getMoveInDate(), request.getPaymentMethod());
        return ResponseEntity.ok(ApiResponse.success("Booking created successfully",
                adminBookingService.convertToAdminResponse(booking)));
    }
}
