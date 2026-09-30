package com.storename.erp.analytics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Cấu hình múi giờ cho báo cáo analytics (quyết định M-06, docs/fix-dashboard/00-PLAN.md).
 *
 * <p>{@code businessZone}: múi giờ của "ngày nghiệp vụ" mà người dùng chọn trên dashboard (mặc định Asia/Ho_Chi_Minh).
 * {@code storageZone}: múi giờ mà {@code confirmed_at} được ghi ({@code LocalDateTime.now()} theo giờ JVM của instance
 * xác nhận hóa đơn). Để trống = {@link ZoneId#systemDefault()}; đúng khi HQ và Branch chạy cùng múi giờ JVM
 * (Docker = UTC, chạy local = giờ máy). Nếu các instance khác múi giờ, phải cấu hình giá trị này tường minh.</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "analytics")
public class AnalyticsProperties {
    private ZoneId businessZone = ZoneId.of("Asia/Ho_Chi_Minh");
    private ZoneId storageZone = ZoneId.systemDefault();
    
    public void setBusinessZone(String businessZone) {
        if (businessZone != null && !businessZone.isEmpty()) {
            this.businessZone = ZoneId.of(businessZone);
        }
    }
    
    public void setStorageZone(String storageZone) {
        if (storageZone != null && !storageZone.isEmpty()) {
            this.storageZone = ZoneId.of(storageZone);
        } else {
            this.storageZone = ZoneId.systemDefault();
        }
    }
}
