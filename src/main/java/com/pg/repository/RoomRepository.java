package com.pg.repository;

import com.pg.entity.Room;
import com.pg.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String>, JpaSpecificationExecutor<Room> {
        boolean existsByRoomNumber(String roomNumber);

        List<Room> findByAvailability(Boolean availability);

        List<Room> findByRoomType(RoomType roomType);

        @Query("SELECT r FROM Room r WHERE r.availability = true AND " +
                        "(:roomType IS NULL OR r.roomType = :roomType) AND " +
                        "(:minPrice IS NULL OR r.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR r.price <= :maxPrice) AND " +
                        "r.roomId NOT IN (" +
                        "  SELECT b.room.roomId FROM Booking b WHERE " +
                        "  b.status IN ('CONFIRMED', 'ACTIVE', 'PENDING')" +
                        ")")
        Page<Room> searchAvailableRooms(
                        @Param("roomType") RoomType roomType,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        Pageable pageable);

        Page<Room> findByRoomNumberContainingIgnoreCaseOrRoomTypeContaining(
                        String roomNumber, String roomType, Pageable pageable);
}
