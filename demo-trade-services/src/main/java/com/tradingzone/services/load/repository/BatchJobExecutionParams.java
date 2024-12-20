package com.tradingzone.services.load.repository;



import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity

@Table(name = "BATCH_JOB_EXECUTION_PARAMS")
@Data
@NoArgsConstructor
public class BatchJobExecutionParams {

    private @Id Integer jobExecutionId ;
    private String parameterName ;
    private String parameterType ;
    private String parameterValue ;
    private String identifying ;

}
