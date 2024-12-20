package com.tradingzone.services.holdings.service;

import com.tradingzone.services.holdings.data.HoldingValue;
import com.tradingzone.services.holdings.data.Holdings;
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
import java.util.*;


@Slf4j
@Service
public class HoldingsService {

    @Autowired
    private HoldingJpaRepository holdingJpaRepository;

    @Autowired
    private TradeJedisService tradeJedisService;

    public void buyTradeToHolding(String symbol, Integer qty){
        HoldingEntity entity = new HoldingEntity();
        entity.setUsrId(3);
        entity.setTckrSymb(symbol);
        entity.setAction("B");
        entity.setQty(qty);
        TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
        entity.setPric(trade.getLastPric());

        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        entity.setTradDt(Timestamp.valueOf((LocalDateTime.now()).format(formatter)));
        holdingJpaRepository.save(entity);
    }

    public void sellTradeToHolding(String symbol, Integer qty){
        HoldingEntity entity = new HoldingEntity();
        entity.setUsrId(3);
        entity.setTckrSymb(symbol);
        entity.setAction("S");
        entity.setQty(qty);
        TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
        entity.setPric(trade.getLastPric());

        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        entity.setTradDt(Timestamp.valueOf((LocalDateTime.now()).format(formatter)));
        holdingJpaRepository.save(entity);
    }



    public List<HoldingEntity> fetchAllHoldings(){
        return holdingJpaRepository.findAll();
    }

    public Holdings fetchHoldings(Integer userId){

        Map<String, List<HoldingEntity>> mapHoldingEntity = convertListToMap(holdingJpaRepository.findByUsrId(userId));
        List<HoldingValue> holdingValueLst = convertEntityToHolding(mapHoldingEntity);
        Holdings holdings = averageCostTotal(holdingValueLst);

        return holdings;
    }


    private List<HoldingValue> convertEntityToHolding(Map<String, List<HoldingEntity>> mapHoldingEntity){
        List<HoldingValue> holdingValueLst = new ArrayList<HoldingValue>();
        Iterator iterator = mapHoldingEntity.keySet().iterator();
        while (iterator.hasNext()) {
            String symbol = (String)iterator.next();
            List<HoldingEntity> lst = mapHoldingEntity.get(symbol);
            if(!lst.isEmpty()){
                TradeJedisCache trade = tradeJedisService.fetchLatestPrice(symbol);
                HoldingValue holdingValue = averageCostSymbol(symbol, lst, trade);
                if(holdingValue.getAvgQty() >0){
                    holdingValueLst.add(holdingValue);
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
            if("B".equalsIgnoreCase(holdingEntity.getAction())){
                qty += holdingEntity.getQty();
                sum += holdingEntity.getQty() * holdingEntity.getPric();

            }else{
                qty -= holdingEntity.getQty();
                sum -= holdingEntity.getQty() * holdingEntity.getPric();

            }
        }

        holdingValue.setAvgQty(qty);
        if(qty >0){
            holdingValue.setAvgCost((new BigDecimal(sum / qty)).setScale(2, RoundingMode.UP));

            holdingValue.setTotCost((new BigDecimal(sum)).setScale(2, RoundingMode.UP) );

            holdingValue.setLastPric(trade.getLastPric());
            holdingValue.setCurrValue((new BigDecimal(qty * holdingValue.getLastPric())).setScale(2, RoundingMode.UP));

            holdingValue.setPnl(holdingValue.getCurrValue().subtract(holdingValue.getTotCost()));
            holdingValue.setNetChng(holdingValue.getCurrValue().subtract(holdingValue.getTotCost()));

            MathContext pctmc = new MathContext(1);
            BigDecimal changePct = (holdingValue.getNetChng().divide(holdingValue.getCurrValue(),pctmc)).multiply(new BigDecimal("100"));
            changePct.setScale(2, RoundingMode.UP);
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
        BigDecimal netChngPct = (holdings.getTotNetChng().divide(holdings.getTotInvestment(),pctmc)).multiply(new BigDecimal("100"));
        netChngPct.setScale(2, RoundingMode.UP);
        holdings.setTotNetChngPct(netChngPct);

        BigDecimal dayChngPct = (holdings.getTotDayChng().divide(holdings.getTotInvestment(),pctmc)).multiply(new BigDecimal("100"));
        dayChngPct.setScale(2, RoundingMode.UP);
        holdings.setTotDayChngPct(dayChngPct);

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
