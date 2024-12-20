package com.tradingzone.services.trades.repositories;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;

@Entity
@IdClass(TradeEntityKey.class)
@Table(name = "TRADES")
@Data
@NoArgsConstructor
public class TradeEntity {

    private @Id Timestamp tradDt ;
    private Timestamp bizDt ;
    private String sgmt ;
    private String src ;
    private String finInstrmTp ;
    private @Id Integer finInstrmId ;
    private String iSIN ;
    private String tckrSymb ;
    private String sctySrs ;
    private Timestamp xpryDt ;
    private Timestamp fininstrmActlXpryDt ;
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
    private BigInteger ttlTradgVol ;
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

class TradeEntityKey implements Serializable {
    private Timestamp tradDt;
    private Integer finInstrmId ;
}