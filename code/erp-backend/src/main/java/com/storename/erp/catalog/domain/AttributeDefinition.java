package com.storename.erp.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attribute_definition")
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "data_type", nullable = false, length = 50)
    private String dataType; // TEXT, NUMBER, BOOLEAN
}
