package com.storename.erp.crm.application;

import com.storename.erp.common.exception.ResourceNotFoundException;
import com.storename.erp.crm.application.dto.CustomerCreateDto;
import com.storename.erp.crm.application.dto.CustomerResponseDto;
import com.storename.erp.crm.application.dto.CustomerUpdateDto;
import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustomerWriteServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerWriteService customerWriteService;

    private Customer mockCustomer;
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(customerId);
        mockCustomer.setCustomerCode("KH123");
        mockCustomer.setName("Nguyen Van A");
    }

    @Test
    void createCustomer_ShouldSaveAndReturnDto() {
        CustomerCreateDto dto = new CustomerCreateDto();
        dto.setName("Nguyen Van A");
        dto.setPhone("0123456789");

        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> {
            Customer c = i.getArgument(0);
            c.setId(customerId);
            return c;
        });

        CustomerResponseDto result = customerWriteService.createCustomer(dto);

        assertNotNull(result);
        assertEquals(customerId, result.getId());
        assertEquals("Nguyen Van A", result.getName());
        assertNotNull(result.getCustomerCode()); // Generated or passed
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomer_ShouldUpdateAndReturnDto_WhenExists() {
        CustomerUpdateDto dto = new CustomerUpdateDto();
        dto.setName("Nguyen Van B");

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        CustomerResponseDto result = customerWriteService.updateCustomer(customerId, dto);

        assertNotNull(result);
        assertEquals("Nguyen Van B", result.getName());
        assertEquals("KH123", result.getCustomerCode());
        verify(customerRepository).save(mockCustomer);
    }

    @Test
    void updateCustomer_ShouldThrowException_WhenNotExists() {
        CustomerUpdateDto dto = new CustomerUpdateDto();
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerWriteService.updateCustomer(customerId, dto));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void softDeleteCustomer_ShouldSetDeletedFlag_WhenExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));

        customerWriteService.softDeleteCustomer(customerId);

        assertTrue(mockCustomer.isDeleted());
        verify(customerRepository).save(mockCustomer);
    }

    @Test
    void softDeleteCustomer_ShouldThrowException_WhenNotExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> customerWriteService.softDeleteCustomer(customerId));
        verify(customerRepository, never()).save(any());
    }
}
