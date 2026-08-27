package com.storename.erp.crm.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER_CREATE', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createCustomer(@Valid @RequestBody CustomerCreateDto dto) {
        Customer customer = new Customer();
        customer.setCustomerCode(dto.getCustomerCode());
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setTaxCode(dto.getTaxCode());
        customer.setCustomerType(dto.getCustomerType());
        
        Customer saved = customerRepository.save(customer);
        return ApiResponse.success(saved.getId(), "Customer created successfully");
    }
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER_READ', 'ADMIN', 'SALES')")
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
