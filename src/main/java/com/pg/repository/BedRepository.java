package com.pg.repository;

import com.pg.entity.Bed;
import com.pg.entity.Room;
import com.pg.enums.BedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, String> {
    List<Bed> findByRoomOrderByBedNumberAsc(Room room);

    List<Bed> findByRoomAndStatusOrderByBedNumberAsc(Room room, BedStatus status);

    Optional<Bed> findFirstByRoomAndStatusOrderByBedNumberAsc(Room room, BedStatus status);

    long countByRoomAndStatus(Room room, BedStatus status);

    long countByStatus(BedStatus status);

    void deleteByRoom(Room room);

    Optional<Bed> findByTenant_TenantId(String tenantId);
}
