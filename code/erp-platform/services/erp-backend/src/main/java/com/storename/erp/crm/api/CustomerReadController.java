package com.storename.erp.crm.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerReadController {

    private final CustomerRepository customerRepository;

    public CustomerReadController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<java.util.List<Customer>> getCustomers(@RequestParam(required = false) String search) {
        java.util.List<Customer> all = customerRepository.findAll();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase();
            all = all.stream().filter(c -> 
                (c.getName() != null && c.getName().toLowerCase().contains(lowerSearch)) ||
                (c.getCustomerCode() != null && c.getCustomerCode().toLowerCase().contains(lowerSearch)) ||
                (c.getPhone() != null && c.getPhone().contains(search))
            ).toList();
        }
        return ApiResponse.success(all);
    }
}
