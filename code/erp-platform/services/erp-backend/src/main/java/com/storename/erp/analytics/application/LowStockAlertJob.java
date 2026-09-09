package com.storename.erp.analytics.application;

import com.storename.erp.analytics.application.port.AnalyticsDataPort;
import com.storename.erp.analytics.domain.InventoryAlertConfig;
import com.storename.erp.analytics.domain.InventoryAlertLog;
import com.storename.erp.analytics.infrastructure.InventoryAlertConfigRepository;
import com.storename.erp.analytics.infrastructure.InventoryAlertLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertJob {

    private final InventoryAlertConfigRepository configRepository;
    private final InventoryAlertLogRepository logRepository;
    private final AnalyticsDataPort analyticsDataPort;
    // private final JavaMailSender mailSender; // Disabled for dev without mail server

    @Scheduled(cron = "0 0/15 * * * *") // Run every 15 mins
    @Transactional
    public void checkLowStock() {
        log.info("[ALERT] Starting low stock check...");
        List<InventoryAlertConfig> configs = configRepository.findByIsActiveTrue();
        
        for (InventoryAlertConfig config : configs) {
            BigDecimal currentStock = analyticsDataPort.getCurrentStockQuantity(config.getProductId(), config.getBranchId());
            if (currentStock != null) {
                if (currentStock.compareTo(config.getMinQuantityThreshold()) <= 0) {
                    // It is below threshold
                    Optional<InventoryAlertLog> activeAlert = logRepository.findByProductIdAndBranchIdAndStatus(config.getProductId(), config.getBranchId(), "ACTIVE");
                    if (activeAlert.isEmpty()) {
                        // Create new alert
                        InventoryAlertLog newAlert = InventoryAlertLog.builder()
                                .productId(config.getProductId())
                                .branchId(config.getBranchId())
                                .status("ACTIVE")
                                .build();
                        logRepository.save(newAlert);
                        sendAlertEmail(config, currentStock);
                    }
                } else {
                    // It is above threshold, resolve any active alerts
                    Optional<InventoryAlertLog> activeAlert = logRepository.findByProductIdAndBranchIdAndStatus(config.getProductId(), config.getBranchId(), "ACTIVE");
                    activeAlert.ifPresent(alert -> {
                        alert.setStatus("RESOLVED");
                        logRepository.save(alert);
                        log.info("[ALERT] Resolved low stock alert for product {} at branch {}", config.getProductId(), config.getBranchId());
                    });
                }
            }
        }
        log.info("[ALERT] Finished low stock check.");
    }
    
    private void sendAlertEmail(InventoryAlertConfig config, BigDecimal currentQuantity) {
        log.warn("[ALERT-EMAIL] LOW STOCK ALERT: Product {} at Branch {} has quantity {} which is <= threshold {}. Sending email to {}", 
                config.getProductId(), config.getBranchId(), currentQuantity, config.getMinQuantityThreshold(), config.getEmailRecipients());
                
        // Uncomment to actually send email
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(config.getEmailRecipients().split(","));
        // message.setSubject("Low Stock Alert: Product " + config.getProductId());
        // message.setText("Quantity is " + currentQuantity + " (Threshold: " + config.getMinQuantityThreshold() + ")");
        // mailSender.send(message);
    }
}
