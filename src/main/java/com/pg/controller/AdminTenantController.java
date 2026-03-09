package com.pg.controller;

import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.TenantResponse;
import com.pg.enums.UserStatus;
import com.pg.service.AdminTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> getAllTenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TenantResponse> tenants = adminTenantService.getAllTenants(search, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tenants retrieved successfully", tenants));
    }

    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable String tenantId,
            @RequestBody TenantResponse updateData) {
        TenantResponse updated = adminTenantService.updateTenant(tenantId, updateData);
        return ResponseEntity.ok(ApiResponse.success("Tenant updated successfully", updated));
    }

    @PostMapping("/{tenantId}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkoutTenant(@PathVariable String tenantId) {
        adminTenantService.checkoutTenant(tenantId);
        return ResponseEntity.ok(ApiResponse.success("Tenant checked out and bed released.", null));
    }
}
