package com.store.erp.purchasing.service;

import com.store.erp.core.dto.PageDto;
import com.store.erp.purchasing.dto.PurchaseOrderDto;
import com.store.erp.purchasing.dto.PurchaseOrderItemDto;
import com.store.erp.purchasing.dto.PurchaseOrderRequest;
import com.store.erp.purchasing.dto.PurchaseOrderItemRequest;
import com.store.erp.catalog.entity.Distributor;
import com.store.erp.inventory.entity.InventoryTransaction;
import com.store.erp.catalog.entity.Product;
import com.store.erp.purchasing.entity.PurchaseOrder;
import com.store.erp.purchasing.entity.PurchaseOrderItem;
import com.store.erp.catalog.repos.DistributorRepository;
import com.store.erp.inventory.repos.InventoryTransactionRepository;
import com.store.erp.catalog.repos.ProductRepository;
import com.store.erp.purchasing.repos.PurchaseOrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DistributorRepository distributorRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    @Transactional
    public PurchaseOrderDto createPurchaseOrder(PurchaseOrderRequest request) {
        log.info("Creating purchase order for distributor: {}", request.getDistributorId());
        
        Distributor distributor = distributorRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found"));

        PurchaseOrder order = new PurchaseOrder();
        order.setDistributor(distributor);
        order.setStatus("COMPLETED");
        order.setOrderDate(request.getOrderDate());
        order.setNotes(request.getNotes());
        
        String orderNumber = sequenceGeneratorService.generateCode("PO", 5);
        order.setOrderNumber(orderNumber);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setPurchaseOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            
            BigDecimal itemTotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setTotalPrice(itemTotal);
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        for (PurchaseOrderItem orderItem : savedOrder.getItems()) {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(orderItem.getProduct());
            transaction.setQuantity(orderItem.getQuantity());
            transaction.setTransactionType("PURCHASE_IN");
            transaction.setReferenceId(savedOrder.getId());
            inventoryTransactionRepository.save(transaction);
        }

        return mapToDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public PageDto<PurchaseOrderDto> getPurchaseOrders(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), endDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PurchaseOrder> poPage = purchaseOrderRepository.findAll(spec, pageable);
        
        List<PurchaseOrderDto> items = poPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PageDto<>(items, poPage.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(UUID id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));
        return mapToDto(order);
    }

    @Transactional
    public PurchaseOrderDto updatePurchaseOrder(UUID id, PurchaseOrderRequest request) {
        log.info("Updating purchase order: {}", id);
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));

        Distributor distributor = distributorRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found"));

        order.setDistributor(distributor);
        order.setOrderDate(request.getOrderDate());
        order.setNotes(request.getNotes());

        order.getItems().clear();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemRequest.getProductId()));

            PurchaseOrderItem orderItem = new PurchaseOrderItem();
            orderItem.setPurchaseOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(itemRequest.getUnitPrice());
            
            BigDecimal itemTotal = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            orderItem.setTotalPrice(itemTotal);
            order.getItems().add(orderItem);

            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        
        return mapToDto(updatedOrder);
    }

    @Transactional
    public void deletePurchaseOrder(UUID id) {
        log.info("Deleting purchase order: {}", id);
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found: " + id));
        
        for (PurchaseOrderItem item : order.getItems()) {
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setProduct(item.getProduct());
            transaction.setQuantity(item.getQuantity());
            transaction.setTransactionType("PURCHASE_RETURN");
            transaction.setReferenceId(order.getId());
            inventoryTransactionRepository.save(transaction);
        }
        
        purchaseOrderRepository.delete(order);
    }

    private PurchaseOrderDto mapToDto(PurchaseOrder order) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        if (order.getDistributor() != null) {
            dto.setDistributorId(order.getDistributor().getId());
        }
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setNotes(order.getNotes());
        
        if (order.getItems() != null) {
            List<PurchaseOrderItemDto> itemDtos = order.getItems().stream().map(item -> {
                PurchaseOrderItemDto itemDto = new PurchaseOrderItemDto();
                itemDto.setId(item.getId());
                if (item.getProduct() != null) {
                    itemDto.setProductId(item.getProduct().getId());
                }
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setTotalPrice(item.getTotalPrice());
                return itemDto;
            }).collect(Collectors.toList());
            dto.setItems(itemDtos);
        } else {
            dto.setItems(new ArrayList<>());
        }
        return dto;
    }
}

