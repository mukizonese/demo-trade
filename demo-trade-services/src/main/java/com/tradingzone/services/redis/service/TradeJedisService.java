package com.tradingzone.services.redis.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tradingzone.services.load.writer.TradeJedisWriter;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.ZAddParams;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class TradeJedisService {

    @Autowired
    private UnifiedJedis unifiedJedis;

    @Autowired
    private Gson gson;

    //Fetching from Trades hash
    public List<String> getSymbols() {
        List<String> symbols = new ArrayList<String>();
        Map<String,String> symMap = unifiedJedis.hgetAll("Trades");
        if(symMap!=null || !symMap.isEmpty()) {
            for (Map.Entry<String, String> entry : symMap.entrySet()) {
                symbols.add(entry.getKey());
            }
        }else{
            log.error("No Symbols found in Trades cache");
        }

        return symbols;
    }

    public String getLatestTradeDate(){

        String latestDate = unifiedJedis.hget("Trades", "HDFCBANK");
        return latestDate;
    }

    public List<TradeJedisCache> fetchAll(){

        List<TradeJedisCache> trdFinalList = new ArrayList<TradeJedisCache>();

        //double min = parseDate(dateString + " 00:00:00");
        double max = System.currentTimeMillis();

        Map<String,String> symMap = unifiedJedis.hgetAll("Trades");
        if(symMap!=null || !symMap.isEmpty()){
            for (Map.Entry<String, String> entry : symMap.entrySet()){
                List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();
                String dateString = entry.getValue().substring(1, 20);
                double number = parseDate(dateString);
                //List<String> trdList = unifiedJedis.zrevrangeByScore(entry.getKey(), number, Double.parseDouble("1729017000000"));
                List<String> trdList = unifiedJedis.zrevrangeByScore(entry.getKey(), max , number);
                for (String trd : trdList) {
                    TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
                    objectList.add(convertedObject);
                }
                if(objectList.getLast()!=null)
                    trdFinalList.add(objectList.getLast());
            }
        }

        return trdFinalList;

    }

    public List<TradeJedisCache> fetchAll( String cache, String key, String dateString){
        List<TradeJedisCache> trdFinalList = new ArrayList<TradeJedisCache>();

        double min = parseDate(dateString);
        double max = System.currentTimeMillis();
        //log.info("fetchAll before : {} {}  ", cache, key);
        String symbols = unifiedJedis.hget(cache, key);
        //log.info("fetchAll before : {} {} Symbols in Cache {} ", cache, key, symbols);

        String[] symMap = symbols.split(",");

        Map<String,String> symfullMap = unifiedJedis.hgetAll("Trades");
        //log.info("fetchAll getALl Symbols in Trades {} ", symfullMap);

            for (String symb :symMap){
                //List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();
                //String dateString = entry.getValue().substring(1, 20);

                if(dateString.equalsIgnoreCase(gson.fromJson(symfullMap.get(symb), String.class))){
                    try{
                        //rev max min
                        //List<String> trdList = unifiedJedis.zrevrangeByScore(symb, max , min,0,1);
                        //log.info("zrevrangeByScore(symb, max , min) : {} {} {} {}", symb, max, min, dateString);
                        //log.info("ZREVRANGEBYSCORE {} {} {}", symb, max, min);
                        List<String> trdList = unifiedJedis.zrevrangeByScore(symb, max , min);
                        //List<String> trdList = unifiedJedis.zrevrangeByScore(symb, max , min);
                        if(trdList == null){
                            System.out.println("Symbol: " + symb );
                        }else{
                            for (String trd : trdList) {
                                TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
                                trdFinalList.add(convertedObject);
                                //log.info("fetchAll : {} {} found {} ", symb, dateString,  convertedObject);
                                break;
                            }
                        }
                    }catch(Exception ex){
                        //Some symbols data not found on some dates eg DIGIDRIVE avail from 2024-10-08 00:00:00 not before
                        //ex.printStackTrace();
                    }
                }

            }


        return trdFinalList;
    }

    public List<TradeJedisCache> fetchAllByDate( String dateString, boolean live){

        List<TradeJedisCache> trdFinalList = new ArrayList<TradeJedisCache>();

        double min = parseDate(dateString );
        double max = System.currentTimeMillis();
        //List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();

        Map<String,String> symMap = unifiedJedis.hgetAll("Trades");
        if(symMap!=null || !symMap.isEmpty()){
            for (Map.Entry<String, String> entry : symMap.entrySet()){

                try{

                    List<String> trdList = new ArrayList<>();
                    //rev max min
                    //List<String> trdList = unifiedJedis.zrevrangeByScore(entry.getKey(), max , min,0,1);
                    if(live){
                        if(dateString.equalsIgnoreCase(gson.fromJson(entry.getValue(), String.class)) ){
                            trdList = unifiedJedis.zrevrangeByScore(entry.getKey(), max , min);
                        }

                    }else{
                        trdList = unifiedJedis.zrevrangeByScore(entry.getKey(), min , min);
                    }

                    if(trdList == null){
                        System.out.println("Symbol: " + entry.getKey() );
                    }else{
                        for (String trd : trdList) {
                            TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
                            trdFinalList.add(convertedObject);
                            break;
                        }
                    }

                }catch(Exception ex){
                    //Some symbols data not found on some dates eg DIGIDRIVE avail from 2024-10-08 00:00:00 not before
                    //ex.printStackTrace();
                }
            }
        }

        return trdFinalList;

    }

    public List<String> fetchAllTradeDatesByDate(String dateString){
        List<String> objectList = new ArrayList<String>();
        List<String> trdDatesList = new ArrayList<String>();
        double min = parseDate(dateString);
        double max = System.currentTimeMillis();

        try{
            trdDatesList = unifiedJedis.zrangeByScore("TradeDates", min, max);
            for (String trd : trdDatesList) {
                String  convertedDate = gson.fromJson(trd, String.class);
                objectList.add(convertedDate);
            }

        }catch(Exception ex){
            ex.printStackTrace();
        }

        return objectList;
    }



    public TradeJedisCache fetchSymbol(String symbol, String dateString){
        List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();
        double number = parseDate(dateString + " 00:00:00");
        double max = System.currentTimeMillis();

        List<String> trdList = unifiedJedis.zrevrangeByScore(symbol, number, Double.parseDouble("1729017000000"));
        for (String trd : trdList) {
            TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
            objectList.add(convertedObject);
        }
        return objectList.getLast();

    }

    public TradeJedisCache fetchLatestPrice(String symbol){

        double min = 0;
        double max = System.currentTimeMillis();

        List<String> trdList = unifiedJedis.zrevrangeByScore(symbol, max, min);

        if(trdList != null && !trdList.isEmpty()){
            TradeJedisCache convertedObject = gson.fromJson(trdList.getFirst(), TradeJedisCache.class);
            return convertedObject;
        }

        return null;
    }

    public List<TradeJedisCache> fetchSymbolHistory(String symbol,  String timeRange){
        log.info("In fetchSymbolHistory() {} {} ", symbol, timeRange);
        List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();
        double min = parseDate("2024-09-16 00:00:00");

        switch (timeRange){
            case "90d" :  min = parseDateLastXDays( 90); break;
            case "30d" :  min = parseDateLastXDays( 30); break;
            case "7d" :  min = parseDateLastXDays( 7); break;
            default : min = parseDate("2024-09-16 00:00:00");

        }

        double max = System.currentTimeMillis();


        List<String> trdList = unifiedJedis.zrangeByScore(symbol, min, max);
        for (String trd : trdList) {
            TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
            objectList.add(convertedObject);
        }
        return objectList;
    }

    public List<TradeJedisCache> fetchSymbolHistoryRev(String symbol, String dateString){
        List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();
        double min = parseDate(dateString + " 00:00:00");
        double max = System.currentTimeMillis();


        List<String> trdList = unifiedJedis.zrevrangeByScore(symbol, max, min);
        for (String trd : trdList) {
            TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
            objectList.add(convertedObject);
        }
        return objectList;
    }




    public TradeJedisCache setLTPSymbol(String symbol, String dateString) throws Exception {

        TradeJedisCache toUpdateCache = new TradeJedisCache();
        List<TradeJedisCache> objectList = new ArrayList<TradeJedisCache>();

        double min = parseDate(dateString) ;
        double max = System.currentTimeMillis();

        List<String> trdList = unifiedJedis.zrangeByScore(symbol, min, max,0,1);
        for (String trd : trdList) {
            TradeJedisCache convertedObject = gson.fromJson(trd, TradeJedisCache.class);
            objectList.add(convertedObject);
            break;
        }

        if(objectList.getLast()==null){
            System.out.println("Symbol: " + symbol );
            throw new Exception("No trades found for the day");
        }else{
            toUpdateCache =  objectList.getLast();

                Random r = new Random();

            double randomValue = r.nextDouble()*10;
            double oldLTP = toUpdateCache.getLastPric();
            double rangeMin = oldLTP - randomValue;
            double rangeMax = oldLTP + randomValue;
            double randomRangeValue = rangeMin + (rangeMax - rangeMin) * randomValue;
            //double newLTP = Double.valueOf(randomRangeValue);
            BigDecimal bd = new BigDecimal(randomRangeValue).setScale(2, RoundingMode.HALF_UP);
            double newLTP = bd.doubleValue();

            toUpdateCache.setLastPric(newLTP);
            setPriceChange(toUpdateCache);

            double updatedScore =  ZonedDateTime.of(toUpdateCache.getTradDt().plusSeconds(30), ZoneId.systemDefault()).toInstant().toEpochMilli();

            unifiedJedis.zadd(toUpdateCache.getTckrSymb(),
                    updatedScore,
                    gson.toJson(toUpdateCache),
                    new ZAddParams().lt());

        }
        return toUpdateCache;
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

    private long parseDate(String dateString) {

        long timemillis = 0;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);

        try{
            LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime, ZoneId.systemDefault());
            return zonedDateTime.toInstant().toEpochMilli();

        }catch(Exception pex){
            pex.printStackTrace();
        }
        return timemillis;
    }

    private long parseDateLastXDays(long minusDays) {

        long timemillis = 0;
        DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        DateTimeFormatter dateTimeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        try{
            String dateString = LocalDate.now().format(dateformatter) + " 00:00:00";
            LocalDateTime dateTime = LocalDateTime.parse(dateString, dateTimeformatter);
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime.minusDays(minusDays), ZoneId.systemDefault());
            return zonedDateTime.toInstant().toEpochMilli();

        }catch(Exception pex){
            pex.printStackTrace();
        }
        return timemillis;
    }
}
