package com.store.erp.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_sequence")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysSequence {
    @Id
    @Column(name = "seq_key", length = 50)
    private String seqKey;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;
}

