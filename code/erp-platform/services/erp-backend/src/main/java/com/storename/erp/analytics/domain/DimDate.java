package com.storename.erp.analytics.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "dim_date")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DimDate {
    @Id
    @Column(name = "date_key")
    private Integer dateKey;

    @Column(name = "full_date", nullable = false)
    private LocalDate fullDate;

    @Column(nullable = false)
    private Short year;

    @Column(nullable = false)
    private Short month;

    @Column(nullable = false)
    private Short day;
}