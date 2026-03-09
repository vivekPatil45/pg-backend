package com.pg.dto.response;

import com.pg.enums.RoomType;
import com.pg.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    private String tenantId;
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String roomNumber;
    private Integer bedNumber;
    private String idProof;
    private LocalDateTime checkInDate;
    private RoomType preferredRoomType;
    private UserStatus status;
}
