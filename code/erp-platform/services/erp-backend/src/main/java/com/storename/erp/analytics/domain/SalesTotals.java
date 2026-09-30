package com.storename.erp.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesTotals {
    private BigDecimal revenue;
    private BigDecimal cogs;
}
