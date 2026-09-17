package com.storename.erp.crm.api;

import com.storename.erp.crm.infrastructure.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmFacade {
    private final CustomerRepository customerRepository;

    public Map<UUID, String> getCustomerNames(Collection<UUID> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) return Map.of();
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(com.storename.erp.crm.domain.Customer::getId, com.storename.erp.crm.domain.Customer::getName));
    }

    public boolean customerExists(UUID customerId) {
        if (customerId == null) return false;
        return customerRepository.existsById(customerId);
    }
}
