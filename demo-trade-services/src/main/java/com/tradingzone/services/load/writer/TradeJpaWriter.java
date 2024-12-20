package com.tradingzone.services.load.writer;

import com.tradingzone.services.trades.repositories.TradeEntity;
import com.tradingzone.services.trades.repositories.TradeJpaRepository;
import com.tradingzone.services.load.processor.Trade;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class TradeJpaWriter implements ItemWriter<Trade> {

    @Autowired
    TradeJpaRepository tradeJpaRepository;

    @Override
    public void write(Chunk<? extends Trade> chunk) throws Exception {

        chunk.forEach(trade -> {
            TradeEntity tradeEntity = null;
            try {
                tradeEntity = tradeEntity = processDBEntity(trade);
                tradeJpaRepository.save(tradeEntity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
    }


    private TradeEntity processDBEntity(Trade item) throws Exception {

        //System.out.println("Trade item "+ item.toString());

        TradeEntity tradeEntity = new TradeEntity();

        tradeEntity.setTradDt(parseDate(item.getTradDt()));
        tradeEntity.setBizDt(parseDate(item.getBizDt()));
        tradeEntity.setSgmt(item.getSgmt());
        tradeEntity.setSrc(item.getSrc());
        tradeEntity.setFinInstrmTp(item.getFinInstrmTp());
        tradeEntity.setFinInstrmId(parseInt(item.getFinInstrmId()));
        tradeEntity.setISIN(item.getISIN());
        tradeEntity.setTckrSymb(item.getTckrSymb());
        tradeEntity.setSctySrs(item.getSctySrs());
        tradeEntity.setXpryDt(parseDate(item.getXpryDt()));
        tradeEntity.setFininstrmActlXpryDt(parseDate(item.getFininstrmActlXpryDt()));
        tradeEntity.setStrkPric(parseDouble(item.getStrkPric()));
        tradeEntity.setOptnTp(item.getOptnTp());
        tradeEntity.setFinInstrmNm(item.getFinInstrmNm());
        tradeEntity.setOpnPric(parseDouble(item.getOpnPric()));
        tradeEntity.setHghPric(parseDouble(item.getHghPric()));
        tradeEntity.setLwPric(parseDouble(item.getLwPric()));
        tradeEntity.setClsPric(parseDouble(item.getClsPric()));
        tradeEntity.setLastPric(parseDouble(item.getLastPric()));
        tradeEntity.setPrvsClsgPric(parseDouble(item.getPrvsClsgPric()));
        tradeEntity.setUndrlygPric(parseDouble(item.getUndrlygPric()));
        tradeEntity.setSttlmPric(parseDouble(item.getSttlmPric()));
        tradeEntity.setOpnIntrst(item.getOpnIntrst());
        tradeEntity.setChngInOpnIntrst(item.getChngInOpnIntrst());
        tradeEntity.setTtlTradgVol(parseBigInt(item.getTtlTradgVol()));
        tradeEntity.setTtlTrfVal(parseDouble(item.getTtlTrfVal()));
        tradeEntity.setTtlNbOfTxsExctd(item.getTtlNbOfTxsExctd());
        tradeEntity.setSsnId(item.getSsnId());
        tradeEntity.setNewBrdLotQty(parseInt(item.getNewBrdLotQty()));
        tradeEntity.setRmks(item.getRmks());
        tradeEntity.setRsvd1(item.getRsvd1());
        tradeEntity.setRsvd2(item.getRsvd2());
        tradeEntity.setRsvd3(item.getRsvd3());
        tradeEntity.setRsvd4(item.getRsvd4());
        return tradeEntity;
    }

    private Timestamp parseDate2(String dateString) {

        Timestamp timestamp;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        // Define the date format of the input string
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        try{
            Date parsedDate = dateFormat.parse(dateString);

            // Convert java.util.Date to java.sql.Timestamp
            timestamp = new Timestamp(parsedDate.getTime());

        }catch(Exception pex){
            return null;
        }
        return timestamp;
    }

    private Timestamp parseDate(String dateString) {

        Timestamp timestamp;
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);

        try{

            LocalDate locDate = LocalDate.parse(dateString + " 00:00:00", formatter);
            timestamp = Timestamp.valueOf(locDate.atStartOfDay());


            //LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
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
