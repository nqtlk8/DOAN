package com.store.erp.purchasing.controller;

import com.store.erp.core.dto.ApiResponse;
import com.store.erp.core.dto.PageDto;
import com.store.erp.purchasing.dto.PurchaseOrderDto;
import com.store.erp.purchasing.dto.PurchaseOrderRequest;
import com.store.erp.purchasing.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {
        
        PurchaseOrderDto response = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, response, "Purchase order created successfully", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageDto<PurchaseOrderDto>>> getPurchaseOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageDto<PurchaseOrderDto> response = purchaseOrderService.getPurchaseOrders(startDate, endDate, page, size);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Purchase orders retrieved successfully", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> getPurchaseOrderById(@PathVariable UUID id) {
        PurchaseOrderDto response = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Purchase order retrieved successfully", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> updatePurchaseOrder(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        
        PurchaseOrderDto response = purchaseOrderService.updatePurchaseOrder(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Purchase order updated successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePurchaseOrder(@PathVariable UUID id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Purchase order deleted successfully", null));
    }
}

