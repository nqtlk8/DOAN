package com.storename.erp.crm.api;

import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
public class CustomerWriteController {

    private final CustomerRepository customerRepository;

    public CustomerWriteController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UUID> createCustomer(@Valid @RequestBody CustomerCreateDto dto) {
        Customer customer = new Customer();
        String code = dto.getCustomerCode();
        if (code == null || code.trim().isEmpty()) {
            code = "KH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        customer.setCustomerCode(code);
        customer.setName(dto.getName());
        customer.setPhone(dto.getPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setTaxCode(dto.getTaxCode());
        customer.setCustomerType(dto.getCustomerType());
        
        Customer saved = customerRepository.save(customer);
        return ApiResponse.success(saved.getId(), "Customer created successfully");
    }
}
