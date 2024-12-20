package com.tradingzone.services.load.writer;

import com.tradingzone.services.load.processor.Trade;
import com.tradingzone.services.redis.repositories.TradeCache;
import com.tradingzone.services.redis.repositories.TradeRedisRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class TradeCacheWriter implements ItemWriter<Trade> {

    @Autowired
    private TradeRedisRepository tradeRedisRepository;

    @Override
    public void write(Chunk<? extends Trade> chunk) throws Exception {

        chunk.forEach(trade -> {
            TradeCache tradeCache = null;
            try {
                tradeCache =  processCacheEntity(trade);
                tradeRedisRepository.save(tradeCache);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }


    private TradeCache processCacheEntity(Trade item) throws Exception {

        //System.out.println("Trade item "+ item.toString());

        TradeCache tradeCache = new TradeCache();

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
        tradeCache.setOpnIntrst(item.getOpnIntrst());
        tradeCache.setChngInOpnIntrst(item.getChngInOpnIntrst());
        tradeCache.setTtlTradgVol(parseInt(item.getTtlTradgVol()));
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



}
