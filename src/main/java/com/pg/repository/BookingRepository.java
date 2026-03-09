package com.pg.repository;

import com.pg.entity.Booking;
import com.pg.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String>, JpaSpecificationExecutor<Booking> {

        List<Booking> findByTenant_TenantId(String tenantId);

        Page<Booking> findByTenant_TenantId(String tenantId, Pageable pageable);

        Page<Booking> findByTenant_TenantIdAndStatus(String tenantId, BookingStatus status, Pageable pageable);

        List<Booking> findByStatus(BookingStatus status);

        Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

        long countByStatus(BookingStatus status);

        Page<Booking> findByStatusAndTenant_User_NameContainingIgnoreCase(BookingStatus status, String name,
                        Pageable pageable);

        Page<Booking> findByTenant_User_NameContainingIgnoreCase(String name, Pageable pageable);

        @Query("SELECT b FROM Booking b WHERE " +
                        "(:status IS NULL OR b.status = :status) AND " +
                        "(:dateFrom IS NULL OR b.moveInDate >= :dateFrom) AND " +
                        "(:roomNumber IS NULL OR b.room.roomNumber = :roomNumber) AND " +
                        "(:tenantName IS NULL OR LOWER(b.tenant.user.name) LIKE LOWER(CONCAT('%', :tenantName, '%')))")
        Page<Booking> searchBookings(
                        @Param("status") BookingStatus status,
                        @Param("dateFrom") LocalDate dateFrom,
                        @Param("roomNumber") String roomNumber,
                        @Param("tenantName") String tenantName,
                        Pageable pageable);

        @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.moveInDate >= :startDate AND b.moveInDate < :endDate")
        long countByStatusAndDateRange(
                        @Param("status") BookingStatus status,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        List<Booking> findByRoomAndStatusIn(com.pg.entity.Room room, List<BookingStatus> statuses);

        @Query("SELECT b FROM Booking b WHERE b.room.roomId = :roomId AND b.status <> 'CANCELLED' AND b.bookingId <> :excludeBookingId")
        List<Booking> findActiveBookingsForRoomExcluding(
                        @Param("roomId") String roomId,
                        @Param("excludeBookingId") String excludeBookingId);

        java.util.Optional<Booking> findFirstByTenant_TenantIdAndStatusInOrderByCreatedAtDesc(String tenantId,
                        List<BookingStatus> statuses);
}
