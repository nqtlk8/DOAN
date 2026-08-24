package com.storename.erp.common.security;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthDetails {
    private String branchId;
    private String tokenId;
}
