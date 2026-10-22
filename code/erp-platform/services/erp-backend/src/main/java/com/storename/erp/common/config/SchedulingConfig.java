package com.storename.erp.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật {@code @Scheduled} cho các job định kỳ (hiện có IdempotencyCleanupJob).
 * Khung ETL {@code AnalyticsEtlJob} chỉ được tạo khi {@code analytics.etl.enabled=true}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
