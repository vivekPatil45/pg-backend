package com.pg.service;

import com.pg.dto.request.LoginRequest;
import com.pg.dto.request.RegisterRequest;
import com.pg.dto.response.ApiResponse;
import com.pg.dto.response.AuthResponse;
import com.pg.entity.Tenant;
import com.pg.entity.User;
import com.pg.enums.UserRole;
import com.pg.enums.UserStatus;
import com.pg.exception.AccountLockedException;
import com.pg.exception.DuplicateResourceException;
import com.pg.exception.InvalidRequestException;
import com.pg.repository.TenantRepository;
import com.pg.repository.UserRepository;
import com.pg.security.JwtUtil;
import com.pg.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final IdGenerator idGenerator;

    @Transactional
    public ApiResponse<Map<String, String>> register(RegisterRequest request) {
        // Validate passwords match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidRequestException("Passwords do not match.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered.");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Mobile number already registered.");
        }

        // Create user
        User user = new User();
        user.setUserId(idGenerator.generateUserId());
        user.setUsername(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setRole(UserRole.TENANT);
        user.setStatus(UserStatus.ACTIVE); // Tenant must be activated by admin
        user.setFailedLoginAttempts(0);
        user.setRequirePasswordChange(false); // Tenant chose their own password

        User savedUser = userRepository.save(user);

        // Create tenant profile
        Tenant tenant = new Tenant();
        tenant.setTenantId(idGenerator.generateTenantId());
        tenant.setUser(savedUser);
        tenant.setTotalBookings(0);

        tenantRepository.save(tenant);

        Map<String, String> data = new HashMap<>();
        data.put("userId", savedUser.getUserId());
        data.put("email", savedUser.getEmail());
        data.put("email", savedUser.getEmail());
        data.put("name", savedUser.getName());
        data.put("message", "Please login with your credentials");

        return ApiResponse.success("Registration successful", data);
    }

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (user.getFailedLoginAttempts() >= 5) {
            throw new AccountLockedException("Your account is locked. Please contact support.");
        }

        // Check if account is active
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InvalidRequestException("Your account is inactive. Please contact administrator for activation.");
        }

        try {
            // Authenticate
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // Reset failed login attempts on successful login
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            // Generate JWT token
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String token = jwtUtil.generateToken(userDetails);

            // Prepare response
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getUserId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getRequirePasswordChange());

            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(token);
            authResponse.setTokenType("Bearer");
            authResponse.setExpiresIn(jwtUtil.getExpirationTime());
            authResponse.setUser(userInfo);

            return ApiResponse.success("Login successful", authResponse);

        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            userRepository.save(user);
            throw e;
        }
    }

    public ApiResponse<Void> logout() {
        // In a stateless JWT system, logout is handled client-side by removing the
        // token
        // Here we just return a success message
        return new ApiResponse<>(true, "Logged out successfully", null);
    }

    /**
     * Create default demo admin user
     * Email: admin@hotel.com
     * Password: admin123
     */
    @Transactional
    public ApiResponse<Map<String, String>> createDemoAdmin() {
        // Check if admin already exists
        if (userRepository.findByEmail("admin@hotel.com").isPresent()) {
            throw new InvalidRequestException("Demo admin user already exists");
        }

        // Create admin user
        User adminUser = new User();
        adminUser.setUserId(idGenerator.generateUserId());
        adminUser.setUsername("admin@hotel.com");
        adminUser.setEmail("admin@hotel.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setName("Demo Admin");
        adminUser.setPhone("+91-8888888888");
        adminUser.setAddress("Hotel Address");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setRequirePasswordChange(false);
        adminUser.setFailedLoginAttempts(0);

        userRepository.save(adminUser);

        Map<String, String> data = new HashMap<>();
        data.put("email", "admin@hotel.com");
        data.put("email", "admin@hotel.com");
        data.put("password", "admin123");
        data.put("message", "Demo admin user created successfully. Please change the password after first login.");

        return ApiResponse.success("Demo admin created successfully", data);
    }
}
