package com.pg.repository.specification;

import com.pg.entity.Booking;
import com.pg.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationSpecification {

    public static Specification<Booking> getFilterSpecification(
            LocalDate startDate,
            LocalDate endDate,
            String roomType,
            BookingStatus status,
            String searchQuery,
            LocalDate bookingDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                Predicate checkInPredicate = criteriaBuilder.between(root.get("moveInDate"), startDate, endDate);
                Predicate checkOutPredicate = criteriaBuilder.between(root.get("moveOutDate"), startDate, endDate);
                Predicate overlapPredicate = criteriaBuilder.and(
                        criteriaBuilder.lessThanOrEqualTo(root.get("moveInDate"), endDate),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("moveOutDate"), startDate));
                predicates.add(criteriaBuilder.or(checkInPredicate, checkOutPredicate, overlapPredicate));
            }

            if (StringUtils.hasText(roomType)) {
                predicates.add(criteriaBuilder.equal(
                        root.get("room").get("roomType"),
                        com.pg.enums.RoomType.valueOf(roomType.toUpperCase())));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (bookingDate != null) {
                java.time.LocalDateTime startOfDay = bookingDate.atStartOfDay();
                java.time.LocalDateTime endOfDay = bookingDate.plusDays(1).atStartOfDay();
                predicates.add(criteriaBuilder.between(root.get("createdAt"), startOfDay, endOfDay));
            }

            if (StringUtils.hasText(searchQuery)) {
                String searchPattern = "%" + searchQuery.toLowerCase() + "%";
                Predicate byReservationId = criteriaBuilder.like(criteriaBuilder.lower(root.get("bookingId")),
                        searchPattern);
                Predicate byCustomerName = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("tenant").get("user").get("name")), searchPattern);
                Predicate byCustomerEmail = criteriaBuilder
                        .like(criteriaBuilder.lower(root.get("tenant").get("user").get("email")), searchPattern);
                Predicate byRoomNumber = criteriaBuilder.like(criteriaBuilder.lower(root.get("room").get("roomNumber")),
                        searchPattern);

                predicates.add(criteriaBuilder.or(byReservationId, byCustomerName, byCustomerEmail, byRoomNumber));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
