package com.storename.erp.crm.application;

import com.storename.erp.branch.api.BranchFacade;
import com.storename.erp.common.exception.BusinessConflictException;
import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
import com.storename.erp.crm.application.dto.CustomerUpdateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ghi khách hàng (chỉ HQ). Mã khách không được trùng (kể cả khách đã xóa mềm) -> HTTP 409.
 * branchId (nếu gửi) phải là chi nhánh có thật -> nếu không, HTTP 400.
 */
@Slf4j
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${instance.role:ALL}' == 'HQ' or '${instance.role:ALL}' == 'ALL'")
@RequiredArgsConstructor
public class CustomerWriteService {

    private final CustomerRepository customerRepository;
    private final BranchFacade branchFacade;

    @Transactional
    public CustomerResponseDto createCustomer(CustomerCreateDto dto) {
        log.info("Creating customer with code: {}", dto.getCustomerCode());
        String code = dto.getCustomerCode();
        if (code == null || code.trim().isEmpty()) {
            code = "KH" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (customerRepository.findByCustomerCode(code).isPresent()) {
            throw new BusinessConflictException("CUSTOMER_CODE_EXISTS", "Mã khách hàng '" + code + "' đã tồn tại");
        }
        requireExistingBranch(dto.getBranchId());

        Customer customer = new Customer();
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
        requireExistingBranch(dto.getBranchId());

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

    /** customer.branch_id không có khóa ngoại (V3), nên kiểm tra ở đây. */
    private void requireExistingBranch(Long branchId) {
        if (branchId != null && !branchFacade.branchExists(branchId)) {
            throw new IllegalArgumentException("Branch " + branchId + " does not exist");
        }
    }
}
