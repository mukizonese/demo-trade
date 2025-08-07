package com.tradingzone.services.dummy.service;

import com.google.gson.Gson;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import com.tradingzone.services.watchlist.service.WatchListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ZAddParams;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class TradeDummyService {

    @Autowired
    private TradeJedisService tradeJedisService;

    @Autowired
    private WatchListService watchListService;

    @Autowired
    private UnifiedJedis unifiedJedis;

    @Autowired
    private Gson gson;

    // Use ConcurrentHashMap for thread safety
    private final Map<String, String> activeSymbols = new ConcurrentHashMap<>();
    private final Map<String, String> watchlistSymbols = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Configuration
    private static final int BATCH_SIZE = 1000; // Increased batch size for faster processing
    private static final int CREATE_INTERVAL_SECONDS = 5; // Reduced interval for more frequent updates
    private static final int DELETE_INTERVAL_SECONDS = 120;
    
    // Price generation configuration
    private static final double BASE_VOLATILITY = 0.02; // 2% base volatility
    private static final double LARGE_MOVE_PROBABILITY = 0.1; // 10% chance of large move
    private static final double LARGE_MOVE_RANGE = 0.06; // ±3% large move
    private static final double TREND_BIAS = 0.005; // Slight upward bias
    private static final double MIN_PRICE = 1.0; // Minimum reasonable price

    public TradeDummyService() {
        executorService = Executors.newScheduledThreadPool(8); // Increased thread pool for better performance
    }

    public void initiateDummyFull(String dateString) throws Exception {
        log.info("initiateDummyFull {}", dateString);
        
        if (isRunning.get()) {
            log.warn("Dummy service is already running. Stopping previous instance.");
            stopDummyService();
        }
        
        List<String> symbolList = tradeJedisService.getSymbols();
        
        // Add all symbols to active set
        for (String symbol : symbolList) {
            activeSymbols.put(symbol, dateString);
        }
        
        // Start batch processing
        startBatchProcessing(dateString);
        
        log.info("Started dummy full service for {} symbols", symbolList.size());
    }

    public void initiateDummyWatchList(String cache, String key, String dateString) throws Exception {
        log.info("initiateDummyWatchList {} {} {}", cache, key, dateString);
        
        if (isRunning.get()) {
            log.warn("Dummy service is already running. Stopping previous instance.");
            stopDummyService();
        }
        
        String[] watchlist = watchListService.getSymbols(cache, key);
        
        // Add watchlist symbols to active set
        for (String symbol : watchlist) {
            watchlistSymbols.put(symbol, dateString);
        }
        
        // Start batch processing with optimized settings for watchlist
        startBatchProcessing(dateString);
        
        log.info("Started dummy watchlist service for {} symbols with optimized performance", watchlist.length);
    }

    /**
     * Initiate dummy trading for watchlist with high-frequency updates
     * @param cache The cache name
     * @param key The watchlist key
     * @param dateString The date string for trading simulation
     */
    public void initiateDummyWatchListHighFrequency(String cache, String key, String dateString) throws Exception {
        log.info("initiateDummyWatchListHighFrequency {} {} {}", cache, key, dateString);
        
        if (isRunning.get()) {
            log.warn("Dummy service is already running. Stopping previous instance.");
            stopDummyService();
        }
        
        String[] watchlist = watchListService.getSymbols(cache, key);
        
        // Add watchlist symbols to active set
        for (String symbol : watchlist) {
            watchlistSymbols.put(symbol, dateString);
        }
        
        // Start high-frequency batch processing
        startHighFrequencyBatchProcessing(dateString);
        
        log.info("Started high-frequency dummy watchlist service for {} symbols", watchlist.length);
    }

    /**
     * Initiate dummy trading for all watchlists of a specific user
     * @param userId The user ID whose watchlists should be simulated
     * @param dateString The date string for trading simulation
     */
    public void initiateDummyUserWatchlists(String userId, String dateString) throws Exception {
        log.info("initiateDummyUserWatchlists for user: {} date: {}", userId, dateString);
        
        if (isRunning.get()) {
            log.warn("Dummy service is already running. Stopping previous instance.");
            stopDummyService();
        }
        
        // Get all watchlists for the user
        Map<String, String[]> userWatchlists = watchListService.getUserWatchlists(userId);
        
        int totalSymbols = 0;
        // Add all symbols from all watchlists to active set
        for (Map.Entry<String, String[]> entry : userWatchlists.entrySet()) {
            String watchlistKey = entry.getKey();
            String[] symbols = entry.getValue();
            
            log.debug("Processing watchlist {} for user {} with {} symbols", watchlistKey, userId, symbols.length);
            
            for (String symbol : symbols) {
                watchlistSymbols.put(symbol, dateString);
                totalSymbols++;
            }
        }
        
        // Start batch processing
        startBatchProcessing(dateString);
        
        log.info("Started dummy user watchlists service for user: {} with {} watchlists and {} total symbols", 
                userId, userWatchlists.size(), totalSymbols);
    }

    /**
     * Initiate dummy trading for multiple users' watchlists
     * @param userIds List of user IDs whose watchlists should be simulated
     * @param dateString The date string for trading simulation
     */
    public void initiateDummyMultipleUsersWatchlists(List<String> userIds, String dateString) throws Exception {
        log.info("initiateDummyMultipleUsersWatchlists for {} users date: {}", userIds.size(), dateString);
        
        if (isRunning.get()) {
            log.warn("Dummy service is already running. Stopping previous instance.");
            stopDummyService();
        }
        
        int totalUsers = 0;
        int totalWatchlists = 0;
        int totalSymbols = 0;
        
        // Process each user's watchlists
        for (String userId : userIds) {
            try {
                Map<String, String[]> userWatchlists = watchListService.getUserWatchlists(userId);
                
                log.debug("Processing user: {} with {} watchlists", userId, userWatchlists.size());
                
                // Add all symbols from all watchlists to active set
                for (Map.Entry<String, String[]> entry : userWatchlists.entrySet()) {
                    String watchlistKey = entry.getKey();
                    String[] symbols = entry.getValue();
                    
                    for (String symbol : symbols) {
                        watchlistSymbols.put(symbol, dateString);
                        totalSymbols++;
                    }
                    totalWatchlists++;
                }
                totalUsers++;
                
            } catch (Exception e) {
                log.error("Error processing watchlists for user: {}", userId, e);
                // Continue with other users even if one fails
            }
        }
        
        // Start batch processing
        startBatchProcessing(dateString);
        
        log.info("Started dummy multiple users watchlists service for {} users with {} watchlists and {} total symbols", 
                totalUsers, totalWatchlists, totalSymbols);
    }

    private void startBatchProcessing(String dateString) {
        if (isRunning.compareAndSet(false, true)) {
            log.info("🚀 Starting batch processing for date: {}", dateString);
            
            // Schedule batch create task
            executorService.scheduleWithFixedDelay(
                new BatchCreateTradeTask(dateString), 
                0, 
                CREATE_INTERVAL_SECONDS, 
                TimeUnit.SECONDS
            );
            log.info("📅 Scheduled batch create task to run every {} seconds", CREATE_INTERVAL_SECONDS);
            
            // Schedule batch delete task
            executorService.scheduleWithFixedDelay(
                new BatchDeleteTradeTask(dateString), 
                0, 
                DELETE_INTERVAL_SECONDS, 
                TimeUnit.SECONDS
            );
            log.info("🗑️ Scheduled batch delete task to run every {} seconds", DELETE_INTERVAL_SECONDS);
        } else {
            log.warn("⚠️ Batch processing already running, skipping start");
        }
    }

    private void startHighFrequencyBatchProcessing(String dateString) {
        if (isRunning.compareAndSet(false, true)) {
            log.info("🚀 Starting high-frequency batch processing for date: {}", dateString);
            
            // Schedule batch create task with higher frequency (every 2 seconds)
            executorService.scheduleWithFixedDelay(
                new BatchCreateTradeTask(dateString), 
                0, 
                2, // Every 2 seconds for high frequency
                TimeUnit.SECONDS
            );
            log.info("📅 Scheduled high-frequency batch create task to run every 2 seconds");
            
            // Schedule batch delete task
            executorService.scheduleWithFixedDelay(
                new BatchDeleteTradeTask(dateString), 
                0, 
                DELETE_INTERVAL_SECONDS, 
                TimeUnit.SECONDS
            );
            log.info("🗑️ Scheduled batch delete task to run every {} seconds", DELETE_INTERVAL_SECONDS);
        } else {
            log.warn("⚠️ Batch processing already running, skipping start");
        }
    }

    public void stopDummyService() {
        if (isRunning.compareAndSet(true, false)) {
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = Executors.newScheduledThreadPool(4);
            }
            activeSymbols.clear();
            watchlistSymbols.clear();
            log.info("Stopped dummy service");
        }
    }

    public void clearDummyFull(String dateString) throws Exception {
        log.info("clearDummyFull {}", dateString);
        stopDummyService();
        
        List<String> symbolList = tradeJedisService.getSymbols();
        for (String symbol : symbolList) {
            clearDummyTrade(symbol, dateString);
        }
    }

    /**
     * Cleanup all manually created dummy trades across all symbols
     * This method finds and deletes all "+30 second" trades that were created by the dummy service
     * @return Map containing cleanup statistics
     */
    public Map<String, Object> clearFullCleanup() throws Exception {
        log.info("🧹 Starting full cleanup of all manually created dummy trades");
        
        List<String> symbolList = tradeJedisService.getSymbols();
        int totalSymbols = symbolList.size();
        int processedSymbols = 0;
        int totalDeletedTrades = 0;
        Map<String, Integer> symbolDeletionCounts = new HashMap<>();
        
        log.info("🔍 Processing {} symbols for cleanup", totalSymbols);
        
        for (String symbol : symbolList) {
            try {
                int deletedCount = clearAllDummyTradesForSymbol(symbol);
                if (deletedCount > 0) {
                    symbolDeletionCounts.put(symbol, deletedCount);
                    totalDeletedTrades += deletedCount;
                    log.info("🗑️ Symbol {}: deleted {} dummy trades", symbol, deletedCount);
                }
                processedSymbols++;
            } catch (Exception e) {
                log.error("❌ Error cleaning up symbol: {}", symbol, e);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalSymbols", totalSymbols);
        result.put("processedSymbols", processedSymbols);
        result.put("totalDeletedTrades", totalDeletedTrades);
        result.put("symbolDeletionCounts", symbolDeletionCounts);
        
        log.info("✅ Full cleanup completed: {} symbols processed, {} trades deleted", 
                processedSymbols, totalDeletedTrades);
        
        return result;
    }

    public void clearDummyWatchList(String cache, String key, String dateString) throws Exception {
        log.info("clearDummyWatchList {} {} {}", cache, key, dateString);
        stopDummyService();
        
        String[] watchlist = watchListService.getSymbols(cache, key);
        for (String symbol : watchlist) {
            clearDummyTrade(symbol, dateString);
        }
    }

    /**
     * Clear dummy trading for all watchlists of a specific user
     * @param userId The user ID whose watchlists should be cleared
     * @param dateString The date string for trading simulation
     */
    public void clearDummyUserWatchlists(String userId, String dateString) throws Exception {
        log.info("clearDummyUserWatchlists for user: {} date: {}", userId, dateString);
        stopDummyService();
        
        // Get all watchlists for the user
        Map<String, String[]> userWatchlists = watchListService.getUserWatchlists(userId);
        
        // Clear all symbols from all watchlists
        for (Map.Entry<String, String[]> entry : userWatchlists.entrySet()) {
            String watchlistKey = entry.getKey();
            String[] symbols = entry.getValue();
            
            for (String symbol : symbols) {
                clearDummyTrade(symbol, dateString);
            }
        }
        
        log.info("Cleared dummy user watchlists service for user: {} with {} watchlists", 
                userId, userWatchlists.size());
    }

    /**
     * Clear dummy trading for multiple users' watchlists
     * @param userIds List of user IDs whose watchlists should be cleared
     * @param dateString The date string for trading simulation
     */
    public void clearDummyMultipleUsersWatchlists(List<String> userIds, String dateString) throws Exception {
        log.info("clearDummyMultipleUsersWatchlists for {} users date: {}", userIds.size(), dateString);
        stopDummyService();
        
        int totalUsers = 0;
        int totalWatchlists = 0;
        
        // Process each user's watchlists
        for (String userId : userIds) {
            try {
                Map<String, String[]> userWatchlists = watchListService.getUserWatchlists(userId);
                
                // Clear all symbols from all watchlists
                for (Map.Entry<String, String[]> entry : userWatchlists.entrySet()) {
                    String watchlistKey = entry.getKey();
                    String[] symbols = entry.getValue();
                    
                    for (String symbol : symbols) {
                        clearDummyTrade(symbol, dateString);
                    }
                    totalWatchlists++;
                }
                totalUsers++;
                
            } catch (Exception e) {
                log.error("Error clearing watchlists for user: {}", userId, e);
                // Continue with other users even if one fails
            }
        }
        
        log.info("Cleared dummy multiple users watchlists service for {} users with {} watchlists", 
                totalUsers, totalWatchlists);
    }

    /**
     * Legacy method for single symbol dummy trading (for backward compatibility)
     * @param symbol The symbol to simulate
     * @param dateString The date string for trading simulation
     * @return Success/failure status
     */
    public String initiateDummyTrade(String symbol, String dateString) throws Exception {
        log.debug("Initiating dummy trades for {} {} .....", symbol, dateString);
        String returnString = "FAILURE";

        try {
            // Add symbol to active symbols for batch processing
            activeSymbols.put(symbol, dateString);
            
            // Start batch processing if not already running
            if (!isRunning.get()) {
                startBatchProcessing(dateString);
            }
            
            returnString = "SUCCESS";
        } catch (Exception ex) {
            log.error("Error initiating dummy trade for symbol: {}", symbol, ex);
        }

        return returnString;
    }

    /**
     * Debug method to check Redis data for a symbol
     */
    public void debugSymbolData(String symbol) {
        try {
            log.info("🔍 Debugging symbol: {}", symbol);
            
            // Get all trades for the symbol
            List<String> allTrades = unifiedJedis.zrange(symbol, 0, -1);
            log.info("📊 Total trades in Redis for {}: {}", symbol, allTrades.size());
            
            if (!allTrades.isEmpty()) {
                // Show first few trades
                for (int i = 0; i < Math.min(3, allTrades.size()); i++) {
                    TradeJedisCache trade = gson.fromJson(allTrades.get(i), TradeJedisCache.class);
                    log.info("📈 Trade {}: Date={}, Price={}, Score={}", 
                        i+1, trade.getTradDt(), trade.getLastPric(), 
                        ZonedDateTime.of(trade.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }
            
            // Get the latest trade
            List<String> latestTrades = unifiedJedis.zrange(symbol, -1, -1);
            if (!latestTrades.isEmpty()) {
                TradeJedisCache latestTrade = gson.fromJson(latestTrades.get(0), TradeJedisCache.class);
                log.info("🎯 Latest trade: Date={}, Price={}, Score={}", 
                    latestTrade.getTradDt(), latestTrade.getLastPric(), 
                    ZonedDateTime.of(latestTrade.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli());
            } else {
                log.info("🎯 No trades found for symbol: {}", symbol);
            }
            
            // Check for specific date range (2024-07-29)
            double min = parseDate("2024-07-29 00:00:00", 0);
            double max = parseDate("2024-07-29 00:00:00", 86400); // 24 hours
            List<String> dateRangeTrades = unifiedJedis.zrangeByScore(symbol, min, max, 0, 5);
            log.info("🎯 Trades in date range (2024-07-29): {}", dateRangeTrades.size());
            
            if (!dateRangeTrades.isEmpty()) {
                for (int i = 0; i < Math.min(3, dateRangeTrades.size()); i++) {
                    TradeJedisCache trade = gson.fromJson(dateRangeTrades.get(i), TradeJedisCache.class);
                    log.info("🎯 Date range trade {}: Date={}, Price={}, Score={}", 
                        i+1, trade.getTradDt(), trade.getLastPric(), 
                        ZonedDateTime.of(trade.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error debugging symbol data for: {}", symbol, e);
        }
    }

    public String clearDummyTrade(String symbol, String dateString) throws Exception {
        String returnString = "FAILURE";

        try {
            double toDeleteScore = parseDate(dateString, 30);
            double deletedScore = unifiedJedis.zremrangeByScore(symbol, toDeleteScore, toDeleteScore);
            if (deletedScore != 0.0) {
                log.info("DeleteTradeTask {} {} Score to delete {} returned score {}", 
                    symbol, dateString, toDeleteScore, deletedScore);
            }
            returnString = "SUCCESS";
        } catch (Exception ex) {
            log.error("Error clearing dummy trade for symbol: {}", symbol, ex);
        }

        return returnString;
    }

    /**
     * Clear all dummy trades for a specific symbol by finding all "+30 second" trades
     * This method identifies manually created trades by their Redis scores
     * @param symbol The symbol to clean up
     * @return Number of trades deleted
     */
    private int clearAllDummyTradesForSymbol(String symbol) throws Exception {
        try {
            // Get all trades for the symbol
            List<String> allTrades = unifiedJedis.zrange(symbol, 0, -1);
            if (allTrades.isEmpty()) {
                return 0;
            }
            
            int deletedCount = 0;
            Set<Double> scoresToDelete = new HashSet<>();
            
            // Analyze each trade to find "+30 second" trades
            for (String tradeJson : allTrades) {
                try {
                    TradeJedisCache trade = gson.fromJson(tradeJson, TradeJedisCache.class);
                    if (trade != null && trade.getTradDt() != null) {
                        // Calculate the base score (00:00:00) and +30 second score
                        LocalDateTime baseTime = trade.getTradDt().withSecond(0).withNano(0);
                        LocalDateTime plus30Time = baseTime.plusSeconds(30);
                        
                        double baseScore = ZonedDateTime.of(baseTime, ZoneId.systemDefault()).toInstant().toEpochMilli();
                        double plus30Score = ZonedDateTime.of(plus30Time, ZoneId.systemDefault()).toInstant().toEpochMilli();
                        
                        // Get the actual score of this trade
                        double actualScore = ZonedDateTime.of(trade.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli();
                        
                        // If this trade has a "+30 second" score, mark it for deletion
                        if (Math.abs(actualScore - plus30Score) < 1000) { // Within 1 second tolerance
                            scoresToDelete.add(actualScore);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error parsing trade JSON for symbol {}: {}", symbol, e.getMessage());
                }
            }
            
            // Delete all identified "+30 second" trades
            for (Double score : scoresToDelete) {
                double deleted = unifiedJedis.zremrangeByScore(symbol, score, score);
                if (deleted > 0) {
                    deletedCount += (int) deleted;
                }
            }
            
            return deletedCount;
            
        } catch (Exception e) {
            log.error("Error clearing all dummy trades for symbol: {}", symbol, e);
            return 0;
        }
    }

    private void setPriceChange(TradeJedisCache tradeCache) {
        DecimalFormat df = new DecimalFormat("#,###.##");
        MathContext mc = new MathContext(2);

        if (tradeCache.getLastPric() != null && tradeCache.getPrvsClsgPric() != null) {
            BigDecimal lp = new BigDecimal(tradeCache.getLastPric());
            BigDecimal cp = new BigDecimal(tradeCache.getPrvsClsgPric());
            BigDecimal chngeDbl = lp.subtract(cp, mc);
            chngeDbl.setScale(2, RoundingMode.UP);
            tradeCache.setChngePric(chngeDbl);
            MathContext pctmc = new MathContext(1);
            BigDecimal changePct = (chngeDbl.divide(lp, pctmc)).multiply(new BigDecimal("100"), pctmc);
            changePct.setScale(2, RoundingMode.UP);
            tradeCache.setChngePricPct(changePct);
        } else {
            log.warn("LastPric or PrvsClsgPric is null: {}", tradeCache.toString());
        }
    }

    /**
     * Generate realistic price movement with enhanced randomness
     * Simulates real market behavior with volatility, trends, and random shocks
     */
    private double generateRealisticPrice(Double currentPrice) {
        Random r = new Random();
        
        // Random volatility multiplier (0.5x to 2x base volatility)
        double volatilityMultiplier = 0.5 + r.nextDouble() * 1.5;
        
        // Calculate base price change as percentage (centered around 0)
        double priceChangePercent = (r.nextDouble() - 0.5) * 2 * BASE_VOLATILITY * volatilityMultiplier;
        
        // Add occasional larger moves for more realistic market behavior
        if (r.nextDouble() < LARGE_MOVE_PROBABILITY) {
            double largeMovePercent = (r.nextDouble() - 0.5) * LARGE_MOVE_RANGE;
            priceChangePercent += largeMovePercent;
        }
        
        // Add slight trend bias (simulates market momentum)
        double trendBias = (r.nextDouble() - 0.4) * TREND_BIAS;
        priceChangePercent += trendBias;
        
        // Calculate new price
        double newPrice = currentPrice * (1 + priceChangePercent);
        
        // Ensure price doesn't go below minimum reasonable price
        if (newPrice < MIN_PRICE) {
            newPrice = MIN_PRICE + r.nextDouble() * 2;
        }
        
        // Round to 2 decimal places
        BigDecimal bd = new BigDecimal(newPrice).setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private long parseDate(String dateString, long addSeconds) {
        long timemillis = 0;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);

        try {
            // Clean the date string by removing any surrounding quotes
            String cleanDateString = dateString.trim();
            if (cleanDateString.startsWith("\"") && cleanDateString.endsWith("\"")) {
                cleanDateString = cleanDateString.substring(1, cleanDateString.length() - 1);
                log.debug("🧹 Cleaned date string from '{}' to '{}'", dateString, cleanDateString);
            }
            
            LocalDateTime dateTime = LocalDateTime.parse(cleanDateString, formatter);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime.plusSeconds(addSeconds), ZoneId.systemDefault());
            long result = zonedDateTime.toInstant().toEpochMilli();
            log.debug("Parsed date: {} + {}s = {} (timestamp: {})", cleanDateString, addSeconds, zonedDateTime, result);
            return result;
        } catch (Exception pex) {
            log.error("❌ Error parsing date: '{}'", dateString, pex);
        }
        return timemillis;
    }

    // Batch processing task for creating trades
    @RequiredArgsConstructor
    public class BatchCreateTradeTask implements Runnable {
        final String dateString;

        @Override
        public void run() {
            if (!isRunning.get()) {
                log.debug("Batch create task not running, skipping...");
                return;
            }

            try {
                // Combine all active symbols
                Set<String> allSymbols = new HashSet<>();
                allSymbols.addAll(activeSymbols.keySet());
                allSymbols.addAll(watchlistSymbols.keySet());

                log.info("🔄 Batch create task running for {} symbols", allSymbols.size());
                
                // Process in batches
                List<String> symbolList = new ArrayList<>(allSymbols);
                int processedCount = 0;
                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < symbolList.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, symbolList.size());
                    List<String> batch = symbolList.subList(i, endIndex);
                    
                    long batchStartTime = System.currentTimeMillis();
                    log.debug("Processing batch {}/{} with {} symbols", 
                        (i/BATCH_SIZE) + 1, (symbolList.size() + BATCH_SIZE - 1)/BATCH_SIZE, batch.size());
                    
                    // Process batch
                    for (String symbol : batch) {
                        processCreateTrade(symbol, dateString);
                        processedCount++;
                    }
                    
                    long batchTime = System.currentTimeMillis() - batchStartTime;
                    log.debug("Batch {}/{} completed in {}ms", 
                        (i/BATCH_SIZE) + 1, (symbolList.size() + BATCH_SIZE - 1)/BATCH_SIZE, batchTime);
                    
                    // Minimal delay between batches to prevent overwhelming Redis
                    if (endIndex < symbolList.size()) {
                        Thread.sleep(5); // Reduced delay for faster processing
                    }
                }
                
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("✅ Batch create task completed. Processed {} symbols in {}ms (avg: {}ms per symbol)", 
                    processedCount, totalTime, processedCount > 0 ? totalTime / processedCount : 0);
            } catch (Exception e) {
                log.error("❌ Error in batch create trade task", e);
            }
        }

        private void processCreateTrade(String symbol, String dateString) {
            try {
                // Find existing trades for the given date (original logic)
                double min = parseDate(dateString, 0);
                double max = parseDate(dateString, 86400); // Add 24 hours to cover the full day

                log.debug("Looking for trades for symbol: {} between min: {} and max: {} for date: {}", 
                    symbol, min, max, dateString);
                List<String> trdList = unifiedJedis.zrangeByScore(symbol, min, max, 0, 1);

                if (trdList != null && !trdList.isEmpty()) {
                    // Found existing trade for the requested date, create a copy with +30 seconds
                    TradeJedisCache foundTrade = gson.fromJson(trdList.getFirst(), TradeJedisCache.class);
                    log.debug("Found existing trade for symbol: {} with date: {} and price: {}", 
                        symbol, foundTrade.getTradDt(), foundTrade.getLastPric());

                    // Create a copy of the found trade
                    TradeJedisCache newTrade = copyTradeWithNewPrice(foundTrade);
                    
                    if (newTrade != null) {
                        // Calculate score with +30 seconds (original logic)
                        double updatedScore = ZonedDateTime.of(foundTrade.getTradDt().plusSeconds(30), ZoneId.systemDefault()).toInstant().toEpochMilli();

                        // Add the new trade to Redis
                        long result = unifiedJedis.zadd(symbol, updatedScore, gson.toJson(newTrade), new ZAddParams().lt());
                        
                        double priceChange = newTrade.getLastPric() - foundTrade.getLastPric();
                        double priceChangePercent = (priceChange / foundTrade.getLastPric()) * 100;
                        log.info("✅ Created new trade for symbol: {} | Old: {} | New: {} | Change: {:.2f} ({:+.2f}%) | Score: {} | Redis: {}", 
                            symbol, foundTrade.getLastPric(), newTrade.getLastPric(), priceChange, priceChangePercent, updatedScore, result);
                    } else {
                        log.error("❌ Failed to create new trade copy for symbol: {}", symbol);
                    }

                } else {
                    log.warn("⚠️ No existing trade found for symbol: {} date: {} (min: {}, max: {})", 
                        symbol, dateString, min, max);
                    
                    // Try to find any trade for this symbol and create a base trade for the requested date
                    List<String> anyTrade = unifiedJedis.zrange(symbol, 0, 0);
                    if (anyTrade != null && !anyTrade.isEmpty()) {
                        TradeJedisCache templateTrade = gson.fromJson(anyTrade.getFirst(), TradeJedisCache.class);
                        log.info("🆕 Creating base trade for symbol: {} date: {} using template from: {}", 
                            symbol, dateString, templateTrade.getTradDt());
                        
                        TradeJedisCache baseTrade = createBaseTradeForDate(symbol, dateString, templateTrade);
                        if (baseTrade != null) {
                            double baseScore = parseDate(dateString, 0);
                            long result = unifiedJedis.zadd(symbol, baseScore, gson.toJson(baseTrade), new ZAddParams().lt());
                            log.info("✅ Created base trade for symbol: {} | Price: {} | Score: {} | Redis result: {}", 
                                symbol, baseTrade.getLastPric(), baseScore, result);
                        }
                    } else {
                        log.error("❌ No trades found for symbol: {} in sorted set", symbol);
                    }
                }
            } catch (Exception e) {
                log.error("❌ Error processing create trade for symbol: {}", symbol, e);
            }
        }
        
        private TradeJedisCache copyTradeWithNewPrice(TradeJedisCache originalTrade) {
            try {
                // Create a deep copy of the original trade
                TradeJedisCache newTrade = new TradeJedisCache();
                
                // Copy all fields from original trade
                newTrade.setTckrSymb(originalTrade.getTckrSymb());
                newTrade.setTradDt(originalTrade.getTradDt());
                newTrade.setBizDt(originalTrade.getBizDt());
                newTrade.setSgmt(originalTrade.getSgmt());
                newTrade.setSrc(originalTrade.getSrc());
                newTrade.setFinInstrmTp(originalTrade.getFinInstrmTp());
                newTrade.setFinInstrmId(originalTrade.getFinInstrmId());
                newTrade.setISIN(originalTrade.getISIN());
                newTrade.setSctySrs(originalTrade.getSctySrs());
                newTrade.setXpryDt(originalTrade.getXpryDt());
                newTrade.setFininstrmActlXpryDt(originalTrade.getFininstrmActlXpryDt());
                newTrade.setStrkPric(originalTrade.getStrkPric());
                newTrade.setOptnTp(originalTrade.getOptnTp());
                newTrade.setFinInstrmNm(originalTrade.getFinInstrmNm());
                newTrade.setOpnPric(originalTrade.getOpnPric());
                newTrade.setHghPric(originalTrade.getHghPric());
                newTrade.setLwPric(originalTrade.getLwPric());
                newTrade.setClsPric(originalTrade.getClsPric());
                newTrade.setPrvsClsgPric(originalTrade.getPrvsClsgPric());
                newTrade.setUndrlygPric(originalTrade.getUndrlygPric());
                newTrade.setSttlmPric(originalTrade.getSttlmPric());
                newTrade.setOpnIntrst(originalTrade.getOpnIntrst());
                newTrade.setChngInOpnIntrst(originalTrade.getChngInOpnIntrst());
                newTrade.setTtlTradgVol(originalTrade.getTtlTradgVol());
                newTrade.setTtlTrfVal(originalTrade.getTtlTrfVal());
                newTrade.setTtlNbOfTxsExctd(originalTrade.getTtlNbOfTxsExctd());
                newTrade.setSsnId(originalTrade.getSsnId());
                newTrade.setNewBrdLotQty(originalTrade.getNewBrdLotQty());
                newTrade.setRmks(originalTrade.getRmks());
                newTrade.setRsvd1(originalTrade.getRsvd1());
                newTrade.setRsvd2(originalTrade.getRsvd2());
                newTrade.setRsvd3(originalTrade.getRsvd3());
                newTrade.setRsvd4(originalTrade.getRsvd4());
                
                // Generate realistic price movement with enhanced randomness
                double newLTP = generateRealisticPrice(originalTrade.getLastPric());
                newTrade.setLastPric(newLTP);
                
                // Recalculate price change
                setPriceChange(newTrade);
                
                return newTrade;
            } catch (Exception e) {
                log.error("Error copying trade with new price", e);
                return null;
            }
        }
        
        private TradeJedisCache createBaseTradeForDate(String symbol, String dateString, TradeJedisCache templateTrade) {
            try {
                TradeJedisCache baseTrade = new TradeJedisCache();
                
                // Copy all fields from template trade
                baseTrade.setTckrSymb(templateTrade.getTckrSymb());
                baseTrade.setSgmt(templateTrade.getSgmt());
                baseTrade.setSrc(templateTrade.getSrc());
                baseTrade.setFinInstrmTp(templateTrade.getFinInstrmTp());
                baseTrade.setFinInstrmId(templateTrade.getFinInstrmId());
                baseTrade.setISIN(templateTrade.getISIN());
                baseTrade.setSctySrs(templateTrade.getSctySrs());
                baseTrade.setXpryDt(templateTrade.getXpryDt());
                baseTrade.setFininstrmActlXpryDt(templateTrade.getFininstrmActlXpryDt());
                baseTrade.setStrkPric(templateTrade.getStrkPric());
                baseTrade.setOptnTp(templateTrade.getOptnTp());
                baseTrade.setFinInstrmNm(templateTrade.getFinInstrmNm());
                baseTrade.setOpnPric(templateTrade.getOpnPric());
                baseTrade.setHghPric(templateTrade.getHghPric());
                baseTrade.setLwPric(templateTrade.getLwPric());
                baseTrade.setClsPric(templateTrade.getClsPric());
                baseTrade.setPrvsClsgPric(templateTrade.getPrvsClsgPric());
                baseTrade.setUndrlygPric(templateTrade.getUndrlygPric());
                baseTrade.setSttlmPric(templateTrade.getSttlmPric());
                baseTrade.setOpnIntrst(templateTrade.getOpnIntrst());
                baseTrade.setChngInOpnIntrst(templateTrade.getChngInOpnIntrst());
                baseTrade.setTtlTradgVol(templateTrade.getTtlTradgVol());
                baseTrade.setTtlTrfVal(templateTrade.getTtlTrfVal());
                baseTrade.setTtlNbOfTxsExctd(templateTrade.getTtlNbOfTxsExctd());
                baseTrade.setSsnId(templateTrade.getSsnId());
                baseTrade.setNewBrdLotQty(templateTrade.getNewBrdLotQty());
                baseTrade.setRmks(templateTrade.getRmks());
                baseTrade.setRsvd1(templateTrade.getRsvd1());
                baseTrade.setRsvd2(templateTrade.getRsvd2());
                baseTrade.setRsvd3(templateTrade.getRsvd3());
                baseTrade.setRsvd4(templateTrade.getRsvd4());
                
                // Set the requested date
                String pattern = "yyyy-MM-dd HH:mm:ss";
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                LocalDateTime tradeDateTime = LocalDateTime.parse(dateString, formatter);
                baseTrade.setTradDt(tradeDateTime);
                baseTrade.setBizDt(tradeDateTime);
                
                // Use the template's last price as base
                baseTrade.setLastPric(templateTrade.getLastPric());
                
                // Recalculate price change
                setPriceChange(baseTrade);
                
                return baseTrade;
            } catch (Exception e) {
                log.error("❌ Error creating base trade for date: {} symbol: {}", dateString, symbol, e);
                return null;
            }
        }
        

    }

    // Batch processing task for deleting trades
    @RequiredArgsConstructor
    public class BatchDeleteTradeTask implements Runnable {
        final String dateString;

        @Override
        public void run() {
            if (!isRunning.get()) {
                log.debug("Batch delete task not running, skipping...");
                return;
            }

            try {
                // Combine all active symbols
                Set<String> allSymbols = new HashSet<>();
                allSymbols.addAll(activeSymbols.keySet());
                allSymbols.addAll(watchlistSymbols.keySet());

                log.info("🗑️ Batch delete task running for {} symbols", allSymbols.size());

                // Process in batches
                List<String> symbolList = new ArrayList<>(allSymbols);
                int processedCount = 0;
                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < symbolList.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, symbolList.size());
                    List<String> batch = symbolList.subList(i, endIndex);
                    
                    // Process batch
                    for (String symbol : batch) {
                        processDeleteTrade(symbol, dateString);
                        processedCount++;
                    }
                    
                    // Minimal delay between batches
                    if (endIndex < symbolList.size()) {
                        Thread.sleep(5); // Reduced delay for faster processing
                    }
                }
                
                long totalTime = System.currentTimeMillis() - startTime;
                log.info("✅ Batch delete task completed. Processed {} symbols in {}ms", processedCount, totalTime);
            } catch (Exception e) {
                log.error("❌ Error in batch delete trade task", e);
            }
        }

                private void processDeleteTrade(String symbol, String dateString) {
            try {
                double toDeleteScore = parseDate(dateString, 30);
                double deletedScore = unifiedJedis.zremrangeByScore(symbol, toDeleteScore, toDeleteScore);
                if (deletedScore != 0.0) {
                    log.info("🗑️ DeleteTradeTask {} {} Score to delete {} returned score {}", 
                        symbol, dateString, toDeleteScore, deletedScore);
                } else {
                    log.debug("🗑️ DeleteTradeTask {} {} Score to delete {} - no trades deleted", 
                        symbol, dateString, toDeleteScore);
                }
            } catch (Exception e) {
                log.error("❌ Error processing delete trade for symbol: {}", symbol, e);
            }
        }
    }
}
