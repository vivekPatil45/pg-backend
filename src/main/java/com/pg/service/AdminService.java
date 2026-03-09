package com.pg.service;

import com.pg.dto.request.CreateRoomRequest;
import com.pg.dto.request.RoomFilterRequest;
import com.pg.dto.request.UpdateRoomRequest;
import com.pg.dto.response.RoomResponse;

import com.pg.entity.Booking;
import com.pg.entity.Room;
import com.pg.entity.Tenant;
import com.pg.entity.User;
import com.pg.enums.BedStatus;
import com.pg.enums.BookingStatus;
import com.pg.enums.PaymentMethod;
import com.pg.enums.PaymentStatus;

import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;

import com.pg.repository.BedRepository;
import com.pg.repository.BookingRepository;
import com.pg.repository.ComplaintRepository;
import com.pg.repository.RoomRepository;
import com.pg.repository.TenantRepository;
import com.pg.repository.UserRepository;
import com.pg.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final BookingRepository bookingRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final BookingService bookingService;
    private final IdGenerator idGenerator;

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalRooms = roomRepository.count();
        stats.put("totalRooms", totalRooms);

        long occupiedBeds = bedRepository.countByStatus(BedStatus.OCCUPIED);
        long availableBeds = bedRepository.countByStatus(BedStatus.AVAILABLE);
        stats.put("occupiedBeds", occupiedBeds);
        stats.put("availableBeds", availableBeds);

        long pendingBookings = bookingRepository.countByStatus(BookingStatus.PENDING)
                + bookingRepository.countByStatus(BookingStatus.PENDING_PAYMENT);
        stats.put("pendingBookings", pendingBookings);

        long totalTenants = tenantRepository.count();
        stats.put("totalTenants", totalTenants);

        long openComplaints = complaintRepository.countByStatus(com.pg.enums.ComplaintStatus.OPEN)
                + complaintRepository.countByStatus(com.pg.enums.ComplaintStatus.IN_PROGRESS);
        stats.put("openComplaints", openComplaints);

        // Revenue calculations
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        List<BookingStatus> paidStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE,
                BookingStatus.COMPLETED);

        BigDecimal monthlyRevenue = bookingRepository.findAll().stream()
                .filter(b -> paidStatuses.contains(b.getStatus()))
                .filter(b -> b.getCreatedAt() != null && !b.getCreatedAt().toLocalDate().isBefore(startOfMonth)
                        && b.getCreatedAt().toLocalDate().isBefore(startOfNextMonth))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthlyRevenue", monthlyRevenue);

        BigDecimal totalRevenue = bookingRepository.findAll().stream()
                .filter(b -> paidStatuses.contains(b.getStatus()))
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);

        // Booking status breakdown for chart
        Map<String, Long> bookingsByStatus = new HashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            long count = bookingRepository.countByStatus(status);
            if (count > 0) {
                bookingsByStatus.put(status.name(), count);
            }
        }
        stats.put("bookingsByStatus", bookingsByStatus);

        // Recent Bookings (Top 5)
        Pageable topFive = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> recentBookingsPage = bookingRepository.findAll(topFive);
        List<Map<String, Object>> recentBookings = recentBookingsPage.getContent().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getBookingId());
            map.put("tenant", r.getTenant().getUser().getName());
            map.put("email", r.getTenant().getUser().getEmail());
            map.put("amount", r.getTotalAmount());
            map.put("status", r.getStatus());
            map.put("date", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            return map;
        }).collect(java.util.stream.Collectors.toList());
        stats.put("recentBookings", recentBookings);

        return stats;
    }

    @Transactional
    public RoomResponse addRoom(CreateRoomRequest request) {
        String roomNumber = request.getRoomNumber();
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            throw new InvalidRequestException("Room number is required");
        }
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new InvalidRequestException("Room with number " + roomNumber + " already exists");
        }

        // Determine totalBeds: derived purely from roomType
        int beds = request.getRoomType() != null ? switch (request.getRoomType()) {
            case SINGLE_SHARING -> 1;
            case DOUBLE_SHARING -> 2;
            case TRIPLE_SHARING -> 3;
        } : 1;

        Room room = new Room();
        room.setRoomId(idGenerator.generateRoomId());
        room.setRoomNumber(roomNumber);
        room.setRoomType(request.getRoomType());
        room.setPrice(request.getPrice());
        room.setAmenities(request.getAmenities());
        room.setDescription(request.getDescription());
        room.setAvailability(request.getAvailability() != null ? request.getAvailability() : true);
        room.setFloor(request.getFloor());
        room.setRoomSize(request.getRoomSize());
        room.setImages(request.getImages());
        room.setTotalBeds(beds);

        Room savedRoom = roomRepository.save(room);

        // Auto-generate beds
        for (int i = 1; i <= beds; i++) {
            com.pg.entity.Bed bed = new com.pg.entity.Bed();
            bed.setBedId(idGenerator.generateRoomId().replace("RM", "BD")); // Reusing ID generator logic with different
                                                                            // prefix
            bed.setRoom(savedRoom);
            bed.setBedNumber(i);
            bed.setStatus(com.pg.enums.BedStatus.AVAILABLE);
            bedRepository.save(bed);
        }

        return mapToRoomResponse(savedRoom);
    }

    @Transactional
    public RoomResponse updateRoom(String roomId, UpdateRoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Booking> activeBookings = bookingRepository.findByRoomAndStatusIn(
                room, List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE));

        if (!activeBookings.isEmpty()) {
            throw new InvalidRequestException("Cannot update room with active bookings.");
        }

        if (request.getRoomType() != null)
            room.setRoomType(request.getRoomType());
        if (request.getPrice() != null)
            room.setPrice(request.getPrice());
        if (request.getAmenities() != null)
            room.setAmenities(request.getAmenities());
        if (request.getDescription() != null)
            room.setDescription(request.getDescription());
        if (request.getAvailability() != null)
            room.setAvailability(request.getAvailability());
        if (request.getFloor() != null)
            room.setFloor(request.getFloor());
        if (request.getRoomSize() != null)
            room.setRoomSize(request.getRoomSize());
        if (request.getImages() != null)
            room.setImages(request.getImages());
        if (request.getTotalBeds() != null)
            room.setTotalBeds(request.getTotalBeds());

        Room updatedRoom = roomRepository.save(room);
        return mapToRoomResponse(updatedRoom);
    }

    @Transactional
    public void deleteRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Booking> activeBookings = bookingRepository.findByRoomAndStatusIn(
                room, List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE));

        if (!activeBookings.isEmpty()) {
            throw new InvalidRequestException("Cannot delete room with active bookings.");
        }

        roomRepository.delete(room);
    }

    public Page<RoomResponse> getAllRooms(RoomFilterRequest filterRequest, Pageable pageable) {
        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "roomNumber";
        boolean isComputedField = "availableBeds".equals(sortBy) || "currentStatus".equals(sortBy);

        org.springframework.data.jpa.domain.Specification<Room> spec = com.pg.repository.specification.RoomSpecification
                .getFilterSpecification(filterRequest);

        if (isComputedField) {
            // Fetch all matching rooms, map, and sort in memory
            List<Room> allRooms = roomRepository.findAll(spec);
            List<RoomResponse> responses = allRooms.stream()
                    .map(this::mapToRoomResponse)
                    .collect(Collectors.toList());

            boolean isDesc = "desc".equalsIgnoreCase(filterRequest.getSortOrder());
            Comparator<RoomResponse> comparator;
            if ("availableBeds".equals(sortBy)) {
                comparator = Comparator.comparing(RoomResponse::getAvailableBeds);
            } else {
                comparator = (r1, r2) -> {
                    String s1 = r1.getCurrentStatus() != null ? r1.getCurrentStatus() : "";
                    String s2 = r2.getCurrentStatus() != null ? r2.getCurrentStatus() : "";
                    return s1.compareTo(s2);
                };
            }

            if (isDesc) {
                comparator = comparator.reversed();
            }

            responses.sort(comparator);

            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), responses.size());

            List<RoomResponse> pagedResponses = new ArrayList<>();
            if (start <= responses.size()) {
                pagedResponses = responses.subList(start, end);
            }

            return new org.springframework.data.domain.PageImpl<>(pagedResponses, pageable, responses.size());
        }

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(filterRequest.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return roomRepository.findAll(spec, sortedPageable).map(this::mapToRoomResponse);
    }

    public RoomResponse getRoomById(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));
        return mapToRoomResponse(room);
    }

    public Page<RoomResponse> searchRooms(String query, Pageable pageable) {
        return roomRepository.findByRoomNumberContainingIgnoreCaseOrRoomTypeContaining(query, query, pageable)
                .map(this::mapToRoomResponse);
    }

    @Transactional
    public Booking createAdminBooking(String tenantEmail, String roomId, LocalDate moveInDate, String paymentMethod) {
        User user = userRepository.findByEmail(tenantEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with email: " + tenantEmail));

        Tenant tenant = tenantRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    Tenant newTenant = new Tenant();
                    newTenant.setTenantId(idGenerator.generateTenantId());
                    newTenant.setUser(user);
                    newTenant.setTotalBookings(0);
                    return tenantRepository.save(newTenant);
                });

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<Booking> activeBookings = bookingRepository.findByRoomAndStatusIn(
                room, List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE));
        if (!activeBookings.isEmpty()) {
            throw new InvalidRequestException("Room is already occupied.");
        }

        Booking booking = new Booking();
        booking.setBookingId(idGenerator.generateBookingId());
        booking.setTenant(tenant);
        booking.setRoom(room);
        booking.setMoveInDate(moveInDate);
        booking.setTotalAmount(room.getPrice());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        if (paymentMethod != null) {
            booking.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        }

        // Auto-assign available bed in the room
        bedRepository.findFirstByRoomAndStatusOrderByBedNumberAsc(room, BedStatus.AVAILABLE)
                .ifPresent(bed -> {
                    bed.setTenant(tenant);
                    bed.setStatus(BedStatus.OCCUPIED);
                    bedRepository.save(bed);
                });

        return bookingRepository.save(booking);
    }

    @Transactional
    public void cancelAdminBooking(String bookingId) {
        bookingService.cancelBooking(bookingId, "Cancelled by Administrator");
    }

    @Transactional(readOnly = true)
    public Page<Booking> getAllBookings(BookingStatus status, String searchQuery, LocalDate bookingDate,
            Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Booking> spec = com.pg.repository.specification.ReservationSpecification
                .getFilterSpecification(
                        null, null, null, status, searchQuery, bookingDate);
        return bookingRepository.findAll(spec, pageable);
    }

    // Removed generateRoomNumber, using user input now

    public Map<String, Object> bulkImportRooms(org.springframework.web.multipart.MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new java.util.ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                try {
                    // Split by comma, ignoring commas in quotes
                    String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (data.length < 7) {
                        throw new IllegalArgumentException(
                                "Insufficient columns. Expected: RoomNumber, RoomType, Price, Amenities, Floor, RoomSize, Description");
                    }

                    CreateRoomRequest request = new CreateRoomRequest();
                    request.setRoomNumber(data[0].trim());
                    request.setRoomType(com.pg.enums.RoomType.valueOf(data[1].trim().toUpperCase()));
                    request.setPrice(new java.math.BigDecimal(data[2].trim()));

                    // Amenities: Semicolon separated
                    if (data[3] != null && !data[3].trim().isEmpty()) {
                        String amenitiesStr = data[3].replace("\"", "").trim();
                        request.setAmenities(List.of(amenitiesStr.split(";")));
                    } else {
                        request.setAmenities(List.of());
                    }

                    request.setFloor(Integer.parseInt(data[4].trim()));
                    request.setRoomSize(Integer.parseInt(data[5].trim()));
                    request.setDescription(data[6].replace("\"", "").trim());
                    request.setAvailability(data.length > 7 ? Boolean.parseBoolean(data[7].trim()) : true);
                    request.setImages(List.of()); // Default empty images

                    addRoom(request);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (java.io.IOException e) {
            throw new InvalidRequestException("Failed to read CSV file: " + e.getMessage());
        }

        result.put("processed", successCount + failureCount);
        result.put("success", successCount);
        result.put("failure", failureCount);
        result.put("errors", errors);
        return result;
    }

    public byte[] getRoomTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("RoomNumber,RoomType,Price,Amenities,Floor,RoomSize,Description,Availability\n");
        sb.append("101,SINGLE_SHARING,5000,\"Wi-Fi;Air Conditioning\",1,150,\"Spacious single room\",true\n");
        sb.append("102,DOUBLE_SHARING,3500,\"Wi-Fi;TV\",1,200,\"Comfortable double sharing\",true\n");
        sb.append("103,TRIPLE_SHARING,2500,\"Wi-Fi\",2,250,\"Budget triple sharing\",true\n");
        return sb.toString().getBytes();
    }

    private RoomResponse mapToRoomResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getRoomId());
        response.setRoomNumber(room.getRoomNumber());
        response.setRoomType(room.getRoomType());
        response.setPrice(room.getPrice());
        response.setAmenities(room.getAmenities());
        response.setAvailability(room.getAvailability());
        response.setDescription(room.getDescription());
        response.setFloor(room.getFloor());
        response.setRoomSize(room.getRoomSize());
        response.setImages(room.getImages());
        response.setCreatedAt(room.getCreatedAt());
        response.setUpdatedAt(room.getUpdatedAt());

        // PG Bed occupancy logic
        int total = room.getTotalBeds() != null ? room.getTotalBeds() : 1;
        response.setTotalBeds(total);

        long available = bedRepository.countByRoomAndStatus(room, BedStatus.AVAILABLE);
        response.setAvailableBeds((int) available);

        long activeBookingsCount = bookingRepository.findByRoomAndStatusIn(
                room, List.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE)).size();
        boolean hasActive = activeBookingsCount > 0;
        response.setHasActiveReservations(hasActive);
        if (!room.getAvailability()) {
            response.setCurrentStatus("MAINTENANCE");
        } else if (available == 0) {
            response.setCurrentStatus("FULL");
        } else {
            response.setCurrentStatus("AVAILABLE");
        }
        return response;
    }
}
