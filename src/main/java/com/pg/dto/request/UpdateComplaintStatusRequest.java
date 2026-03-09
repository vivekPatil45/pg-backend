package com.pg.dto.request;

import com.pg.enums.ComplaintStatus;
import lombok.Data;

@Data
public class UpdateComplaintStatusRequest {
    private ComplaintStatus status;
    private String notes; // Optional notes
}
