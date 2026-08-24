package com.store.erp.core.gateway;

import com.store.erp.core.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class GatewaySalesController {

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SALES')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrders() {
        List<Map<String, Object>> mockOrders = List.of(
                Map.of("id", 1, "customer", "John Doe", "total", 150.0, "status", "COMPLETED"),
                Map.of("id", 2, "customer", "Jane Smith", "total", 299.99, "status", "PENDING")
        );

        return ResponseEntity.ok(new ApiResponse<>(true, mockOrders, "Orders fetched successfully", null));
    }
}

