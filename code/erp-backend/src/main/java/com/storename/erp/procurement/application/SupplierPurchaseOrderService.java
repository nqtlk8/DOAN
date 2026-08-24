package com.storename.erp.procurement.application;

import com.storename.erp.inventory.domain.event.InboundReceiptConfirmedEvent;
import com.storename.erp.procurement.application.dto.SupplierPurchaseOrderCreateDto;
import com.storename.erp.procurement.domain.Supplier;
import com.storename.erp.procurement.domain.SupplierPurchaseOrder;
import com.storename.erp.procurement.domain.SupplierPurchaseOrderLine;
import com.storename.erp.procurement.infrastructure.SupplierPurchaseOrderRepository;
import com.storename.erp.procurement.infrastructure.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPurchaseOrderService {

    private final SupplierPurchaseOrderRepository poRepository;
    private final SupplierRepository supplierRepository;
    private final PayableDebtService debtService;

    @Transactional
    public UUID createDraft(UUID branchId, SupplierPurchaseOrderCreateDto dto) {
        if (poRepository.existsByPoCode(dto.getPoCode())) {
            throw new IllegalArgumentException("PO Code already exists");
        }

        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        SupplierPurchaseOrder po = new SupplierPurchaseOrder();
        po.setBranchId(branchId);
        po.setSupplier(supplier);
        po.setPoCode(dto.getPoCode());
        po.setNote(dto.getNote());

        for (SupplierPurchaseOrderCreateDto.LineDto lineDto : dto.getLines()) {
            SupplierPurchaseOrderLine line = new SupplierPurchaseOrderLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitCost(lineDto.getUnitCost());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            po.addLine(line);
        }

        po.calculateTotal();
        po = poRepository.save(po);
        log.info("Created DRAFT Purchase Order {} for supplier {}", po.getId(), supplier.getId());
        return po.getId();
    }

    @Transactional
    public void confirmOrder(UUID poId, UUID branchId) {
        SupplierPurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("PO not found"));

        if (!po.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("PO does not belong to this branch");
        }

        po.confirm();
        poRepository.save(po);
        log.info("Confirmed Purchase Order {}", po.getId());
    }

    @Transactional
    @EventListener
    public void handleInboundReceiptConfirmed(InboundReceiptConfirmedEvent event) {
        if (event.getPurchaseOrderId() != null) {
            SupplierPurchaseOrder po = poRepository.findById(event.getPurchaseOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("PO not found for inbound receipt"));
            
            if (!po.getBranchId().equals(event.getBranchId())) {
                log.error("Branch mismatch: PO {} belongs to {}, but receipt is from {}", po.getId(), po.getBranchId(), event.getBranchId());
                throw new IllegalArgumentException("Cannot receive PO from different branch");
            }
            
            po.receive();
            poRepository.save(po);
            
            // Increase debt for supplier
            debtService.increaseDebt(po.getSupplier().getId(), event.getBranchId(), po.getTotalAmount());
            log.info("Auto received Purchase Order {} and updated debt via Inbound Receipt {}", po.getId(), event.getReceiptId());
        }
    }
}
