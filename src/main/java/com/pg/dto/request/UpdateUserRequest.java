package com.pg.dto.request;

import com.pg.enums.UserRole;
import com.pg.enums.UserStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Enter a valid email address")
    private String email;

    @Size(min = 3, max = 100, message = "Full name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must contain only letters and spaces")
    private String name;

    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[6789]\\d{9}$", message = "Please enter a valid 10-digit Indian mobile number (e.g., 9876543210 or +91-9876543210). Numbers starting with 0 or dummy values like 0000000000 are not allowed.")
    private String phone;

    @Size(min = 10, max = 200, message = "Address must be between 10 and 200 characters")
    private String address;

    private UserRole role;

    private UserStatus status;
}
