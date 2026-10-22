package com.storename.erp.catalog.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Danh mục sản phẩm trả về cho client. Cây danh mục có 2 cấp: danh mục gốc ({@code parentId == null})
 * dùng để nhóm, sản phẩm gắn vào danh mục con.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {
    private Long id;
    private String code;
    private String name;
    /** Id danh mục cha; {@code null} với danh mục gốc. */
    private Long parentId;
    /** Tên danh mục cha (để hiển thị nhóm), {@code null} với danh mục gốc. */
    private String parentName;
    /** Tên JSON là {@code isActive} (không phải {@code active} mặc định của Jackson). */
    @JsonProperty("isActive")
    private boolean isActive;
}
