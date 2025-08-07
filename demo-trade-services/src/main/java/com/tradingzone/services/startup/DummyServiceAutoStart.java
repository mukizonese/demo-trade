package com.tradingzone.services.startup;

import com.tradingzone.services.dummy.service.TradeDummyService;
import com.tradingzone.services.redis.service.TradeJedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Simple startup component that directly uses TradeDummyService to start dummy trading
 * when the application is ready, avoiding unnecessary HTTP calls and reusing existing logic.
 * 
 * Features:
 * - Direct use of TradeDummyService methods
 * - Configurable startup delay
 * - Non-blocking startup
 * - No code duplication
 * - No unnecessary HTTP calls
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DummyServiceAutoStart {

    private final TradeDummyService tradeDummyService;
    private final TradeJedisService tradeJedisService;
    
    // Configuration from application properties
    @Value("${dummy.service.auto-start.enabled:true}")
    private boolean autoStartEnabled;
    
    @Value("${dummy.service.auto-start.delay-seconds:30}")
    private int startupDelaySeconds;
    
    @Value("${dummy.service.auto-start.fallback-date:2025-07-21 00:00:00}")
    private String fallbackDate;
    
    /**
     * Event listener that triggers when the application is ready
     * Directly calls TradeDummyService to start dummy service
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoStartEnabled) {
            log.info("🚫 Auto-start of dummy service is disabled. Set dummy.service.auto-start.enabled=true to enable.");
            return;
        }
        
        log.info("🚀 Application is ready! Scheduling dummy service auto-start in {} seconds...", startupDelaySeconds);
        
        // Run the startup process asynchronously to avoid blocking application startup
        CompletableFuture.runAsync(() -> {
            try {
                // Wait for the configured delay
                log.info("⏳ Waiting {} seconds before starting dummy service...", startupDelaySeconds);
                TimeUnit.SECONDS.sleep(startupDelaySeconds);
                
                // Start the dummy service directly
                startDummyServiceDirectly();
                
            } catch (InterruptedException e) {
                log.warn("⚠️ Dummy service startup was interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("❌ Error during dummy service startup", e);
            }
        });
    }
    
    /**
     * Starts the dummy service by directly calling TradeDummyService methods
     * This reuses all existing logic without any HTTP calls or duplication
     */
    private void startDummyServiceDirectly() {
        try {
            log.info("🎯 Starting dummy service directly via TradeDummyService...");
            
            // Get the latest trade date
            String latestDate = tradeJedisService.getLatestTradeDate();
            log.info("🔍 Raw latest date from Redis: '{}'", latestDate);
            
            if (latestDate == null || latestDate.trim().isEmpty()) {
                log.warn("⚠️ No latest trade date available, using fallback date: {}", fallbackDate);
                latestDate = fallbackDate;
            } else {
                // Clean the date string by removing any surrounding quotes
                latestDate = latestDate.trim();
                if (latestDate.startsWith("\"") && latestDate.endsWith("\"")) {
                    latestDate = latestDate.substring(1, latestDate.length() - 1);
                    log.info("🧹 Cleaned date string: '{}'", latestDate);
                }
            }
            
            log.info("📅 Using latest trade date for dummy service: '{}'", latestDate);
            
            // Get the number of symbols available
            int symbolCount = tradeJedisService.getSymbols().size();
            log.info("📊 Found {} symbols available for dummy trading", symbolCount);
            
            if (symbolCount == 0) {
                log.warn("⚠️ No symbols found in Redis. Dummy service will not start.");
                return;
            }
            
            // Start the dummy service directly using existing method
            tradeDummyService.initiateDummyFull(latestDate);
            
            log.info("✅ Dummy service successfully started for {} symbols with date: {}", symbolCount, latestDate);
            log.info("🎮 Trading simulation is now active! UI will automatically refresh every 10 seconds.");
            
        } catch (Exception e) {
            log.error("❌ Failed to start dummy service directly", e);
        }
    }
} 