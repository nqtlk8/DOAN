package com.storename.erp.crm.domain;

import com.storename.erp.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer")
@Getter
@Setter
public class Customer extends BaseEntity {
    
    @Column(name = "customer_code", nullable = false, unique = true, length = 50)
    private String customerCode;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 200)
    private String email;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "tax_code", length = 20)
    private String taxCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 50)
    private CustomerType customerType = CustomerType.RETAIL;
}
