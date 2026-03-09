package com.pg.service;

import com.pg.dto.request.ChangePasswordRequest;
import com.pg.dto.request.CreateUserRequest;
import com.pg.dto.request.UpdateUserRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.CreateUserResponse;
import com.pg.dto.response.UserListResponse;
import com.pg.dto.response.UserResponse;
import com.pg.entity.Tenant;
import com.pg.entity.User;
import com.pg.enums.UserRole;
import com.pg.enums.UserStatus;
import com.pg.exception.DuplicateResourceException;
import com.pg.exception.InvalidRequestException;
import com.pg.exception.ResourceNotFoundException;
import com.pg.repository.TenantRepository;
import com.pg.repository.UserRepository;
import com.pg.util.BeanUtil;
import com.pg.util.IdGenerator;
import com.pg.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    /**
     * Creates a new user account with auto-generated password.
     * Admin-created users are ACTIVE by default and must change password on first
     * login.
     */
    @Transactional
    public ApiResponse<CreateUserResponse> createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Mobile number already registered");
        }

        // Generate secure password
        String generatedPassword = PasswordUtils.generateSecurePassword();

        // Create user
        User user = new User();
        user.setUserId(idGenerator.generateUserId());
        user.setUsername(request.getEmail());
        user.setPassword(passwordEncoder.encode(generatedPassword));
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole());
        user.setStatus(UserStatus.ACTIVE); // Admin-created users are active
        user.setRequirePasswordChange(true); // Must change password on first login
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);

        // Create tenant profile if role is TENANT (User)
        if (savedUser.getRole() == UserRole.TENANT) {
            Tenant tenant = new Tenant();
            tenant.setTenantId(idGenerator.generateTenantId());
            tenant.setUser(savedUser);
            tenant.setTotalBookings(0);
            tenantRepository.save(tenant);
        }

        // Prepare response
        CreateUserResponse response = new CreateUserResponse();
        response.setUserId(savedUser.getUserId());
        response.setUsername(savedUser.getEmail());
        response.setEmail(savedUser.getEmail());
        response.setName(savedUser.getName());
        response.setPhone(savedUser.getPhone());
        response.setAddress(savedUser.getAddress());
        response.setRole(savedUser.getRole());
        response.setStatus(savedUser.getStatus());
        response.setCreatedAt(savedUser.getCreatedAt());
        response.setUpdatedAt(savedUser.getUpdatedAt());
        response.setGeneratedPassword(generatedPassword);

        return ApiResponse.success("User created successfully. Please share the generated password with the user.",
                response);
    }

    /**
     * Retrieves a user by ID.
     */
    public ApiResponse<UserResponse> getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return ApiResponse.success("User retrieved successfully", convertToUserResponse(user));
    }

    /**
     * Retrieves all users with pagination, search, filter, and sort capabilities.
     */
    public ApiResponse<UserListResponse> getAllUsers(
            int page,
            int size,
            String username,
            String role,
            String status,
            String sortBy,
            String sortDir) {

        // Create sort
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Build query based on filters
        Page<User> userPage;

        UserRole userRole = (role != null && !role.isEmpty()) ? UserRole.valueOf(role.toUpperCase()) : null;
        UserStatus userStatus = (status != null && !status.isEmpty()) ? UserStatus.valueOf(status.toUpperCase()) : null;
        boolean hasUsername = username != null && !username.isEmpty();

        if (hasUsername && userRole != null && userStatus != null) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseAndRoleAndStatus(username, userRole, userStatus,
                    pageable);
        } else if (hasUsername && userRole != null) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseAndRole(username, userRole, pageable);
        } else if (hasUsername && userStatus != null) {
            userPage = userRepository.findByUsernameContainingIgnoreCaseAndStatus(username, userStatus, pageable);
        } else if (userRole != null && userStatus != null) {
            userPage = userRepository.findByRoleAndStatus(userRole, userStatus, pageable);
        } else if (hasUsername) {
            userPage = userRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (userRole != null) {
            userPage = userRepository.findByRole(userRole, pageable);
        } else if (userStatus != null) {
            userPage = userRepository.findByStatus(userStatus, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // Convert to response
        List<UserResponse> users = userPage.getContent().stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());

        UserListResponse response = new UserListResponse(
                users,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber(),
                userPage.getSize());

        return ApiResponse.success("Users retrieved successfully", response);
    }

    /**
     * Updates a user account.
     */
    @Transactional
    public ApiResponse<UserResponse> updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Email already registered");
            }
            user.setEmail(request.getEmail());
            user.setUsername(request.getEmail());
        }

        // Update mobile number if provided and different
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("Mobile number already registered");
            }
            user.setPhone(request.getPhone());
        }

        // Update other fields if provided
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        User updatedUser = userRepository.save(user);

        return ApiResponse.success("User updated successfully", convertToUserResponse(updatedUser));
    }

    /**
     * Resets a user's password and requires them to change it on first login.
     */
    @Transactional
    public ApiResponse<Map<String, String>> resetPassword(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Generate new password
        String newPassword = PasswordUtils.generateSecurePassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setRequirePasswordChange(true);

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());
        response.put("newPassword", newPassword);
        response.put("message", "Password reset successfully. Please share the new password with the user.");

        return ApiResponse.success("Password reset successfully", response);
    }

    /**
     * Deactivates a user account.
     */
    @Transactional
    public ApiResponse<UserResponse> deactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InvalidRequestException("User is already inactive");
        }

        user.setStatus(UserStatus.INACTIVE);
        User updatedUser = userRepository.save(user);

        return ApiResponse.success("User deactivated successfully", convertToUserResponse(updatedUser));
    }

    /**
     * Activates a user account.
     */
    @Transactional
    public ApiResponse<UserResponse> activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new InvalidRequestException("User is already active");
        }

        user.setStatus(UserStatus.ACTIVE);
        User updatedUser = userRepository.save(user);

        return ApiResponse.success("User activated successfully", convertToUserResponse(updatedUser));
    }

    /**
     * Allows a user to change their own password.
     */
    @Transactional
    public ApiResponse<Void> changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidRequestException("Current password is incorrect");
        }

        // Validate new passwords match
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new InvalidRequestException("New passwords do not match");
        }

        // Validate new password meets policy
        if (!PasswordUtils.isValidPassword(request.getNewPassword())) {
            throw new InvalidRequestException("New password does not meet security requirements");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequirePasswordChange(false); // Password changed successfully
        userRepository.save(user);

        return ApiResponse.success("Password changed successfully", null);
    }

    /**
     * Converts User entity to UserResponse DTO.
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setRequirePasswordChange(user.getRequirePasswordChange());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
