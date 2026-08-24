package com.store.erp.catalog.service;

import com.store.erp.catalog.dto.CustomerDto;
import com.store.erp.core.dto.PageDto;
import com.store.erp.catalog.dto.ProductDto;
import com.store.erp.catalog.entity.Customer;
import com.store.erp.catalog.entity.Product;
import com.store.erp.catalog.repos.CustomerRepository;
import com.store.erp.catalog.repos.DistributorRepository;
import com.store.erp.inventory.repos.InventoryTransactionRepository;
import com.store.erp.catalog.repos.ProductRepository;
import com.store.erp.catalog.dto.DistributorDto;
import com.store.erp.catalog.entity.Distributor;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.store.erp.purchasing.repos.PurchaseOrderRepository;
import com.store.erp.sales.repos.SalesOrderRepository;
import java.util.HashMap;
import java.util.UUID;
import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final DistributorRepository distributorRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SequenceGeneratorService sequenceGenerator;

    public PageDto<CustomerDto> getCustomers(String name, int page, int size) {
        log.info("Fetching customers with name: {}, page: {}, size: {}", name, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Customer> customerPage;
        
        if (name != null && !name.isEmpty()) {
            customerPage = customerRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }

        List<CustomerDto> items = customerPage.getContent().stream().map(this::mapToCustomerDto).collect(Collectors.toList());
        
        return new PageDto<>(items, customerPage.getTotalElements(), page, size);
    }

    public PageDto<ProductDto> getProducts(String code, String name, int page, int size) {
        log.info("Fetching products with code: {}, name: {}, page: {}, size: {}", code, name, page, size);
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> productPage = productRepository.searchProducts(code, name, pageable);

        List<ProductDto> items = productPage.getContent().stream().map(this::mapToProductDto).collect(Collectors.toList());
        
        return new PageDto<>(items, productPage.getTotalElements(), page, size);
    }

    public PageDto<DistributorDto> getDistributors(String search, int page, int size) {
        log.info("Fetching distributors with search: {}, page: {}, size: {}", search, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<Distributor> distributorPage;

        if (search != null && !search.isEmpty()) {
            distributorPage = distributorRepository.search(search, pageable);
        } else {
            distributorPage = distributorRepository.findAll(pageable);
        }

        List<DistributorDto> items = distributorPage.getContent().stream()
                .map(this::mapToDistributorDto)
                .collect(Collectors.toList());
        
        return new PageDto<>(items, distributorPage.getTotalElements(), page, size);
    }

    private CustomerDto mapToCustomerDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setCreatedAt(customer.getCreatedAt());
        dto.setUpdatedAt(customer.getUpdatedAt());
        Map<String, Object> metadata = customer.getMetadata();
        if (metadata != null) {
            if (metadata.get("code") != null) dto.setCode((String) metadata.get("code"));
            if (metadata.get("taxCode") != null) dto.setTaxCode((String) metadata.get("taxCode"));
        }
        return dto;
    }

    private ProductDto mapToProductDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setSku(product.getCode());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        Map<String, Object> metadata = product.getMetadata();
        if (metadata != null) {
            if (metadata.get("description") != null) dto.setDescription((String) metadata.get("description"));
            if (metadata.get("cost") != null) dto.setCost(new java.math.BigDecimal(metadata.get("cost").toString()));
            if (metadata.get("unit") != null) dto.setUnit((String) metadata.get("unit"));
            if (metadata.get("category") != null) dto.setCategory((String) metadata.get("category"));
        }
        return dto;
    }

    private DistributorDto mapToDistributorDto(Distributor distributor) {
        DistributorDto dto = new DistributorDto();
        dto.setId(distributor.getId());
        dto.setName(distributor.getName());
        dto.setCreatedAt(distributor.getCreatedAt());
        dto.setUpdatedAt(distributor.getUpdatedAt());
        Map<String, Object> metadata = distributor.getMetadata();
        if (metadata != null) {
            if (metadata.get("code") != null) dto.setCode((String) metadata.get("code"));
            if (metadata.get("contactName") != null) dto.setContactName((String) metadata.get("contactName"));
            if (metadata.get("phone") != null) dto.setPhone((String) metadata.get("phone"));
            if (metadata.get("email") != null) dto.setEmail((String) metadata.get("email"));
            if (metadata.get("address") != null) dto.setAddress((String) metadata.get("address"));
            if (metadata.get("region") != null) dto.setRegion((String) metadata.get("region"));
            if (metadata.get("taxCode") != null) dto.setTaxCode((String) metadata.get("taxCode"));
        }
        return dto;
    }

    public CustomerDto createCustomer(CustomerDto req) {
        Customer customer = new Customer();
        customer.setName(req.getName());
        customer.setPhone(req.getPhone());
        customer.setEmail(req.getEmail());
        customer.setAddress(req.getAddress());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("code", sequenceGenerator.generateCode("KH", 5));
        metadata.put("taxCode", req.getTaxCode());
        customer.setMetadata(metadata);
        return mapToCustomerDto(customerRepository.save(customer));
    }

    public CustomerDto updateCustomer(UUID id, CustomerDto req) {
        Customer customer = customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setName(req.getName());
        customer.setPhone(req.getPhone());
        customer.setEmail(req.getEmail());
        customer.setAddress(req.getAddress());
        Map<String, Object> metadata = customer.getMetadata() == null ? new HashMap<>() : customer.getMetadata();
        metadata.put("taxCode", req.getTaxCode());
        if (req.getCode() != null) metadata.put("code", req.getCode());
        customer.setMetadata(metadata);
        return mapToCustomerDto(customerRepository.save(customer));
    }

    public void deleteCustomer(UUID id) {
        if (salesOrderRepository.existsByCustomerId(id)) {
            throw new RuntimeException("Cannot delete customer: exists in sales orders");
        }
        customerRepository.deleteById(id);
    }

    public ProductDto createProduct(ProductDto req) {
        Product product = new Product();
        product.setCode(sequenceGenerator.generateCode("SP", 5));
        product.setName(req.getName());
        product.setPrice(req.getPrice());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("description", req.getDescription());
        metadata.put("cost", req.getCost());
        metadata.put("unit", req.getUnit());
        metadata.put("category", req.getCategory());
        product.setMetadata(metadata);
        return mapToProductDto(productRepository.save(product));
    }

    public ProductDto updateProduct(UUID id, ProductDto req) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        if (req.getSku() != null) product.setCode(req.getSku());
        product.setName(req.getName());
        product.setPrice(req.getPrice());
        Map<String, Object> metadata = product.getMetadata() == null ? new HashMap<>() : product.getMetadata();
        metadata.put("description", req.getDescription());
        metadata.put("cost", req.getCost());
        metadata.put("unit", req.getUnit());
        metadata.put("category", req.getCategory());
        product.setMetadata(metadata);
        return mapToProductDto(productRepository.save(product));
    }

    public void deleteProduct(UUID id) {
        if (inventoryTransactionRepository.existsByProductId(id)) {
            throw new RuntimeException("Cannot delete product: exists in inventory transactions");
        }
        productRepository.deleteById(id);
    }

    public DistributorDto createDistributor(DistributorDto req) {
        Distributor distributor = new Distributor();
        distributor.setName(req.getName());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("code", sequenceGenerator.generateCode("NPP", 5));
        metadata.put("contactName", req.getContactName());
        metadata.put("phone", req.getPhone());
        metadata.put("email", req.getEmail());
        metadata.put("address", req.getAddress());
        metadata.put("region", req.getRegion());
        metadata.put("taxCode", req.getTaxCode());
        distributor.setMetadata(metadata);
        return mapToDistributorDto(distributorRepository.save(distributor));
    }

    public DistributorDto updateDistributor(UUID id, DistributorDto req) {
        Distributor distributor = distributorRepository.findById(id).orElseThrow(() -> new RuntimeException("Distributor not found"));
        distributor.setName(req.getName());
        Map<String, Object> metadata = distributor.getMetadata() == null ? new HashMap<>() : distributor.getMetadata();
        if (req.getCode() != null) metadata.put("code", req.getCode());
        metadata.put("contactName", req.getContactName());
        metadata.put("phone", req.getPhone());
        metadata.put("email", req.getEmail());
        metadata.put("address", req.getAddress());
        metadata.put("region", req.getRegion());
        metadata.put("taxCode", req.getTaxCode());
        distributor.setMetadata(metadata);
        return mapToDistributorDto(distributorRepository.save(distributor));
    }

    public void deleteDistributor(UUID id) {
        if (purchaseOrderRepository.existsByDistributorId(id)) {
            throw new RuntimeException("Cannot delete distributor: exists in purchase orders");
        }
        distributorRepository.deleteById(id);
    }
}

