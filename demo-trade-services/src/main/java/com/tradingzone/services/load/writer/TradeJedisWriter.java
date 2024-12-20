package com.tradingzone.services.load.writer;

import com.google.gson.Gson;
import com.tradingzone.services.load.processor.Trade;
import com.tradingzone.services.redis.repositories.TradeCache;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.repositories.TradeRedisRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Locale;
import java.util.Map;

public class TradeJedisWriter implements ItemWriter<Trade> {

    @Autowired
    private UnifiedJedis unifiedJedis;

    @Autowired
    private Gson gson;


    @Override
    public void write(Chunk<? extends Trade> chunk) throws Exception {

        chunk.forEach(trade -> {
            TradeJedisCache tradeCache = null;
            try {
                tradeCache =  processCacheEntity(trade);

                unifiedJedis.hset("Trades",tradeCache.getTckrSymb(), gson.toJson(tradeCache.getTradDt()));

                unifiedJedis.zadd("TradeDates",
                        ZonedDateTime.of(tradeCache.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        gson.toJson(tradeCache.getTradDt()),
                        new ZAddParams().lt());


                unifiedJedis.zadd(tradeCache.getTckrSymb(),
                        ZonedDateTime.of(tradeCache.getTradDt(), ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        gson.toJson(tradeCache),
                        new ZAddParams().lt());


            } catch (Exception e) {
                //throw new RuntimeException(e);
                e.printStackTrace();
            }

        });
    }


    private TradeJedisCache processCacheEntity(Trade item) throws Exception {

        //System.out.println("Trade item "+ item.toString());

        TradeJedisCache tradeCache = new TradeJedisCache();

        tradeCache.setTradDt(parseDate(item.getTradDt()));
        tradeCache.setBizDt(parseDate(item.getBizDt()));
        tradeCache.setSgmt(item.getSgmt());
        tradeCache.setSrc(item.getSrc());
        tradeCache.setFinInstrmTp(item.getFinInstrmTp());
        tradeCache.setFinInstrmId(parseInt(item.getFinInstrmId()));
        tradeCache.setISIN(item.getISIN());
        tradeCache.setTckrSymb(item.getTckrSymb());
        tradeCache.setSctySrs(item.getSctySrs());
        tradeCache.setXpryDt(parseDate(item.getXpryDt()));
        tradeCache.setFininstrmActlXpryDt(parseDate(item.getFininstrmActlXpryDt()));
        tradeCache.setStrkPric(parseDouble(item.getStrkPric()));
        tradeCache.setOptnTp(item.getOptnTp());
        tradeCache.setFinInstrmNm(item.getFinInstrmNm());
        tradeCache.setOpnPric(parseDouble(item.getOpnPric()));
        tradeCache.setHghPric(parseDouble(item.getHghPric()));
        tradeCache.setLwPric(parseDouble(item.getLwPric()));
        tradeCache.setClsPric(parseDouble(item.getClsPric()));
        tradeCache.setLastPric(parseDouble(item.getLastPric()));
        tradeCache.setPrvsClsgPric(parseDouble(item.getPrvsClsgPric()));
        tradeCache.setUndrlygPric(parseDouble(item.getUndrlygPric()));
        tradeCache.setSttlmPric(parseDouble(item.getSttlmPric()));

        setPriceChange(tradeCache);

        tradeCache.setOpnIntrst(item.getOpnIntrst());
        tradeCache.setChngInOpnIntrst(item.getChngInOpnIntrst());
        tradeCache.setTtlTradgVol(parseBigInt(item.getTtlTradgVol()));
        tradeCache.setTtlTrfVal(parseDouble(item.getTtlTrfVal()));
        tradeCache.setTtlNbOfTxsExctd(item.getTtlNbOfTxsExctd());
        tradeCache.setSsnId(item.getSsnId());
        tradeCache.setNewBrdLotQty(parseInt(item.getNewBrdLotQty()));
        tradeCache.setRmks(item.getRmks());
        tradeCache.setRsvd1(item.getRsvd1());
        tradeCache.setRsvd2(item.getRsvd2());
        tradeCache.setRsvd3(item.getRsvd3());
        tradeCache.setRsvd4(item.getRsvd4());
        return tradeCache;
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

    private LocalDateTime parseDate(String dateString) {

        LocalDateTime timestamp = null;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);

        try{

            //LocalDate locDate = LocalDate.parse(dateString + " 00:00:00", formatter);
            //timestamp = Timestamp.valueOf(locDate.atStartOfDay());


            timestamp = LocalDateTime.parse(dateString + " 00:00:00", formatter);
            //timestamp = Timestamp.valueOf(dateTime);


            //LocalDateTime localDateTime = LocalDateTime.from(formatter.parse(dateString));
            //timestamp = Timestamp.valueOf(localDateTime);
        }catch(Exception pex){
            return null;
        }
        return timestamp;
    }

    private Double parseDouble(String doubleString) {
        if(doubleString.isEmpty()){
            return null;
        }else{
            return Double.parseDouble(doubleString);
        }
    }

    private Integer parseInt(String intString) {
        if(intString.isEmpty()){
            return null;
        }else{
            return Integer.parseInt(intString);
        }
    }

    private BigInteger parseBigInt(String intString) {
        if(intString.isEmpty()){
            return null;
        }else{
            return new BigInteger(intString);
        }
    }

}
