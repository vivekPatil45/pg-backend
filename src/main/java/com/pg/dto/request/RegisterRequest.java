package com.pg.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = "Password must be at least 8 characters and include a mix of uppercase, lowercase, number, and special character.")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Full name is required")
    @Size(min = 3, message = "Name must be at least 3 characters long and contain only letters.")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name must be at least 3 characters long and contain only letters.")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(\\+91[\\-\\s]?)?[6789]\\d{9}$", message = "Please enter a valid 10-digit Indian mobile number (e.g., 9876543210 or +91-9876543210). Numbers starting with 0 or dummy values like 0000000000 are not allowed.")
    private String phone;

    @NotBlank(message = "Address is required")
    @Size(min = 10, message = "Address must be at least 10 characters long.")
    private String address;

    @NotBlank(message = "Aadhaar Number is required")
    @Pattern(regexp = "^[2-9][0-9]{11}$", message = "Please enter a valid 12-digit Aadhaar number")
    private String idProof;
}
