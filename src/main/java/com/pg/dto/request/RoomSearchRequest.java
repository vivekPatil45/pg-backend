package com.pg.dto.request;

import com.pg.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomSearchRequest {

    private RoomType roomType;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String sortBy = "price";

    private String sortOrder = "asc";
}
