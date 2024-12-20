package com.tradingzone.services.load.processor;

import com.tradingzone.services.load.reader.NonEqTradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

public class TradeItemProcessor implements ItemProcessor<Trade, Trade> {

    private static final Logger log = LoggerFactory.getLogger(TradeItemProcessor.class);

    @Override
    public Trade process(Trade item) throws Exception {

        if (!"EQ".equals(item.getSctySrs())) {
            throw new NonEqTradeException(item);
        }
        return item;
    }

}
