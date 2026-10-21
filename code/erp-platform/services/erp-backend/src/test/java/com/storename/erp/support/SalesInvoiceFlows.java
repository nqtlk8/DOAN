package com.storename.erp.support;

import com.storename.erp.order.application.SalesInvoiceService;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.domain.SalesInvoice;

import java.util.UUID;

/**
 * Lập hóa đơn bán đầy đủ trong test: lưu nháp rồi xác nhận (hai bước như người dùng thật).
 * Dùng cho các test cần một hóa đơn đã CONFIRMED (trả hàng, đối soát, đồng thời).
 */
public final class SalesInvoiceFlows {

    private SalesInvoiceFlows() {
    }

    public static SalesInvoice createAndConfirm(SalesInvoiceService service, SalesInvoiceCreateDto dto,
                                                Long branchId, UUID userId) {
        SalesInvoice draft = service.createDraft(dto, branchId);
        return service.confirmInvoice(draft.getId(), branchId, userId);
    }
}
