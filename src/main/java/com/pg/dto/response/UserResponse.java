package com.pg.dto.response;

import com.pg.enums.UserRole;
import com.pg.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String userId;
    private String username;
    private String email;
    private String name;
    private String phone;
    private String address;
    private UserRole role;
    private UserStatus status;
    private Boolean requirePasswordChange;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
