package com.pg.service;

import com.pg.dto.request.RoomSearchRequest;
import com.pg.dto.response.RoomDetailResponse;
import com.pg.entity.Room;
import com.pg.enums.BedStatus;
import com.pg.repository.BedRepository;
import com.pg.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    public Page<RoomDetailResponse> searchAvailableRooms(RoomSearchRequest request, int page, int size) {
        Sort sort = Sort.by(
                "asc".equalsIgnoreCase(request.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC,
                request.getSortBy() != null ? request.getSortBy() : "price");

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Room> roomPage = roomRepository.searchAvailableRooms(
                request.getRoomType(),
                request.getMinPrice(),
                request.getMaxPrice(),
                pageable);

        List<RoomDetailResponse> responseList = roomPage.getContent().stream()
                .map(this::toRoomDetailResponse)
                .filter(r -> r.getAvailableBeds() > 0) // only show rooms with free beds
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        return new PageImpl<>(responseList, pageable, roomPage.getTotalElements());
    }

    public RoomDetailResponse getRoomDetails(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new com.pg.exception.ResourceNotFoundException("Room not found with id: " + roomId));
        return toRoomDetailResponse(room);
    }

    public Room getRoomById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new com.pg.exception.ResourceNotFoundException("Room not found with id: " + roomId));
    }

    public java.util.List<com.pg.entity.Bed> getAvailableBedsForRoom(String roomId) {
        Room room = getRoomById(roomId);
        return bedRepository.findByRoomAndStatusOrderByBedNumberAsc(room, BedStatus.AVAILABLE);
    }

    private RoomDetailResponse toRoomDetailResponse(Room room) {
        long available = bedRepository.countByRoomAndStatus(room, BedStatus.AVAILABLE);
        return RoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .price(room.getPrice())
                .totalBeds(room.getTotalBeds())
                .availableBeds((int) available)
                .availability(room.getAvailability())
                .description(room.getDescription())
                .floor(room.getFloor())
                .roomSize(room.getRoomSize())
                .amenities(room.getAmenities())
                .images(room.getImages())
                .build();
    }
}
