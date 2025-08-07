package com.tradingzone.services.holdings.service;

import com.tradingzone.services.holdings.data.HoldingValue;
import com.tradingzone.services.holdings.data.Holdings;
import com.tradingzone.services.holdings.data.TradeDetail;
import com.tradingzone.services.holdings.repositories.HoldingEntity;
import com.tradingzone.services.holdings.repositories.HoldingJpaRepository;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class HoldingsService {

    @Autowired
    private HoldingJpaRepository holdingJpaRepository;

    @Autowired
    private TradeJedisService tradeJedisService;

    public boolean buyTradeToHolding(String symbol, Integer qty, Integer userId){
        HoldingEntity entity = new HoldingEntity();
        entity.setUsrId(userId);
        entity.setTckrSymb(symbol);
        entity.setAction("B");
        entity.setQty(qty);
        TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
        entity.setPric(trade.getLastPric());

        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        entity.setTradDt(Timestamp.valueOf((LocalDateTime.now()).format(formatter)));
        try{
            holdingJpaRepository.save(entity);
        }catch (Exception e){
            log.error("Error while buyTradeToHolding Symbol {} Qty {} UserId {}",symbol , qty, userId);
            log.error(e.getLocalizedMessage());
            return false;
        }

        log.info("BuyTradeToHolding Symbol {} Qty {} UserId {} successfull. ",symbol , qty, userId);
        return true;
    }

    public boolean sellTradeToHolding(String symbol, Integer qty, Integer userId){
        HoldingEntity entity = new HoldingEntity();
        entity.setUsrId(userId);
        entity.setTckrSymb(symbol);
        entity.setAction("S");
        entity.setQty(qty);
        TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
        entity.setPric(trade.getLastPric());

        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        entity.setTradDt(Timestamp.valueOf((LocalDateTime.now()).format(formatter)));
        try{
            holdingJpaRepository.save(entity);
        }catch (Exception e){
            log.error("Error while sellTradeToHolding Symbol {} Qty {} UserId {}",symbol , qty, userId);
            log.error(e.getLocalizedMessage());
            return false;
        }
        log.info("SellTradeToHolding Symbol {} Qty {} UserId {} successfull. ",symbol , qty, userId);
        return true;
    }



    public List<HoldingEntity> fetchAllHoldings(){
        return holdingJpaRepository.findAll();
    }

    public Holdings fetchHoldings(Integer userId){
        log.info("Fetching holdings for userId: {}", userId);
        
        List<HoldingEntity> userHoldings = holdingJpaRepository.findByUsrId(userId);
        log.info("Found {} holding entities for userId: {}", userHoldings.size(), userId);
        
        if (userHoldings.isEmpty()) {
            log.warn("No holding entities found for userId: {}", userId);
            return new Holdings(); // Return empty holdings
        }
        
        Map<String, List<HoldingEntity>> mapHoldingEntity = convertListToMap(userHoldings);
        log.info("Converted to {} unique symbols for userId: {}", mapHoldingEntity.size(), userId);
        
        List<HoldingValue> holdingValueLst = convertEntityToHolding(mapHoldingEntity);
        log.info("Converted to {} holding values for userId: {}", holdingValueLst.size(), userId);
        
        Holdings holdings = averageCostTotal(holdingValueLst);
        log.info("Final holdings for userId {}: {} transactions, total value: {}", 
                userId, holdings.getTransactionlist().size(), holdings.getTotCurrValue());

        return holdings;
    }

    public List<TradeDetail> fetchTradeDetailsForSymbol(Integer userId, String symbol) {
        log.info("Fetching trade details for userId: {} and symbol: {}", userId, symbol);
        
        List<HoldingEntity> symbolHoldings = holdingJpaRepository.findByUsrIdAndTckrSymbOrderByTradDtDesc(userId, symbol);
        log.info("Found {} trade details for symbol {} for userId: {}", symbolHoldings.size(), symbol, userId);
        
        // Get current price for P&L calculation
        TradeJedisCache currentTrade = tradeJedisService.fetchLatestPrice(symbol);
        BigDecimal currentPrice = currentTrade != null && currentTrade.getLastPric() != null ? 
            BigDecimal.valueOf(currentTrade.getLastPric()) : BigDecimal.ZERO;
        
        List<TradeDetail> tradeDetails = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (HoldingEntity holding : symbolHoldings) {
            TradeDetail detail = new TradeDetail();
            detail.setSymbol(holding.getTckrSymb());
            detail.setAction(holding.getAction());
            detail.setQty(holding.getQty());
            detail.setPrice(BigDecimal.valueOf(holding.getPric()));
            detail.setTradeDate(holding.getTradDt());
            detail.setCurrentPrice(currentPrice);
            
            // Calculate age in days
            LocalDateTime tradeDateTime = holding.getTradDt().toLocalDateTime();
            long ageInDays = ChronoUnit.DAYS.between(tradeDateTime, now);
            detail.setAgeInDays((int) ageInDays);
            
            // Calculate P&L
            if ("B".equalsIgnoreCase(holding.getAction())) {
                // For buy trades: P&L = (Current Price - Buy Price) * Quantity
                BigDecimal pnl = currentPrice.subtract(BigDecimal.valueOf(holding.getPric()))
                        .multiply(BigDecimal.valueOf(holding.getQty()));
                detail.setPnl(pnl.setScale(2, RoundingMode.HALF_UP));
            } else if ("S".equalsIgnoreCase(holding.getAction())) {
                // For sell trades: P&L = (Sell Price - Current Price) * Quantity
                BigDecimal pnl = BigDecimal.valueOf(holding.getPric()).subtract(currentPrice)
                        .multiply(BigDecimal.valueOf(holding.getQty()));
                detail.setPnl(pnl.setScale(2, RoundingMode.HALF_UP));
            }
            
            tradeDetails.add(detail);
        }
        
        return tradeDetails;
    }

    private List<HoldingValue> convertEntityToHolding(Map<String, List<HoldingEntity>> mapHoldingEntity){
        List<HoldingValue> holdingValueLst = new ArrayList<HoldingValue>();
        Iterator iterator = mapHoldingEntity.keySet().iterator();
        while (iterator.hasNext()) {
            String symbol = (String)iterator.next();
            List<HoldingEntity> lst = mapHoldingEntity.get(symbol);
            if(!lst.isEmpty()){
                log.debug("Processing symbol: {} with {} transactions", symbol, lst.size());
                TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
                if (trade == null) {
                    log.warn("No latest price found for symbol: {}", symbol);
                    continue;
                }
                HoldingValue holdingValue = averageCostSymbol(symbol, lst, trade);
                log.debug("Symbol: {} - AvgQty: {}, AvgCost: {}", symbol, holdingValue.getAvgQty(), holdingValue.getAvgCost());
                if(holdingValue.getAvgQty() >0){
                    holdingValueLst.add(holdingValue);
                    log.debug("Added holding for symbol: {}", symbol);
                } else {
                    log.debug("Skipped holding for symbol: {} (avgQty <= 0)", symbol);
                }

            }
        }
        return  holdingValueLst;
    }

    private HoldingValue averageCostSymbol(String symbol, List<HoldingEntity> lst, TradeJedisCache trade)
    {
        HoldingValue holdingValue = new HoldingValue();
        holdingValue.setId(symbol);
        holdingValue.setTckrSymb(symbol);

        int qty = 0;
        double sum = 0;
        int i = 0;

        for (HoldingEntity holdingEntity : lst) {
            String action = holdingEntity.getAction();
            log.debug("Processing transaction: Symbol={}, Action={}, Qty={}, Price={}", 
                     symbol, action, holdingEntity.getQty(), holdingEntity.getPric());
            
            if("B".equalsIgnoreCase(action) || "BUY".equalsIgnoreCase(action)){
                qty += holdingEntity.getQty();
                sum += holdingEntity.getQty() * holdingEntity.getPric();
                log.debug("BUY: qty={}, sum={}", qty, sum);

            }else if("S".equalsIgnoreCase(action) || "SELL".equalsIgnoreCase(action)){
                qty -= holdingEntity.getQty();
                sum -= holdingEntity.getQty() * holdingEntity.getPric();
                log.debug("SELL: qty={}, sum={}", qty, sum);

            }
        }
        
        log.debug("Final calculation for symbol {}: qty={}, sum={}", symbol, qty, sum);

        holdingValue.setAvgQty(qty);
        if(qty >0){
            holdingValue.setAvgCost((new BigDecimal(sum / qty)).setScale(2, RoundingMode.UP));

            holdingValue.setTotCost((new BigDecimal(sum)).setScale(2, RoundingMode.UP) );

            holdingValue.setLastPric(trade.getLastPric());
            holdingValue.setCurrValue((new BigDecimal(qty * holdingValue.getLastPric())).setScale(2, RoundingMode.UP));

            holdingValue.setPnl(holdingValue.getCurrValue().subtract(holdingValue.getTotCost()));
            holdingValue.setNetChng(holdingValue.getCurrValue().subtract(holdingValue.getTotCost()));

            MathContext pctmc = new MathContext(1);
            BigDecimal changePct = BigDecimal.ZERO;
            
            // Check if current value is not zero before performing division
            if (holdingValue.getCurrValue().compareTo(BigDecimal.ZERO) != 0) {
                changePct = (holdingValue.getNetChng().divide(holdingValue.getCurrValue(),pctmc)).multiply(new BigDecimal("100"));
                changePct = changePct.setScale(2, RoundingMode.UP);
            }
            
            holdingValue.setNetChngPct(changePct);

            holdingValue.setPrvsClsgPric(trade.getPrvsClsgPric());
            holdingValue.setChngePric(trade.getChngePric());
            holdingValue.setDayChng(trade.getChngePric().setScale(2, RoundingMode.UP));
            holdingValue.setDayChng((trade.getChngePric().multiply(new BigDecimal(qty))).setScale(2, RoundingMode.UP));
            holdingValue.setDayChngPct(trade.getChngePricPct());
        }


        return holdingValue;
    }

    private Holdings averageCostTotal( List<HoldingValue> lst)
    {
        Holdings holdings = new Holdings();
        holdings.setTransactionlist(lst);

        BigDecimal totCost = BigDecimal.ZERO;
        BigDecimal totCurrVal = BigDecimal.ZERO;
        BigDecimal totNetChng = BigDecimal.ZERO;
        BigDecimal totDayChng = BigDecimal.ZERO;
        for (HoldingValue holdingValue : lst) {
            totCost = totCost.add(holdingValue.getTotCost());
            totCurrVal = totCurrVal.add(holdingValue.getCurrValue());
            totNetChng = totNetChng.add(holdingValue.getNetChng());
            totDayChng = totDayChng.add(holdingValue.getDayChng());
        }
        holdings.setTotInvestment(totCost);
        holdings.setTotCurrValue(totCurrVal);
        holdings.setTotPnl(totNetChng);
        holdings.setTotNetChng(totNetChng);
        holdings.setTotDayChng(totDayChng);

        MathContext pctmc = new MathContext(2);
        BigDecimal netChngPct = BigDecimal.ZERO;
        BigDecimal dayChngPct = BigDecimal.ZERO;
        
        // Check if total investment is not zero before performing division
        if (holdings.getTotInvestment().compareTo(BigDecimal.ZERO) != 0) {
            netChngPct = (holdings.getTotNetChng().divide(holdings.getTotInvestment(),pctmc)).multiply(new BigDecimal("100"));
            netChngPct = netChngPct.setScale(2, RoundingMode.UP);
            
            dayChngPct = (holdings.getTotDayChng().divide(holdings.getTotInvestment(),pctmc)).multiply(new BigDecimal("100"));
            dayChngPct = dayChngPct.setScale(2, RoundingMode.UP);
        }
        
        holdings.setTotNetChngPct(netChngPct);
        holdings.setTotDayChngPct(dayChngPct);
        
        // Calculate P&L percentage
        BigDecimal totPnlPct = BigDecimal.ZERO;
        if (holdings.getTotInvestment().compareTo(BigDecimal.ZERO) != 0) {
            totPnlPct = (holdings.getTotPnl().divide(holdings.getTotInvestment(),pctmc)).multiply(new BigDecimal("100"));
            totPnlPct = totPnlPct.setScale(2, RoundingMode.UP);
        }
        holdings.setTotPnlPct(totPnlPct);

        return holdings;

    }

    private Map<String, List<HoldingEntity>> convertListToMap(List<HoldingEntity> lstHoldingEntity){

        Map<String, List<HoldingEntity>> map = new HashMap<>();
        for (HoldingEntity holdingEntity : lstHoldingEntity) {
            if(!map.containsKey(holdingEntity.getTckrSymb())){
                List<HoldingEntity> lst = new ArrayList<HoldingEntity>();
                lst.add(holdingEntity);
                map.put(holdingEntity.getTckrSymb(), lst );
            }else{
                map.get(holdingEntity.getTckrSymb()).add(holdingEntity);
            }
        }
        return  map;
    }


}
