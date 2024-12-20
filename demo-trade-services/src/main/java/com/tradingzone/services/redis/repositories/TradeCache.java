package com.tradingzone.services.redis.repositories;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@RedisHash("Trades")
@NoArgsConstructor
public class TradeCache {

    private LocalDateTime tradDt ;
    private LocalDateTime bizDt ;
    private String sgmt ;
    private String src ;
    private String finInstrmTp ;
    private Integer finInstrmId ;
    private String iSIN ;
    private @Id String tckrSymb ;
    private String sctySrs ;
    private LocalDateTime xpryDt ;
    private LocalDateTime fininstrmActlXpryDt ;
    private Double strkPric ;
    private String optnTp ;
    private String finInstrmNm ;
    private Double opnPric ;
    private Double hghPric ;
    private Double lwPric ;
    private Double clsPric ;
    private Double lastPric ;
    private Double prvsClsgPric ;
    private Double undrlygPric ;
    private Double sttlmPric ;
    private String opnIntrst ;
    private String chngInOpnIntrst ;
    private Integer ttlTradgVol ;
    private Double ttlTrfVal ;
    private String ttlNbOfTxsExctd ;
    private String ssnId ;
    private Integer newBrdLotQty ;
    private String rmks ;
    private String rsvd1 ;
    private String rsvd2 ;
    private String rsvd3 ;
    private String rsvd4 ;

}
