package com.pg.controller;

import com.pg.dto.request.RoomSearchRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.RoomDetailResponse;
import com.pg.entity.Bed;
import com.pg.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchRooms(
            @Valid @RequestBody RoomSearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<RoomDetailResponse> roomsPage = roomService.searchAvailableRooms(request, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("content", roomsPage.getContent());
        data.put("totalResults", roomsPage.getTotalElements());
        data.put("page", roomsPage.getNumber());
        data.put("size", roomsPage.getSize());
        data.put("totalPages", roomsPage.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Rooms retrieved successfully", data));
    }

    @GetMapping("/{roomId}/details")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetails(@PathVariable String roomId) {
        RoomDetailResponse room = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room details retrieved successfully", room));
    }

    @GetMapping("/{roomId}/beds")
    public ResponseEntity<ApiResponse<List<Bed>>> getAvailableBeds(@PathVariable String roomId) {
        List<Bed> beds = roomService.getAvailableBedsForRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Available beds retrieved successfully", beds));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomById(@PathVariable String roomId) {
        RoomDetailResponse room = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(ApiResponse.success("Room retrieved successfully", room));
    }
}
