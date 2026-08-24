package com.storename.erp.inventory.application;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

/**
 * Dịch vụ quy đổi đơn vị tính.
 */
@Service
public class UnitConversionService {
    
    /**
     * Quy đổi số lượng từ đơn vị gốc sang đơn vị đích.
     */
    public BigDecimal convert(BigDecimal quantity, String fromUnit, String toUnit) {
        if (fromUnit == null || toUnit == null || fromUnit.equalsIgnoreCase(toUnit)) {
            return quantity;
        }
        // Sprint 2.1: stub logic. Thực tế sẽ lấy tỷ lệ quy đổi từ catalog module.
        return quantity;
    }
}
