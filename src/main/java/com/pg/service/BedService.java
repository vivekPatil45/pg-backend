package com.pg.service;

import com.pg.entity.Bed;
import com.pg.entity.Room;
import com.pg.entity.Tenant;
import com.pg.enums.BedStatus;
import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.BedRepository;
import com.pg.repository.RoomRepository;
import com.pg.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BedService {

    private final BedRepository bedRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;

    public List<Bed> getBedsByRoom(String roomId) {
        if (roomId == null)
            throw new IllegalArgumentException("Room ID must not be null");
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        return bedRepository.findByRoomOrderByBedNumberAsc(room);
    }

    @Transactional
    public Bed assignTenantToBed(String bedId, String tenantId) {
        if (bedId == null || tenantId == null)
            throw new IllegalArgumentException("IDs must not be null");
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        if (bed.getStatus() == BedStatus.OCCUPIED) {
            throw new InvalidRequestException("Bed is already occupied.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Check if tenant already has a bed in THIS room
        List<Bed> roomBeds = bedRepository.findByRoomOrderByBedNumberAsc(bed.getRoom());
        boolean hasBed = roomBeds.stream()
                .anyMatch(b -> b.getTenant() != null && b.getTenant().getTenantId().equals(tenantId));
        if (hasBed) {
            throw new InvalidRequestException("Tenant is already assigned to a bed in this room.");
        }

        bed.setTenant(tenant);
        bed.setStatus(BedStatus.OCCUPIED);
        Bed savedBed = bedRepository.save(bed);
        syncRoomAvailability(savedBed.getRoom());
        return savedBed;
    }

    @Transactional
    public Bed markBedAvailable(String bedId) {
        if (bedId == null)
            throw new IllegalArgumentException("Bed ID must not be null");
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        bed.setTenant(null);
        bed.setStatus(BedStatus.AVAILABLE);
        Bed savedBed = bedRepository.save(bed);
        syncRoomAvailability(savedBed.getRoom());
        return savedBed;
    }

    @Transactional
    public Bed updateBedStatus(String bedId, BedStatus status) {
        if (bedId == null)
            throw new IllegalArgumentException("Bed ID must not be null");
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found"));

        if (status == BedStatus.AVAILABLE) {
            bed.setTenant(null);
        }
        bed.setStatus(status);
        Bed savedBed = bedRepository.save(bed);
        syncRoomAvailability(savedBed.getRoom());
        return savedBed;
    }

    public void syncRoomAvailability(Room room) {
        long availableBeds = bedRepository.countByRoomAndStatus(room, BedStatus.AVAILABLE);
        boolean shouldBeAvailable = availableBeds > 0;
        if (room.getAvailability() != shouldBeAvailable) {
            room.setAvailability(shouldBeAvailable);
            roomRepository.save(room);
        }
    }
}
