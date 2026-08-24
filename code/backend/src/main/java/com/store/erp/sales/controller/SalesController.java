package com.store.erp.sales.controller;

import com.store.erp.sales.dto.*;
import com.store.erp.core.dto.ApiResponse;
import com.store.erp.core.dto.PageDto;
import com.store.erp.sales.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    // --- Quotations ---

    @PostMapping("/quotations")
    public ResponseEntity<ApiResponse<QuotationResponse>> createQuotation(@RequestBody QuotationRequest request) {
        QuotationResponse response = salesService.createQuotation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, response, "Quotation created successfully", null));
    }

    @GetMapping("/quotations")
    public ResponseEntity<ApiResponse<PageDto<QuotationDto>>> getQuotations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageDto<QuotationDto> response = salesService.getQuotations(page, size, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Quotations retrieved successfully", null));
    }

    @GetMapping("/quotations/{id}")
    public ResponseEntity<ApiResponse<QuotationDto>> getQuotationById(@PathVariable UUID id) {
        QuotationDto response = salesService.getQuotationById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Quotation details retrieved successfully", null));
    }

    @PutMapping("/quotations/{id}")
    public ResponseEntity<ApiResponse<QuotationResponse>> updateQuotation(@PathVariable UUID id, @RequestBody QuotationRequest request) {
        QuotationResponse response = salesService.updateQuotation(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Quotation updated successfully", null));
    }

    @DeleteMapping("/quotations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuotation(@PathVariable UUID id) {
        salesService.deleteQuotation(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Quotation deleted successfully", null));
    }

    // --- Sales Orders ---

    @PostMapping("/sales-orders")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> createSalesOrder(@RequestBody SalesOrderRequest request) {
        SalesOrderResponse response = salesService.createSalesOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, response, "Sales Order created successfully", null));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<ApiResponse<PageDto<SalesOrderDto>>> getSalesOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        PageDto<SalesOrderDto> response = salesService.getSalesOrders(page, size, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Sales Orders retrieved successfully", null));
    }

    @GetMapping("/sales-orders/{id}")
    public ResponseEntity<ApiResponse<SalesOrderDto>> getSalesOrderById(@PathVariable UUID id) {
        SalesOrderDto response = salesService.getSalesOrderById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Sales Order details retrieved successfully", null));
    }

    @PutMapping("/sales-orders/{id}")
    public ResponseEntity<ApiResponse<SalesOrderResponse>> updateSalesOrder(@PathVariable UUID id, @RequestBody SalesOrderRequest request) {
        SalesOrderResponse response = salesService.updateSalesOrder(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response, "Sales Order updated successfully", null));
    }

    @DeleteMapping("/sales-orders/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSalesOrder(@PathVariable UUID id) {
        salesService.deleteSalesOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Sales Order deleted successfully", null));
    }
}

