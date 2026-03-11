package com.pg.controller;

import com.pg.dto.request.ComplaintRequest;
import com.pg.dto.request.CreateBookingRequest;
import com.pg.dto.request.UpdateUserRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.UserResponse;
import com.pg.entity.Complaint;
import com.pg.entity.Booking;
import com.pg.entity.User;
import com.pg.repository.BookingRepository;
import com.pg.repository.UserRepository;
import com.pg.service.ComplaintService;
import com.pg.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

        private final BookingService bookingService;
        private final ComplaintService complaintService;
        private final BookingRepository bookingRepository;
        private final UserRepository userRepository;
        private final com.pg.repository.TenantRepository tenantRepository;
        private final com.pg.repository.BillRepository billRepository;
        private final com.pg.repository.BedRepository bedRepository;

        private final com.pg.service.BillService billService;

        @GetMapping("/active-stay")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveStay(Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                com.pg.entity.Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Tenant profile not found"));

                // Find most recent active or confirmed booking
                List<com.pg.enums.BookingStatus> activeStatuses = List.of(
                                com.pg.enums.BookingStatus.CONFIRMED,
                                com.pg.enums.BookingStatus.ACTIVE,
                                com.pg.enums.BookingStatus.PENDING,
                                com.pg.enums.BookingStatus.PENDING_PAYMENT);

                Booking activeBooking = bookingRepository.findFirstByTenant_TenantIdAndStatusInOrderByCreatedAtDesc(
                                tenant.getTenantId(), activeStatuses)
                                .orElse(null);

                var assignedBed = bedRepository.findByTenant_TenantId(tenant.getTenantId()).orElse(null);

                if (activeBooking == null) {
                        if (assignedBed != null) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("bookingId", "ADMIN_ASSIGNED");
                                response.put("userId", user.getUserId());
                                response.put("roomId", assignedBed.getRoom().getRoomId());
                                response.put("room", assignedBed.getRoom());
                                response.put("tenant", tenant);
                                response.put("moveInDate", tenant.getCheckInDate() != null ? tenant.getCheckInDate() : java.time.LocalDate.now());
                                response.put("status", com.pg.enums.BookingStatus.ACTIVE);
                                response.put("paymentStatus", com.pg.enums.PaymentStatus.PAID);
                                response.put("totalAmount", assignedBed.getRoom().getPrice());
                                response.put("bedId", assignedBed.getBedId());
                                response.put("bedNumber", assignedBed.getBedNumber());
                                return ResponseEntity.ok(ApiResponse.success("Active stay retrieved", response));
                        }
                        return ResponseEntity.ok(ApiResponse.success("No active stay", null));
                }

                // Look up assigned bed for this tenant in this room
                Map<String, Object> response = new HashMap<>();
                response.put("bookingId", activeBooking.getBookingId());
                response.put("userId", user.getUserId());
                response.put("roomId", activeBooking.getRoom().getRoomId());
                response.put("room", activeBooking.getRoom());
                response.put("tenant", activeBooking.getTenant());
                response.put("moveInDate", activeBooking.getMoveInDate());
                response.put("status", activeBooking.getStatus());
                response.put("paymentStatus", activeBooking.getPaymentStatus());
                response.put("paymentMethod", activeBooking.getPaymentMethod());
                response.put("transactionId", activeBooking.getTransactionId());
                response.put("totalAmount", activeBooking.getTotalAmount());
                response.put("createdAt", activeBooking.getCreatedAt());
                response.put("updatedAt", activeBooking.getUpdatedAt());

                // Include assigned bed info
                if (assignedBed != null && assignedBed.getRoom().getRoomId().equals(activeBooking.getRoom().getRoomId())) {
                        response.put("bedId", assignedBed.getBedId());
                        response.put("bedNumber", assignedBed.getBedNumber());
                }

                return ResponseEntity.ok(ApiResponse.success("Active stay retrieved", response));
        }

        @GetMapping("/bills")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getMyBills(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                com.pg.entity.Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Tenant profile not found"));

                Pageable pageable = PageRequest.of(page, size);
                org.springframework.data.domain.Page<com.pg.entity.Bill> billPage = billRepository
                                .findByTenant_TenantId(tenant.getTenantId(), pageable);

                Map<String, Object> data = new HashMap<>();
                data.put("content", billPage.getContent());
                data.put("totalElements", billPage.getTotalElements());
                data.put("totalPages", billPage.getTotalPages());

                return ResponseEntity.ok(ApiResponse.success("Bills retrieved successfully", data));
        }

        @GetMapping("/{userId}")
        public ResponseEntity<ApiResponse<UserResponse>> getProfile(
                        @PathVariable String userId,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                if (!user.getUsername().equals(userDetails.getUsername())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                UserResponse response = buildUserResponse(user);
                return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
        }

        @PutMapping("/{userId}")
        public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
                        @PathVariable String userId,
                        @RequestBody UpdateUserRequest request,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                if (!user.getUsername().equals(userDetails.getUsername())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                if (request.getName() != null)
                        user.setName(request.getName());
                if (request.getPhone() != null)
                        user.setPhone(request.getPhone());
                if (request.getAddress() != null)
                        user.setAddress(request.getAddress());

                userRepository.save(user);

                return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", buildUserResponse(user)));
        }

        @PostMapping("/bookings/{bookingId}/pay-rent")
        public ResponseEntity<ApiResponse<Map<String, Object>>> payRent(
                        @PathVariable String bookingId,
                        @RequestBody Map<String, String> requestBody,
                        Authentication authentication) {

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

                if (!booking.getTenant().getUser().getUserId().equals(user.getUserId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                if (booking.getStatus() == com.pg.enums.BookingStatus.CANCELLED
                                || booking.getStatus() == com.pg.enums.BookingStatus.COMPLETED) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Cannot pay rent for a cancelled or completed booking.");
                }

                // Determine payment method
                String methodStr = requestBody.getOrDefault("paymentMethod", "UPI");
                String transactionId = requestBody.getOrDefault("transactionId",
                                "TXN_" + java.util.UUID.randomUUID().toString().substring(0, 9).toUpperCase());
                com.pg.enums.PaymentMethod paymentMethod;
                try {
                        paymentMethod = com.pg.enums.PaymentMethod.valueOf(methodStr);
                } catch (IllegalArgumentException e) {
                        paymentMethod = com.pg.enums.PaymentMethod.UPI;
                }

                // Generate a new monthly rent bill (if one doesn't exist for this month)
                com.pg.entity.Bill newBill = billService.generateMonthlyRentBill(bookingId);
                com.pg.entity.Bill paidBill = billService.updatePayment(newBill.getBillId(),
                                newBill.getTotalAmount(), paymentMethod);
                paidBill.setTransactionId(transactionId);
                billService.getBillRepository().save(paidBill);

                Map<String, Object> data = new HashMap<>();
                data.put("billId", paidBill.getBillId());
                data.put("amount", paidBill.getTotalAmount());
                data.put("paymentStatus", paidBill.getPaymentStatus());
                data.put("transactionId", transactionId);
                data.put("message", "Rent paid successfully!");

                return ResponseEntity.ok(ApiResponse.success("Rent payment processed successfully", data));
        }

        @PostMapping("/bookings")
        public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(
                        @Valid @RequestBody CreateBookingRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Booking booking = bookingService.createBooking(user.getUserId(), request);

                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", booking.getBookingId());
                data.put("tenantId", booking.getTenant().getTenantId());
                data.put("roomId", booking.getRoom().getRoomId());
                data.put("roomNumber", booking.getRoom().getRoomNumber());
                data.put("moveInDate", booking.getMoveInDate());
                data.put("totalAmount", booking.getTotalAmount());
                data.put("status", booking.getStatus());
                data.put("paymentStatus", booking.getPaymentStatus());

                return new ResponseEntity<>(ApiResponse.success("Booking created successfully", data),
                                HttpStatus.CREATED);
        }

        @GetMapping("/bookings")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getMyBookings(
                        @RequestParam(required = false) String status,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                com.pg.entity.Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Tenant profile not found"));

                com.pg.enums.BookingStatus bookingStatus = null;
                if (status != null && !status.isEmpty()) {
                        try {
                                bookingStatus = com.pg.enums.BookingStatus.valueOf(status.toUpperCase());
                        } catch (IllegalArgumentException e) {
                                // Ignore invalid status
                        }
                }

                Pageable pageable = PageRequest.of(page, size);
                org.springframework.data.domain.Page<Booking> bookingPage;

                if (bookingStatus != null) {
                        bookingPage = bookingRepository.findByTenant_TenantIdAndStatus(tenant.getTenantId(),
                                        bookingStatus, pageable);
                } else {
                        bookingPage = bookingRepository.findByTenant_TenantId(tenant.getTenantId(), pageable);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("content", bookingPage.getContent());
                data.put("page", bookingPage.getNumber());
                data.put("size", bookingPage.getSize());
                data.put("totalElements", bookingPage.getTotalElements());
                data.put("totalPages", bookingPage.getTotalPages());

                return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", data));
        }

        @GetMapping("/bookings/{bookingId}/check-cancellation")
        public ResponseEntity<ApiResponse<Map<String, Object>>> checkCancellation(
                        @PathVariable String bookingId,
                        Authentication authentication) {
                Map<String, Object> details = bookingService.checkCancellation(bookingId);
                return ResponseEntity.ok(ApiResponse.success("Cancellation check successful", details));
        }

        @DeleteMapping("/bookings/{bookingId}")
        public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(
                        @PathVariable String bookingId,
                        @RequestBody Map<String, String> requestBody) {
                String cancellationReason = requestBody.get("cancellationReason");
                bookingService.cancelBooking(bookingId, cancellationReason);

                Booking booking = bookingService.getBookingById(bookingId);

                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", booking.getBookingId());
                data.put("status", booking.getStatus());
                data.put("refundAmount", booking.getRefundAmount());
                data.put("refundMessage", "Refund will be processed within 3-5 business days");

                return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", data));
        }

        @GetMapping("/bookings/{bookingId}")
        public ResponseEntity<ApiResponse<Booking>> getBookingById(
                        @PathVariable String bookingId,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Booking booking = bookingService.getBookingById(bookingId);

                if (!booking.getTenant().getUser().getUserId().equals(user.getUserId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                }

                return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
        }

        @PostMapping("/bookings/{bookingId}/payment")
        public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
                        @PathVariable String bookingId,
                        @Valid @RequestBody com.pg.dto.request.PaymentRequest request,
                        Authentication authentication) {
                Booking booking = bookingService.confirmPayment(bookingId, request.getPaymentMethod(),
                                request.getTransactionId());

                Map<String, Object> data = new HashMap<>();
                data.put("bookingId", booking.getBookingId());
                data.put("status", booking.getStatus());
                data.put("paymentStatus", booking.getPaymentStatus());
                data.put("transactionId", booking.getTransactionId());

                return ResponseEntity.ok(ApiResponse.success("Payment confirmed and booking completed", data));
        }

        @PostMapping("/complaints")
        public ResponseEntity<ApiResponse<Map<String, Object>>> registerComplaint(
                        @Valid @RequestBody ComplaintRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Complaint complaint = complaintService.createComplaint(user.getUserId(), request);

                Map<String, Object> data = new HashMap<>();
                data.put("complaintId", complaint.getComplaintId());
                data.put("category", complaint.getCategory());
                data.put("title", complaint.getTitle());
                data.put("status", complaint.getStatus());
                data.put("expectedResolutionDate", complaint.getExpectedResolutionDate());
                data.put("message", "Your complaint has been registered. Complaint ID: " + complaint.getComplaintId());

                return new ResponseEntity<>(ApiResponse.success("Complaint registered successfully", data),
                                HttpStatus.CREATED);
        }

        @GetMapping("/complaints")
        public ResponseEntity<ApiResponse<List<Complaint>>> getMyComplaints(Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                List<Complaint> complaints = complaintService.getCustomerComplaints(user.getUserId());
                return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", complaints));
        }

        @GetMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> getComplaintById(
                        @PathVariable String complaintId,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                try {
                        Complaint complaint = complaintService.getComplaintById(complaintId, user.getUserId());
                        return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", complaint));
                } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found");
                }
        }

        @PutMapping("/complaints/{complaintId}/status")
        public ResponseEntity<ApiResponse<Complaint>> updateComplaintStatus(
                        @PathVariable String complaintId,
                        @RequestBody Map<String, String> requestBody,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                String statusStr = requestBody.get("status");
                if (statusStr == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
                }

                try {
                        com.pg.enums.ComplaintStatus newStatus = com.pg.enums.ComplaintStatus.valueOf(statusStr);
                        Complaint complaint = complaintService.updateComplaintStatus(complaintId, newStatus,
                                        user.getUserId());
                        return ResponseEntity
                                        .ok(ApiResponse.success("Complaint status updated successfully", complaint));
                } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
                }
        }

        @PutMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> updateComplaint(
                        @PathVariable String complaintId,
                        @Valid @RequestBody ComplaintRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User user = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                Complaint updated = complaintService.updateComplaint(complaintId, user.getUserId(), request);
                return ResponseEntity.ok(ApiResponse.success("Complaint updated successfully", updated));
        }

        private UserResponse buildUserResponse(User user) {
                UserResponse response = new UserResponse();
                response.setUserId(user.getUserId());
                response.setUsername(user.getUsername());
                response.setEmail(user.getEmail());
                response.setName(user.getName());
                response.setPhone(user.getPhone());
                response.setAddress(user.getAddress());
                response.setRole(user.getRole());
                response.setStatus(user.getStatus());
                response.setCreatedAt(user.getCreatedAt());
                response.setUpdatedAt(user.getUpdatedAt());
                return response;
        }
}
