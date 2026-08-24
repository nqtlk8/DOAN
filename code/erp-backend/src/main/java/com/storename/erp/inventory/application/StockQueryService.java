package com.storename.erp.inventory.application;

import com.storename.erp.inventory.application.dto.StockOnHandDto;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockQueryService {
    
    private final StockOnHandRepository stockRepo;
    
    @Transactional(readOnly = true)
    public StockOnHandDto getStockByProductAndBranch(UUID productId, UUID branchId) {
        return stockRepo.findByProductIdAndBranchId(productId, branchId)
            .map(stock -> {
                StockOnHandDto dto = new StockOnHandDto();
                dto.setId(stock.getId());
                dto.setProductId(stock.getProductId());
                dto.setBranchId(stock.getBranchId());
                dto.setQuantity(stock.getQuantity());
                dto.setAvgCost(stock.getAvgCost());
                return dto;
            })
            .orElse(null);
    }
}
