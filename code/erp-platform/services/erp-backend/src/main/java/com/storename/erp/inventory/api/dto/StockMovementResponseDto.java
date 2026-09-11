package com.storename.erp.inventory.api.dto;

import com.storename.erp.inventory.domain.MovementType;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import java.util.UUID;

public record StockMovementResponseDto(
    UUID id, 
    MovementType movementType, 
    BigDecimal quantity,
    String refType, 
    String refId, 
    ZonedDateTime createdAt
) {}
