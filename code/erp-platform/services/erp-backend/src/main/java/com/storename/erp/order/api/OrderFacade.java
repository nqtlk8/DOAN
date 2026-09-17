package com.storename.erp.order.api;

import com.storename.erp.order.infrastructure.GoodsReturnRepository;
import com.storename.erp.order.infrastructure.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final SalesInvoiceRepository invoiceRepository;
    private final GoodsReturnRepository returnRepository;

    public Map<UUID, String> getInvoiceCodes(Collection<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) return Map.of();
        return invoiceRepository.findAllById(invoiceIds).stream()
                .collect(Collectors.toMap(com.storename.erp.order.domain.SalesInvoice::getId, com.storename.erp.order.domain.SalesInvoice::getInvoiceCode));
    }

    public Map<UUID, String> getReturnCodes(Collection<UUID> returnIds) {
        if (returnIds == null || returnIds.isEmpty()) return Map.of();
        return returnRepository.findAllById(returnIds).stream()
                .collect(Collectors.toMap(com.storename.erp.order.domain.GoodsReturn::getId, com.storename.erp.order.domain.GoodsReturn::getReturnCode));
    }
}
