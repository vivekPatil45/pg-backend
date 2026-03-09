package com.pg.service;

import com.pg.dto.response.TenantResponse;
import com.pg.entity.Bed;
import com.pg.entity.Tenant;
import com.pg.enums.BedStatus;
import com.pg.enums.UserStatus;
import com.pg.repository.BedRepository;
import com.pg.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminTenantService {

    private final TenantRepository tenantRepository;
    private final BedRepository bedRepository;

    public Page<TenantResponse> getAllTenants(String search, UserStatus status, Pageable pageable) {
        Page<Tenant> tenants = tenantRepository.searchTenants(search, status, pageable);
        return tenants.map(this::mapToTenantResponse);
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        // Find if tenant has an active bed
        Optional<Bed> activeBed = bedRepository.findByTenant_TenantId(tenant.getTenantId());

        return TenantResponse.builder()
                .tenantId(tenant.getTenantId())
                .userId(tenant.getUser().getUserId())
                .name(tenant.getUser().getName())
                .email(tenant.getUser().getEmail())
                .phone(tenant.getUser().getPhone())
                .roomNumber(activeBed.map(bed -> bed.getRoom().getRoomNumber()).orElse(null))
                .bedNumber(activeBed.map(Bed::getBedNumber).orElse(null))
                .idProof(tenant.getIdProof())
                .checkInDate(tenant.getCheckInDate())
                .preferredRoomType(tenant.getPreferredRoomType())
                .status(tenant.getUser().getStatus())
                .build();
    }

    @Transactional
    public TenantResponse updateTenant(String tenantId, TenantResponse updateData) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (updateData.getIdProof() != null)
            tenant.setIdProof(updateData.getIdProof());
        if (updateData.getCheckInDate() != null)
            tenant.setCheckInDate(updateData.getCheckInDate());

        // Update user fields if provided
        if (updateData.getName() != null)
            tenant.getUser().setName(updateData.getName());
        if (updateData.getPhone() != null)
            tenant.getUser().setPhone(updateData.getPhone());
        if (updateData.getStatus() != null)
            tenant.getUser().setStatus(updateData.getStatus());

        return mapToTenantResponse(tenantRepository.save(tenant));
    }

    @Transactional
    public void checkoutTenant(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Release the bed if any
        Optional<Bed> bed = bedRepository.findByTenant_TenantId(tenantId);
        if (bed.isPresent()) {
            Bed b = bed.get();
            b.setTenant(null);
            b.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(b);
        }

        // Deactivate user or mark as checked out? For now, let's keep it simple.
        // Maybe mark as INACTIVE or similar.
        tenant.getUser().setStatus(UserStatus.INACTIVE);
        tenantRepository.save(tenant);
    }
}
