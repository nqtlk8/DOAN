package com.storename.erp.inventory.application;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.inventory.application.dto.StockOnHandDto;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockQueryService {
    
    private final StockOnHandRepository stockRepo;
    private final com.storename.erp.inventory.infrastructure.StockMovementRepository movementRepo;
    
    @Transactional(readOnly = true)
    public StockOnHandDto getStockByProductAndBranch(Long productId, Long branchId) {
        return stockRepo.findByProductIdAndBranchId(productId, branchId)
            .map(stock -> {
                StockOnHandDto dto = new StockOnHandDto();
                dto.setId(stock.getId());
                dto.setProductId(stock.getProductId());
                dto.setBranchId(stock.getBranchId());
                dto.setQuantity(stock.getQuantity());
                return dto;
            })
            .orElse(null);
    }

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
