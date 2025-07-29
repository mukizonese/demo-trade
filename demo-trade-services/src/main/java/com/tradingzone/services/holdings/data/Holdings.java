package com.tradingzone.services.holdings.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
public class Holdings {

    private BigDecimal totInvestment = BigDecimal.ZERO;
    private BigDecimal totCurrValue = BigDecimal.ZERO;
    private BigDecimal totPnl = BigDecimal.ZERO;
    private BigDecimal totPnlPct = BigDecimal.ZERO;
    private BigDecimal totNetChng = BigDecimal.ZERO;
    private BigDecimal totNetChngPct = BigDecimal.ZERO;
    private BigDecimal totDayChng = BigDecimal.ZERO;
    private BigDecimal totDayChngPct = BigDecimal.ZERO;

    private List<HoldingValue> transactionlist = new ArrayList<>();
}
