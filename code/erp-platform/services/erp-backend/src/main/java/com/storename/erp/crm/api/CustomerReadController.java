package com.storename.erp.crm.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerReadController {

    private final CustomerRepository customerRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<CustomerResponseDto>> getCustomers(@RequestParam(required = false) String search) {
        log.info("REST request to get customers, search: {}", search);
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<Customer> all = customerRepository.findAll();
        List<CustomerResponseDto> result = all.stream()
                .filter(c -> !c.isDeleted()) // Filter out soft-deleted
                .filter(c -> branchId == null || branchId.equals(c.getBranchId())) // Tenant isolation
                .filter(c -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String lowerSearch = search.toLowerCase();
                    return (c.getName() != null && c.getName().toLowerCase().contains(lowerSearch)) ||
                           (c.getCustomerCode() != null && c.getCustomerCode().toLowerCase().contains(lowerSearch)) ||
                           (c.getPhone() != null && c.getPhone().contains(search));
                })
                .map(CustomerResponseDto::fromEntity)
                .toList();
                
        log.debug("Returning {} customers", result.size());
        return ApiResponse.success(result);
    }

}
