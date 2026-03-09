package com.pg.controller;

import com.pg.dto.response.ApiResponse;
import com.pg.entity.Bed;
import com.pg.enums.BedStatus;
import com.pg.service.BedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/beds")
@RequiredArgsConstructor
public class AdminBedController {

    private final BedService bedService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<Bed>>> getBedsByRoom(@PathVariable String roomId) {
        List<Bed> beds = bedService.getBedsByRoom(roomId);
        return ResponseEntity.ok(ApiResponse.success("Beds retrieved successfully", beds));
    }

    @PutMapping("/{bedId}/assign")
    public ResponseEntity<ApiResponse<Bed>> assignTenantToBed(
            @PathVariable String bedId,
            @RequestBody Map<String, String> payload) {
        String tenantId = payload.get("tenantId");
        Bed bed = bedService.assignTenantToBed(bedId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant assigned successfully", bed));
    }

    @PutMapping("/{bedId}/available")
    public ResponseEntity<ApiResponse<Bed>> markBedAvailable(@PathVariable String bedId) {
        Bed bed = bedService.markBedAvailable(bedId);
        return ResponseEntity.ok(ApiResponse.success("Bed marked available", bed));
    }

    @PutMapping("/{bedId}/status")
    public ResponseEntity<ApiResponse<Bed>> updateBedStatus(
            @PathVariable String bedId,
            @RequestBody Map<String, String> payload) {
        String statusStr = payload.get("status");
        BedStatus status = BedStatus.valueOf(statusStr.toUpperCase());
        Bed bed = bedService.updateBedStatus(bedId, status);
        return ResponseEntity.ok(ApiResponse.success("Bed status updated", bed));
    }
}
