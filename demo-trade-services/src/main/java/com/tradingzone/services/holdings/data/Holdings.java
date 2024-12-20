package com.tradingzone.services.holdings.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@NoArgsConstructor
public class Holdings {

    private BigDecimal totInvestment ;
    private BigDecimal totCurrValue ;
    private BigDecimal totPnl ;
    private BigDecimal totPnlPct ;
    private BigDecimal totNetChng ;
    private BigDecimal totNetChngPct ;
    private BigDecimal totDayChng ;
    private BigDecimal totDayChngPct ;

    private List<HoldingValue> transactionlist;
}
