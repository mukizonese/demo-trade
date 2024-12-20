package com.tradingzone.services.load.reader;

import com.tradingzone.services.load.processor.Trade;

public class NonEqTradeException extends RuntimeException{

    public NonEqTradeException(Trade item){
        //System.out.println("Skipping "+ item.getTckrSymb() + " " + item.getSctySrs());
    }

}
