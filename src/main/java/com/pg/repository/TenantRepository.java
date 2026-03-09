package com.pg.repository;

import com.pg.entity.Tenant;
import com.pg.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
        Optional<Tenant> findByUser_UserId(String userId);

        @Query("SELECT t FROM Tenant t WHERE " +
                        "(:search IS NULL OR LOWER(t.user.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(t.user.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                        "(:status IS NULL OR t.user.status = :status)")
        Page<Tenant> searchTenants(
                        @Param("search") String search,
                        @Param("status") UserStatus status,
                        Pageable pageable);
}
