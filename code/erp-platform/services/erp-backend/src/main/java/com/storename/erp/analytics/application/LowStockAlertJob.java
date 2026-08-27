package com.storename.erp.analytics.application;

import com.storename.erp.analytics.domain.InventoryAlertConfig;
import com.storename.erp.analytics.domain.InventoryAlertLog;
import com.storename.erp.analytics.infrastructure.InventoryAlertConfigRepository;
import com.storename.erp.analytics.infrastructure.InventoryAlertLogRepository;
import com.storename.erp.inventory.domain.StockOnHand;
import com.storename.erp.inventory.infrastructure.StockOnHandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertJob {

    private final InventoryAlertConfigRepository configRepository;
    private final InventoryAlertLogRepository logRepository;
    private final StockOnHandRepository stockOnHandRepository;
    // private final JavaMailSender mailSender; // Disabled for dev without mail server

    @Scheduled(cron = "0 0/15 * * * *") // Run every 15 mins
    @Transactional
    public void checkLowStock() {
        log.info("[ALERT] Starting low stock check...");
        List<InventoryAlertConfig> configs = configRepository.findByIsActiveTrue();
        
        for (InventoryAlertConfig config : configs) {
            Optional<StockOnHand> stockOpt = stockOnHandRepository.findByProductIdAndBranchId(config.getProductId(), config.getBranchId());
            if (stockOpt.isPresent()) {
                StockOnHand stock = stockOpt.get();
                if (stock.getQuantity().compareTo(config.getMinQuantityThreshold()) <= 0) {
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
                        sendAlertEmail(config, stock);
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
    
    private void sendAlertEmail(InventoryAlertConfig config, StockOnHand stock) {
        log.warn("[ALERT-EMAIL] LOW STOCK ALERT: Product {} at Branch {} has quantity {} which is <= threshold {}. Sending email to {}", 
                config.getProductId(), config.getBranchId(), stock.getQuantity(), config.getMinQuantityThreshold(), config.getEmailRecipients());
                
        // Uncomment to actually send email
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(config.getEmailRecipients().split(","));
        // message.setSubject("Low Stock Alert: Product " + config.getProductId());
        // message.setText("Quantity is " + stock.getQuantity() + " (Threshold: " + config.getMinQuantityThreshold() + ")");
        // mailSender.send(message);
    }
}
