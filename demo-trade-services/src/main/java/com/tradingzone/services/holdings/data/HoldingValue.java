package com.tradingzone.services.holdings.data;

import com.tradingzone.services.holdings.repositories.HoldingEntity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
public class HoldingValue {

    private String id ;
    private String tckrSymb ;
    //private Integer finInstrmId ;
    private Integer avgQty ;
    private BigDecimal avgCost ;
    private BigDecimal totCost ;
    private Double lastPric ;
    private Double prvsClsgPric ;
    private BigDecimal chngePric ;
    private BigDecimal currValue ;
    private BigDecimal pnl ;
    //private Double pnlPct ;
    private BigDecimal netChng ;
    private BigDecimal netChngPct ;
    private BigDecimal dayChng ;
    private BigDecimal dayChngPct ;

    private List<HoldingEntity> transactionlist;
}
