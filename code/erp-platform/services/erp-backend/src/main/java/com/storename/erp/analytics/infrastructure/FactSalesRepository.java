package com.storename.erp.analytics.infrastructure;

import com.storename.erp.analytics.api.dto.ProductPerformanceDto;
import com.storename.erp.analytics.domain.FactSales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FactSalesRepository extends JpaRepository<FactSales, Long> {
    
    @Query("SELECT SUM(f.revenue) FROM FactSales f WHERE f.branchId = :branchId AND f.dateKey >= :startDate AND f.dateKey <= :endDate")
    BigDecimal getTotalRevenue(@Param("branchId") Long branchId, @Param("startDate") Integer startDate, @Param("endDate") Integer endDate);

    @Query("SELECT SUM(f.grossProfit) FROM FactSales f WHERE f.branchId = :branchId AND f.dateKey >= :startDate AND f.dateKey <= :endDate")
    BigDecimal getTotalGrossProfit(@Param("branchId") Long branchId, @Param("startDate") Integer startDate, @Param("endDate") Integer endDate);

    @Query("SELECT SUM(f.quantity * f.unitCost) FROM FactSales f WHERE f.branchId = :branchId AND f.dateKey >= :startDate AND f.dateKey <= :endDate")
    BigDecimal getTotalCogs(@Param("branchId") Long branchId, @Param("startDate") Integer startDate, @Param("endDate") Integer endDate);

    @Query("SELECT new com.storename.erp.analytics.api.dto.ProductPerformanceDto(f.productId, '', SUM(f.quantity), SUM(f.revenue)) " +
           "FROM FactSales f WHERE f.branchId = :branchId AND f.dateKey >= :startDate AND f.dateKey <= :endDate " +
           "GROUP BY f.productId ORDER BY SUM(f.quantity) DESC LIMIT 10")
    List<ProductPerformanceDto> getTopSellingProducts(@Param("branchId") Long branchId, @Param("startDate") Integer startDate, @Param("endDate") Integer endDate);
}