package com.storename.erp.crm.application;

import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
import com.storename.erp.crm.application.dto.CustomerUpdateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.storename.erp.common.exception.ResourceNotFoundException;

@Slf4j
@Service
@ConditionalOnProperty(name = "instance.role", havingValue = "HQ")
@RequiredArgsConstructor
public class CustomerWriteService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponseDto createCustomer(CustomerCreateDto dto) {
        log.info("Creating customer with code: {}", dto.getCustomerCode());
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
        if (dto.getBranchId() != null) customer.setBranchId(dto.getBranchId());
        
        Customer saved = customerRepository.save(customer);
        return CustomerResponseDto.fromEntity(saved);
    }

    @Transactional
    public CustomerResponseDto updateCustomer(UUID id, CustomerUpdateDto dto) {
        log.info("Updating customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (dto.getName() != null) customer.setName(dto.getName());
        if (dto.getPhone() != null) customer.setPhone(dto.getPhone());
        if (dto.getEmail() != null) customer.setEmail(dto.getEmail());
        if (dto.getAddress() != null) customer.setAddress(dto.getAddress());
        if (dto.getTaxCode() != null) customer.setTaxCode(dto.getTaxCode());
        if (dto.getBranchId() != null) customer.setBranchId(dto.getBranchId());

        Customer saved = customerRepository.save(customer);
        return CustomerResponseDto.fromEntity(saved);
    }

    @Transactional
    public void softDeleteCustomer(UUID id) {
        log.info("Soft-deleting customer with id: {}", id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        customer.setDeleted(true);
        customerRepository.save(customer);
    }
}
