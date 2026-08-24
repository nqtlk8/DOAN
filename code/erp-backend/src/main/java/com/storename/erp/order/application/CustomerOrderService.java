package com.storename.erp.order.application;

import com.storename.erp.crm.domain.Customer;
import com.storename.erp.crm.infrastructure.CustomerRepository;
import com.storename.erp.order.application.dto.CustomerOrderCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceCreateDto;
import com.storename.erp.order.application.dto.SalesInvoiceLineDto;
import com.storename.erp.order.domain.CustomerOrder;
import com.storename.erp.order.domain.CustomerOrderLine;
import com.storename.erp.order.domain.PaymentMethod;
import com.storename.erp.order.domain.SalesInvoice;
import com.storename.erp.order.infrastructure.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý đơn đặt hàng của khách (Customer Order).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final SalesInvoiceService salesInvoiceService;

    /**
     * Tạo đơn đặt hàng nháp.
     */
    @Transactional
    public UUID createDraft(UUID branchId, CustomerOrderCreateDto dto) {
        if (orderRepository.existsByOrderCode(dto.getOrderCode())) {
            throw new IllegalArgumentException("Order code already exists");
        }

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        CustomerOrder order = new CustomerOrder();
        order.setBranchId(branchId);
        order.setCustomer(customer);
        order.setOrderCode(dto.getOrderCode());
        order.setNote(dto.getNote());

        for (CustomerOrderCreateDto.LineDto lineDto : dto.getLines()) {
            CustomerOrderLine line = new CustomerOrderLine();
            line.setProductId(lineDto.getProductId());
            line.setQuantity(lineDto.getQuantity());
            line.setUnitPrice(lineDto.getUnitPrice());
            line.setUnitOfMeasure(lineDto.getUnitOfMeasure());
            order.addLine(line);
        }

        order.calculateTotal();
        order = orderRepository.save(order);
        log.info("Created DRAFT Customer Order {} for branch {}", order.getId(), branchId);
        return order.getId();
    }

    /**
     * Xác nhận đơn đặt hàng, tự động tách/tạo Hoá đơn bán hàng (Sales Invoice) và confirm hoá đơn đó.
     */
    @Transactional
    public void confirmOrder(UUID orderId, UUID branchId, UUID userId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getBranchId().equals(branchId)) {
            throw new IllegalArgumentException("Order does not belong to this branch");
        }

        order.confirm();
        
        // Create Invoice Draft from Order
        SalesInvoice draftInvoice = salesInvoiceService.createFromOrder(order);
        
        // Confirm Invoice
        salesInvoiceService.confirmInvoice(draftInvoice.getId(), userId, branchId);
        
        // Mark Order as Invoiced
        order.markAsInvoiced();
        orderRepository.save(order);
        
        log.info("Confirmed Customer Order {} and auto-generated Sales Invoice {}", order.getId(), draftInvoice.getId());
    }
}
