package com.storename.erp.inventory.application;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class StockQueryService {
    
    private final StockOnHandRepository stockRepo;
    private final com.storename.erp.inventory.infrastructure.StockMovementRepository movementRepo;
    
    @Transactional(readOnly = true)
    public java.util.List<com.storename.erp.inventory.api.dto.StockMovementResponseDto> getStockMovements(Long productId, Long branchId) {
        return movementRepo.findByProductIdAndBranchIdOrderByCreatedAtAsc(productId, branchId)
            .stream()
            .map(m -> new com.storename.erp.inventory.api.dto.StockMovementResponseDto(
                m.getId(),
                m.getMovementType(),
                m.getQuantity(),
                m.getRefType(),
                m.getRefId(),
                m.getCreatedAt()
            ))
            .collect(java.util.stream.Collectors.toList());
    }
}
