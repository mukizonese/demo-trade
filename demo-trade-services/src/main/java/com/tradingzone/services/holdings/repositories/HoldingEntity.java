package com.tradingzone.services.holdings.repositories;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "HOLDINGS")
@IdClass(HoldingEntityKey.class)
@Data
@NoArgsConstructor
public class HoldingEntity {

    private @Id Integer id ;
    private @Id Integer usrId ;
    private @Id String tckrSymb ;
    private Double pric ;
    private Integer qty ;
    private @Id Timestamp tradDt ;
    private String action ;
}
class HoldingEntityKey implements Serializable {
    private Integer id ;
    private Integer usrId ;
    private String tckrSymb;
    private Timestamp tradDt;

}