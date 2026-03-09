package com.pg.controller;

import com.pg.dto.request.ChangePasswordRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final com.pg.repository.UserRepository userRepository;
    private final UserManagementService userManagementService;

    /**
     * Change password (authenticated users)
     * Users can change their own password, especially required after first login
     * with auto-generated password
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        // Get username from authentication
        String username = authentication.getName();

        // Find user to get the correct UserId
        com.pg.entity.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.pg.exception.ResourceNotFoundException("User not found"));

        ApiResponse<Void> response = userManagementService.changePassword(user.getUserId(), request);
        return ResponseEntity.ok(response);
    }
}
