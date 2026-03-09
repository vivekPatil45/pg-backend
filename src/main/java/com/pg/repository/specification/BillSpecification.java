package com.pg.repository.specification;

import com.pg.entity.Bill;
import com.pg.entity.Tenant;
import com.pg.enums.PaymentStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillSpecification {

    public static Specification<Bill> getBillsWithFilters(
            String searchQuery,
            LocalDate dateFrom,
            LocalDate dateTo,
            PaymentStatus paymentStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search Query (Bill ID or Tenant Name / Tenant ID)
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchPattern = "%" + searchQuery.trim().toLowerCase() + "%";

                // Need to join Tenant
                Join<Bill, Tenant> customerJoin = root.join("tenant");

                Predicate billIdMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("billId")), searchPattern);
                Predicate customerNameMatch = criteriaBuilder
                        .like(criteriaBuilder.lower(customerJoin.get("user").get("name")), searchPattern);
                Predicate tenantIdMatch = criteriaBuilder.like(criteriaBuilder.lower(customerJoin.get("tenantId")),
                        searchPattern);

                predicates.add(criteriaBuilder.or(billIdMatch, customerNameMatch, tenantIdMatch));
            }

            // 2. Date Ranges
            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("billDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("billDate"), dateTo));
            }

            // 3. Payment Status
            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), paymentStatus));
            }

            // 4. Amount Range Matches on totalAmount
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }

            // Handle the case where the count query causes issues from ordering or complex
            // selects
            if (query != null && Long.class != query.getResultType()) {
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
