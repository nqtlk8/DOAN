package com.storename.erp.crm.api;

import lombok.extern.slf4j.Slf4j;
import com.storename.erp.branch.api.BranchFacade;
import com.storename.erp.order.api.OrderFacade;
import com.storename.erp.common.api.ApiResponse;
import com.storename.erp.crm.domain.ReceivableDebt;
import com.storename.erp.crm.domain.ReceivableDebtMovement;
import com.storename.erp.crm.infrastructure.ReceivableDebtRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receivable-debts")
@RequiredArgsConstructor
@Tag(name = "Receivable Debt", description = "Quản lý công nợ khách hàng")
@Slf4j
public class ReceivableDebtController {

    private final ReceivableDebtRepository debtRepository;
    private final com.storename.erp.crm.application.ReceivableDebtService debtService;
    private final BranchFacade branchFacade;
    private final OrderFacade orderFacade;
    private final com.storename.erp.crm.application.CashReceiptService cashReceiptService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.crm.application.dto.ReceivableDebtResponseDto>> getDebts() {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        List<ReceivableDebt> debts;
        if (branchId == null) {
            debts = debtRepository.findAll();
        } else {
            debts = debtRepository.findByBranchId(branchId);
        }
        
        List<com.storename.erp.crm.application.dto.ReceivableDebtResponseDto> response = debts.stream()
                .map(com.storename.erp.crm.application.dto.ReceivableDebtResponseDto::fromEntity)
                .toList();
        
        return ApiResponse.success(response);
    }

    @GetMapping("/{customerId}/movements")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<List<com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto>> getMovements(
            @PathVariable java.util.UUID customerId) {
        
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();

        List<ReceivableDebtMovement> movements = debtService.getMovements(customerId, branchId);
        
        // --- N+1 Cleanup (Batch Fetching) ---
        // 1. Collect all distinct IDs
        java.util.Set<Long> branchIds = movements.stream()
                .map(ReceivableDebtMovement::getBranchId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
                
        java.util.Set<java.util.UUID> invoiceIds = new java.util.HashSet<>();
        java.util.Set<java.util.UUID> returnIds = new java.util.HashSet<>();
        java.util.Set<java.util.UUID> receiptIds = new java.util.HashSet<>();
        
        for (ReceivableDebtMovement m : movements) {
            if (m.getRefId() != null && !m.getRefId().equals("INIT")) {
                try {
                    java.util.UUID refUuid = java.util.UUID.fromString(m.getRefId());
                    if ("SALES_INVOICE".equals(m.getRefType()) || "SALES_INVOICE_ADVANCE".equals(m.getRefType())) {
                        invoiceIds.add(refUuid);
                    } else if ("GOODS_RETURN".equals(m.getRefType())) {
                        returnIds.add(refUuid);
                    } else if (isCashReceiptRef(m.getRefType())) {
                        receiptIds.add(refUuid);
                    }
                } catch (Exception e) {
                    // ignore invalid UUID
                }
            }
        }
        
        // 2. Batch fetch and map
        java.util.Map<Long, String> branchNameMap = branchFacade.getBranchNames(branchIds);
                
        java.util.Map<java.util.UUID, String> invoiceCodeMap = orderFacade.getInvoiceCodes(invoiceIds);
                
        java.util.Map<java.util.UUID, String> returnCodeMap = orderFacade.getReturnCodes(returnIds);

        java.util.Map<java.util.UUID, String> receiptCodeMap = cashReceiptService.getReceiptCodes(receiptIds);
        
        // 3. Construct response
        List<com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto> response = movements.stream()
                .map(m -> {
                    var dto = com.storename.erp.crm.application.dto.ReceivableDebtMovementResponseDto.fromEntity(m);
                    if (m.getBranchId() != null) {
                        dto.setBranchName(branchNameMap.get(m.getBranchId()));
                    }
                    if (m.getRefId() != null && !m.getRefId().equals("INIT")) {
                        try {
                            java.util.UUID refUuid = java.util.UUID.fromString(m.getRefId());
                            if ("SALES_INVOICE".equals(m.getRefType()) || "SALES_INVOICE_ADVANCE".equals(m.getRefType())) {
                                dto.setReferenceCode(invoiceCodeMap.get(refUuid));
                            } else if ("GOODS_RETURN".equals(m.getRefType())) {
                                dto.setReferenceCode(returnCodeMap.get(refUuid));
                            } else if (isCashReceiptRef(m.getRefType())) {
                                dto.setReferenceCode(receiptCodeMap.get(refUuid));
                            }
                        } catch (Exception e) {}
                    }
                    return dto;
                })
                .toList();

        return ApiResponse.success(response);
    }

    /** Biến động do phiếu thu: lập phiếu (PAYMENT) hoặc hủy phiếu (ADJUSTMENT). */
    private static boolean isCashReceiptRef(String refType) {
        return com.storename.erp.crm.application.CashReceiptService.REF_RECEIPT.equals(refType)
                || com.storename.erp.crm.application.CashReceiptService.REF_RECEIPT_CANCEL.equals(refType);
    }

    @GetMapping("/{customerId}/balance")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ApiResponse<java.math.BigDecimal> getBalance(@PathVariable java.util.UUID customerId) {
        Long branchId = com.storename.erp.common.security.AuthUtils.getBranchIdOrNull();
        java.math.BigDecimal balance = debtService.getCurrentDebt(customerId, branchId);
        return ApiResponse.success(balance);
    }
}
