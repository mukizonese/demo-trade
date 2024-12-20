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

    private Map<String, String> masterMap;
    private ScheduledExecutorService executorService;

    public TradeDummyService() {

        masterMap = new HashMap<>();
        executorService = Executors
                .newScheduledThreadPool( 50 );
    }

    public void initiateDummyFull(String dateString) throws Exception {
        log.info("initiateDummyFull {}", dateString);
        List<String> symbollist = tradeJedisService.getSymbols();

        for(String symbol :symbollist){

            if( !masterMap.containsKey(symbol))
                initiateDummyTrade(symbol, dateString);
        }
    }

    public void initiateDummyWatchList(String cache, String key, String dateString) throws Exception {
        log.info("initiateDummyWatchList {} {} {}", cache, key, dateString);
        String[] watchlist = watchListService.getSymbols(cache, key);

        for(String symbol :watchlist){
            if( !masterMap.containsKey(symbol))
                initiateDummyTrade(symbol, dateString);
        }
    }

    public String initiateDummyTrade(String symbol, String dateString) throws Exception {
        log.debug("Initiating dummy trades for {} {} .....", symbol, dateString);
        String retrunString = "FAILURE";

        //ScheduledExecutorService executorService = Executors
          //      .newScheduledThreadPool( 1 );

        try {

            CreateTradeTask createTradeTask = new CreateTradeTask(symbol, dateString);
            DeleteTradeTask deleteTradeTask = new DeleteTradeTask(symbol, dateString);

            //executorService.schedule(createTradeTask, 10, TimeUnit.SECONDS);
            executorService.scheduleWithFixedDelay(createTradeTask, 0, 10, TimeUnit.SECONDS);

            //executorService.schedule(deleteTradeTask, 3, TimeUnit.MINUTES);
            executorService.scheduleWithFixedDelay(deleteTradeTask, 0, 120, TimeUnit.SECONDS);

            masterMap.put(symbol, symbol);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        retrunString = "SUCCESS";
        return retrunString;
    }

    public void clearDummyFull(String dateString) throws Exception {
        log.info("clearDummyFull {}", dateString);

        if(executorService!=null){
            executorService.shutdownNow();
        }

        List<String> symbollist = tradeJedisService.getSymbols();
        for(String symbol :symbollist){
            clearDummyTrade(symbol, dateString);
        }
    }
    public void clearDummyWatchList(String cache, String key, String dateString) throws Exception {
        log.info("clearDummyWatchList {} {} {}", cache, key, dateString);

        if(executorService!=null){
            executorService.shutdownNow();
        }

        String[] watchlist = watchListService.getSymbols(cache, key);
        for(String symbol :watchlist){
            clearDummyTrade(symbol, dateString);
        }
    }

    public String clearDummyTrade(String symbol, String dateString) throws Exception {
        //log.info("Initiating clear dummy trades for {} {} .....", symbol, dateString);
        String retrunString = "FAILURE";

        try {

            masterMap.remove(symbol);

            double toDeleteScore = parseDate(dateString,30);
            double deletedScore = unifiedJedis.zremrangeByScore(symbol, toDeleteScore, toDeleteScore);
            if (deletedScore != 0.0)
                log.info("DeleteTradeTask {} {} Score to delete {} returned score {}", symbol, dateString, toDeleteScore, deletedScore);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        retrunString = "SUCCESS";
        return retrunString;
    }

    private void setPriceChange(TradeJedisCache tradeCache){
        DecimalFormat df = new DecimalFormat("#,###.##");
        MathContext mc = new MathContext(2);

        if(tradeCache.getLastPric() != null && tradeCache.getPrvsClsgPric() != null ){
            BigDecimal lp = new BigDecimal(tradeCache.getLastPric());
            BigDecimal cp = new BigDecimal(tradeCache.getPrvsClsgPric());
            BigDecimal chngeDbl = lp.subtract(cp, mc);
            chngeDbl.setScale(2, RoundingMode.UP);
            tradeCache.setChngePric(chngeDbl);
            MathContext pctmc = new MathContext(1);
            BigDecimal changePct = (chngeDbl.divide(lp,pctmc)).multiply(new BigDecimal("100"),pctmc);
            changePct.setScale(2, RoundingMode.UP);
            tradeCache.setChngePricPct(changePct);
        }else{
            System.out.println("LastPric or PrvsClsgPric is null : "+ tradeCache.toString());
        }

    }

    private long parseDate(String dateString, long addSeconds) {

        long timemillis = 0;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);

        try{
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime.plusSeconds(addSeconds), ZoneId.systemDefault());
            return zonedDateTime.toInstant().toEpochMilli();

        }catch(Exception pex){
            pex.printStackTrace();
        }
        return timemillis;
    }


    @RequiredArgsConstructor
    public class CreateTradeTask implements Runnable {
        final String symbol;
        final String dateString;

        @Override
        public void run() {

            TradeJedisCache foundTrade = null;
            double updatedScore;

            double min = parseDate(dateString,0);
            double max = System.currentTimeMillis();

            List<String> trdList = unifiedJedis.zrangeByScore(symbol, min, max, 0, 1);

            if (trdList != null && !trdList.isEmpty()) {
                foundTrade = gson.fromJson(trdList.getFirst(), TradeJedisCache.class);

                Random r = new Random();
                double randomValue = r.nextDouble()*5;
                double oldLTP = foundTrade.getLastPric();
                double rangeMin = oldLTP - randomValue;
                double rangeMax = oldLTP + randomValue;
                double randomRangeValue = rangeMin + (rangeMax - rangeMin) * randomValue;
                //double newLTP = Double.valueOf(randomRangeValue);
                BigDecimal bd = new BigDecimal(randomRangeValue).setScale(2, RoundingMode.HALF_UP);
                double newLTP = bd.doubleValue();
                foundTrade.setLastPric(newLTP);
                setPriceChange(foundTrade);

                updatedScore = ZonedDateTime.of(foundTrade.getTradDt().plusSeconds(30), ZoneId.systemDefault()).toInstant().toEpochMilli();

                unifiedJedis.zadd(foundTrade.getTckrSymb(),
                        updatedScore,
                        gson.toJson(foundTrade),
                        new ZAddParams().lt());

                //log.info("CreateTradeTask {} {} LTP old {} new {} scrore {} ", symbol, dateString, oldLTP, newLTP, updatedScore);


            } else {
                //log.error("CreateTradeTask unable to add trade.No trade found to duplicate for the given day. {} {}", symbol, dateString);
            }
            //return updatedScore;
        }
    }

    @RequiredArgsConstructor
    public class DeleteTradeTask implements Runnable {
        final String symbol;
        final String dateString;

        @Override
        public void run() {
            double toDeleteScore = parseDate(dateString,30);

            double deletedScore = unifiedJedis.zremrangeByScore(symbol, toDeleteScore, toDeleteScore);
            //if (deletedScore != 0.0)
                //log.info("DeleteTradeTask {} {} Score to deletescore {} returned score {}", symbol, dateString, toDeleteScore, deletedScore);

            //return deletedScore;
        }
    }
}
