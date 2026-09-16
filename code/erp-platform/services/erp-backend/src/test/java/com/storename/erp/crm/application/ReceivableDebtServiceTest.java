package com.storename.erp.crm.application;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReceivableDebtServiceTest {

    @Mock
    private ReceivableDebtRepository debtRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ReceivableDebtService receivableDebtService;

    private final UUID customerId = UUID.randomUUID();
    private final Long branchId = 1L;
    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = new Customer();
        mockCustomer.setId(customerId);
    }

    @Test
    void increaseDebt_ShouldCreateNewDebt_WhenNotExists() {
        BigDecimal amount = new BigDecimal("500.0");
        
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.empty());
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(mockCustomer));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        ReceivableDebt result = receivableDebtService.increaseDebt(customerId, branchId, amount);

        assertNotNull(result);
        assertEquals(0, amount.compareTo(result.getTotalDebt()));
        verify(debtRepository).save(any(ReceivableDebt.class));
    }

    @Test
    void increaseDebt_ShouldIncreaseExistingDebt_WhenExists() {
        BigDecimal amount = new BigDecimal("500.0");
        ReceivableDebt existingDebt = new ReceivableDebt();
        existingDebt.setTotalDebt(new BigDecimal("1000.0"));
        
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.of(existingDebt));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        ReceivableDebt result = receivableDebtService.increaseDebt(customerId, branchId, amount);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("1500.0").compareTo(result.getTotalDebt()));
        verify(customerRepository, never()).findById(any());
        verify(debtRepository).save(existingDebt);
    }

    @Test
    void decreaseDebt_ShouldDecrease_WhenExists() {
        BigDecimal amount = new BigDecimal("200.0");
        ReceivableDebt existingDebt = new ReceivableDebt();
        existingDebt.setTotalDebt(new BigDecimal("1000.0"));
        
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.of(existingDebt));
        when(debtRepository.save(any(ReceivableDebt.class))).thenAnswer(i -> i.getArgument(0));

        ReceivableDebt result = receivableDebtService.decreaseDebt(customerId, branchId, amount);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("800.0").compareTo(result.getTotalDebt()));
    }

    @Test
    void decreaseDebt_ShouldThrowException_WhenNotExists() {
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> receivableDebtService.decreaseDebt(customerId, branchId, new BigDecimal("100.0")));
    }

    @Test
    void getCurrentDebt_ShouldReturnZero_WhenNotExists() {
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.empty());

        BigDecimal result = receivableDebtService.getCurrentDebt(customerId, branchId);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void getCurrentDebt_ShouldReturnAmount_WhenExists() {
        ReceivableDebt existingDebt = new ReceivableDebt();
        existingDebt.setTotalDebt(new BigDecimal("1234.5"));
        when(debtRepository.findByCustomerIdAndBranchId(customerId, branchId)).thenReturn(Optional.of(existingDebt));

        BigDecimal result = receivableDebtService.getCurrentDebt(customerId, branchId);

        assertEquals(0, new BigDecimal("1234.5").compareTo(result));
    }
}
