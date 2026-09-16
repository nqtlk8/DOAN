package com.storename.erp.catalog.application;

import com.storename.erp.catalog.application.dto.SupplierCreateDto;
import com.storename.erp.catalog.application.dto.SupplierResponseDto;
import com.storename.erp.catalog.application.dto.SupplierUpdateDto;
import com.storename.erp.catalog.domain.Supplier;
import com.storename.erp.catalog.infrastructure.SupplierRepository;
import com.storename.erp.common.exception.ResourceNotFoundException;
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
public class SupplierWriteServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierWriteService supplierWriteService;

    private Supplier mockSupplier;
    private final UUID supplierId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockSupplier = new Supplier();
        mockSupplier.setId(supplierId);
        mockSupplier.setCode("SUP-01");
        mockSupplier.setName("Supplier A");
        mockSupplier.setIsActive(true);
    }

    @Test
    void createSupplier_ShouldSaveAndReturnDto() {
        SupplierCreateDto dto = new SupplierCreateDto();
        dto.setCode("SUP-02");
        dto.setName("Supplier B");
        dto.setBranchId(1L);

        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> {
            Supplier s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SupplierResponseDto result = supplierWriteService.createSupplier(dto);

        assertNotNull(result);
        assertEquals("SUP-02", result.getCode());
        assertEquals("Supplier B", result.getName());
        assertEquals(1L, result.getBranchId());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void updateSupplier_ShouldUpdateAndReturnDto_WhenExists() {
        SupplierUpdateDto dto = new SupplierUpdateDto();
        dto.setName("Supplier A Modified");

        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(mockSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(i -> i.getArgument(0));

        SupplierResponseDto result = supplierWriteService.updateSupplier(supplierId, dto);

        assertNotNull(result);
        assertEquals("Supplier A Modified", result.getName());
        assertEquals("SUP-01", result.getCode());
        verify(supplierRepository).save(mockSupplier);
    }

    @Test
    void updateSupplier_ShouldThrowException_WhenNotExists() {
        SupplierUpdateDto dto = new SupplierUpdateDto();
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierWriteService.updateSupplier(supplierId, dto));
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void softDeleteSupplier_ShouldSetIsActiveToFalse_WhenExists() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(mockSupplier));

        supplierWriteService.softDeleteSupplier(supplierId);

        assertFalse(mockSupplier.getIsActive());
        verify(supplierRepository).save(mockSupplier);
    }

    @Test
    void softDeleteSupplier_ShouldThrowException_WhenNotExists() {
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> supplierWriteService.softDeleteSupplier(supplierId));
        verify(supplierRepository, never()).save(any());
    }
}
