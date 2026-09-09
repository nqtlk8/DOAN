package com.storename.erp.crm.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.application.CustomerWriteService;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
import com.storename.erp.crm.application.dto.CustomerUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@RequiredArgsConstructor
public class CustomerWriteController {

    private final CustomerWriteService customerWriteService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerResponseDto> createCustomer(@Valid @RequestBody CustomerCreateDto dto) {
        log.info("REST request to create customer: {}", dto.getCustomerCode());
        CustomerResponseDto response = customerWriteService.createCustomer(dto);
        return ApiResponse.success(response, "Customer created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(
            @PathVariable UUID id, @Valid @RequestBody CustomerUpdateDto dto) {
        log.info("REST request to update customer: {}", id);
        CustomerResponseDto response = customerWriteService.updateCustomer(id, dto);
        return ResponseEntity.ok(ApiResponse.success(response, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable UUID id) {
        log.info("REST request to soft-delete customer: {}", id);
        customerWriteService.softDeleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
    }
}
