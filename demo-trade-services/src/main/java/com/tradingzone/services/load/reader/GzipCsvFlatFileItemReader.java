package com.tradingzone.services.load.reader;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class GzipCsvFlatFileItemReader<Trade> extends FlatFileItemReader {

    public GzipCsvFlatFileItemReader(String filePath ){
        super();

        try {
            this.setResource(new GZIPResource(new ClassPathResource(filePath)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.setLinesToSkip(1);

        this.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter(DELIMITER_COMMA);
                setNames("TradDt", "BizDt", "Sgmt", "Src", "FinInstrmTp", "FinInstrmId", "ISIN", "TckrSymb", "SctySrs", "XpryDt", "FininstrmActlXpryDt", "StrkPric", "OptnTp", "FinInstrmNm", "OpnPric", "HghPric", "LwPric", "ClsPric", "LastPric", "PrvsClsgPric", "UndrlygPric", "SttlmPric", "OpnIntrst", "ChngInOpnIntrst", "TtlTradgVol", "TtlTrfVal", "TtlNbOfTxsExctd", "SsnId", "NewBrdLotQty", "rmks", "rsvd1", "rsvd2", "rsvd3", "rsvd4");
            }});

            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType((Class<? extends Trade>) com.tradingzone.services.load.processor.Trade.class);
                //setTargetType(Trade.class);
            }});
        }});

        //reader.setRecordSeparatorPolicy(new BlankLineRecordSeparatorPolicy());
    }

}

class GZIPResource extends InputStreamResource implements Resource {

    public GZIPResource(Resource delegate) throws IOException {
        super(new GZIPInputStream(delegate.getInputStream()));
    }
}
