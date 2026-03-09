package com.pg.repository;

import com.pg.entity.User;
import com.pg.enums.UserRole;
import com.pg.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByRole(UserRole role);

    // Get all users by role (without pagination)
    java.util.List<User> findAllByRole(UserRole role);

    // Pagination and filtering methods
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseAndRole(String username, UserRole role, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseAndStatus(String username, UserStatus status, Pageable pageable);

    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    Page<User> findByUsernameContainingIgnoreCaseAndRoleAndStatus(String username, UserRole role, UserStatus status,
            Pageable pageable);
}
