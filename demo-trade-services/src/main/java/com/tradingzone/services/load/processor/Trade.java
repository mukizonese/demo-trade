package com.tradingzone.services.load.processor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.Chunk;

import java.util.function.Consumer;

@Data
@NoArgsConstructor
public class Trade {

    private String tradDt ;
    private String bizDt ;
    private String sgmt ;
    private String src ;
    private String finInstrmTp ;
    private String finInstrmId ;
    private String iSIN ;
    private String tckrSymb ;
    private String sctySrs ;
    private String xpryDt ;
    private String fininstrmActlXpryDt ;
    private String strkPric ;
    private String optnTp ;
    private String finInstrmNm ;
    private String opnPric ;
    private String hghPric ;
    private String lwPric ;
    private String clsPric ;
    private String lastPric ;
    private String prvsClsgPric ;
    private String undrlygPric ;
    private String sttlmPric ;
    private String opnIntrst ;
    private String chngInOpnIntrst ;
    private String ttlTradgVol ;
    private String ttlTrfVal ;
    private String ttlNbOfTxsExctd ;
    private String ssnId ;
    private String newBrdLotQty ;

    private String rmks ;
    private String rsvd1 ;
    private String rsvd2 ;
    private String rsvd3 ;
    private String rsvd4 ;

}
