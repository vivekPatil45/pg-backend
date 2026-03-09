package com.pg.controller;

import com.pg.dto.request.AddResponseRequest;
import com.pg.dto.request.AssignComplaintRequest;
import com.pg.dto.request.CreateRoomRequest;
import com.pg.dto.request.ResolveComplaintRequest;
import com.pg.dto.request.RoomFilterRequest;
import com.pg.dto.request.UpdateComplaintStatusRequest;
import com.pg.dto.request.UpdateRoomRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.RoomResponse;
import com.pg.entity.Complaint;
import com.pg.entity.User;
import com.pg.enums.ComplaintCategory;
import com.pg.enums.ComplaintPriority;
import com.pg.enums.ComplaintStatus;
import com.pg.repository.UserRepository;
import com.pg.service.AdminService;
import com.pg.service.BillService;
import com.pg.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

        private final AdminService adminService;
        private final ComplaintService complaintService;
        private final BillService billService;
        private final UserRepository userRepository;

        @GetMapping("/dashboard")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
                Map<String, Object> data = adminService.getDashboardStatistics();
                return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", data));
        }

        @GetMapping("/rooms")
        public ResponseEntity<ApiResponse<Map<String, Object>>> getAllRooms(
                        @RequestParam(required = false) String roomType,
                        @RequestParam(required = false) String minPrice,
                        @RequestParam(required = false) String maxPrice,
                        @RequestParam(required = false) Boolean availability,
                        @RequestParam(required = false) List<String> amenities,
                        @RequestParam(required = false) String availabilityDate,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false) String sortOrder,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                RoomFilterRequest filterRequest = new RoomFilterRequest();
                if (roomType != null && !roomType.isEmpty())
                        filterRequest.setRoomType(com.pg.enums.RoomType.valueOf(roomType));
                if (minPrice != null && !minPrice.isEmpty())
                        filterRequest.setMinPrice(new java.math.BigDecimal(minPrice));
                if (maxPrice != null && !maxPrice.isEmpty())
                        filterRequest.setMaxPrice(new java.math.BigDecimal(maxPrice));
                if (availability != null)
                        filterRequest.setAvailability(availability);
                if (amenities != null && !amenities.isEmpty())
                        filterRequest.setAmenities(amenities);
                if (availabilityDate != null && !availabilityDate.isEmpty())
                        filterRequest.setAvailabilityDate(LocalDate.parse(availabilityDate));
                if (q != null && !q.isEmpty())
                        filterRequest.setSearchQuery(q);
                if (sortBy != null && !sortBy.isEmpty())
                        filterRequest.setSortBy(sortBy);
                if (sortOrder != null && !sortOrder.isEmpty())
                        filterRequest.setSortOrder(sortOrder);

                Pageable pageable = PageRequest.of(page, size);
                Page<RoomResponse> roomsPage = adminService.getAllRooms(filterRequest, pageable);

                Map<String, Object> data = new HashMap<>();
                data.put("content", roomsPage.getContent());
                data.put("page", roomsPage.getNumber());
                data.put("size", roomsPage.getSize());
                data.put("totalElements", roomsPage.getTotalElements());
                data.put("totalPages", roomsPage.getTotalPages());

                return ResponseEntity.ok(ApiResponse.success("Rooms retrieved successfully", data));
        }

        @GetMapping("/rooms/{roomId}")
        public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable String roomId) {
                RoomResponse room = adminService.getRoomById(roomId);
                return ResponseEntity.ok(ApiResponse.success("Room retrieved successfully", room));
        }

        @GetMapping("/rooms/search")
        public ResponseEntity<ApiResponse<Map<String, Object>>> searchRooms(
                        @RequestParam String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<RoomResponse> roomsPage = adminService.searchRooms(q, pageable);

                Map<String, Object> data = new HashMap<>();
                data.put("content", roomsPage.getContent());
                data.put("page", roomsPage.getNumber());
                data.put("size", roomsPage.getSize());
                data.put("totalElements", roomsPage.getTotalElements());
                data.put("totalPages", roomsPage.getTotalPages());

                return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", data));
        }

        @PostMapping("/rooms")
        public ResponseEntity<ApiResponse<RoomResponse>> addRoom(@Valid @RequestBody CreateRoomRequest request) {
                RoomResponse room = adminService.addRoom(request);
                return new ResponseEntity<>(
                                ApiResponse.success(String.format("Room %s added successfully", room.getRoomNumber()),
                                                room),
                                HttpStatus.CREATED);
        }

        @PutMapping("/rooms/{roomId}")
        public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
                        @PathVariable String roomId, @Valid @RequestBody UpdateRoomRequest request) {
                RoomResponse room = adminService.updateRoom(roomId, request);
                return ResponseEntity.ok(
                                ApiResponse.success(String.format("Room %s updated successfully", room.getRoomNumber()),
                                                room));
        }

        @DeleteMapping("/rooms/{roomId}")
        public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable String roomId) {
                adminService.deleteRoom(roomId);
                return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
        }

        @PostMapping("/rooms/bulk-import")
        public ResponseEntity<ApiResponse<Map<String, Object>>> bulkImportRooms(
                        @RequestParam("file") MultipartFile file) {
                Map<String, Object> result = adminService.bulkImportRooms(file);
                return ResponseEntity.ok(ApiResponse.success("Bulk import completed", result));
        }

        @GetMapping("/rooms/template")
        public ResponseEntity<byte[]> downloadTemplate() {
                byte[] template = adminService.getRoomTemplate();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rooms_template.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(template);
        }

        // ============ COMPLAINT MANAGEMENT ============

        @GetMapping("/staff")
        public ResponseEntity<ApiResponse<List<User>>> getAllStaff() {
                List<User> staffUsers = userRepository.findAllByRole(com.pg.enums.UserRole.STAFF);
                return ResponseEntity.ok(ApiResponse.success("Staff users retrieved successfully", staffUsers));
        }

        @GetMapping("/complaints")
        public ResponseEntity<ApiResponse<List<Complaint>>> getAllComplaints(
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) String priority,
                        @RequestParam(required = false) String dateFrom,
                        @RequestParam(required = false) String dateTo,
                        @RequestParam(required = false) String search) {

                ComplaintStatus statusEnum = (status != null && !status.isEmpty()) ? ComplaintStatus.valueOf(status)
                                : null;
                ComplaintCategory categoryEnum = (category != null && !category.isEmpty())
                                ? ComplaintCategory.valueOf(category)
                                : null;
                ComplaintPriority priorityEnum = (priority != null && !priority.isEmpty())
                                ? ComplaintPriority.valueOf(priority)
                                : null;
                LocalDate dateFromParsed = (dateFrom != null && !dateFrom.isEmpty()) ? LocalDate.parse(dateFrom) : null;
                LocalDate dateToParsed = (dateTo != null && !dateTo.isEmpty()) ? LocalDate.parse(dateTo) : null;

                List<Complaint> complaints = complaintService.searchComplaints(
                                statusEnum, categoryEnum, priorityEnum, dateFromParsed, dateToParsed, search);

                return ResponseEntity.ok(ApiResponse.success("Complaints retrieved successfully", complaints));
        }

        @GetMapping("/complaints/{complaintId}")
        public ResponseEntity<ApiResponse<Complaint>> getComplaintById(@PathVariable String complaintId) {
                Complaint complaint = complaintService.getComplaintByIdAdmin(complaintId);
                return ResponseEntity.ok(ApiResponse.success("Complaint retrieved successfully", complaint));
        }

        @PutMapping("/complaints/{complaintId}/assign")
        public ResponseEntity<ApiResponse<Complaint>> assignComplaint(
                        @PathVariable String complaintId,
                        @RequestBody AssignComplaintRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User admin = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.assignComplaint(complaintId, request.getAssignedTo(),
                                admin.getUserId());
                return ResponseEntity.ok(ApiResponse.success("Complaint assigned successfully", complaint));
        }

        @PutMapping("/complaints/{complaintId}/status")
        public ResponseEntity<ApiResponse<Complaint>> updateComplaintStatus(
                        @PathVariable String complaintId,
                        @RequestBody UpdateComplaintStatusRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User admin = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.updateStatusByAdmin(
                                complaintId, request.getStatus(), request.getNotes(), admin.getUserId());
                return ResponseEntity.ok(ApiResponse.success("Complaint status updated successfully", complaint));
        }

        @PostMapping("/complaints/{complaintId}/response")
        public ResponseEntity<ApiResponse<Complaint>> addComplaintResponse(
                        @PathVariable String complaintId,
                        @RequestBody AddResponseRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User admin = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.addAdminResponse(
                                complaintId, request.getAction(), request.getNotes(), admin.getUserId());
                return ResponseEntity.ok(ApiResponse.success("Response added successfully", complaint));
        }

        @PutMapping("/complaints/{complaintId}/resolve")
        public ResponseEntity<ApiResponse<Complaint>> resolveComplaint(
                        @PathVariable String complaintId,
                        @RequestBody ResolveComplaintRequest request,
                        Authentication authentication) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User admin = userRepository.findByUsername(userDetails.getUsername())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Admin user not found"));

                Complaint complaint = complaintService.resolveComplaint(
                                complaintId, request.getResolutionNotes(), admin.getUserId());
                return ResponseEntity.ok(ApiResponse.success("Complaint resolved successfully", complaint));
        }
}
