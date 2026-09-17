package com.storename.erp.inventory.application;

import com.storename.erp.branch.domain.Branch;
import com.storename.erp.branch.infrastructure.BranchRepository;
import com.storename.erp.catalog.domain.Product;
import com.storename.erp.catalog.infrastructure.ProductRepository;
import com.storename.erp.inventory.application.dto.StockOnHandResponseDto;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockOnHandRepository stockRepo;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<StockOnHandResponseDto> getStockByBranch(Long branchId) {
        List<StockOnHand> stocks;
        if (branchId == null) {
            stocks = stockRepo.findAll();
        } else {
            stocks = stockRepo.findByBranchId(branchId);
        }

        List<Long> productIds = stocks.stream().map(StockOnHand::getProductId).distinct().toList();
        List<Long> branchIds = stocks.stream().map(StockOnHand::getBranchId).distinct().toList();

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Branch> branchMap = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, b -> b));

        return stocks.stream().map(stock -> {
            Product product = productMap.get(stock.getProductId());
            Branch branch = branchMap.get(stock.getBranchId());

            return StockOnHandResponseDto.builder()
                    .id(stock.getId())
                    .productId(stock.getProductId())
                    .productCode(product != null ? product.getCode() : null)
                    .productName(product != null ? product.getName() : null)
                    .branchId(stock.getBranchId())
                    .branchName(branch != null ? branch.getName() : null)
                    .quantity(stock.getQuantity())
                    .avgCost(null) // Cost is handled by CostLayer, we might just return null or calculate it
                    .build();
        }).collect(Collectors.toList());
    }
}
