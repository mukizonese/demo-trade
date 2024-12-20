package com.tradingzone.services.load.reader;

import org.apache.commons.io.FileUtils;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipCsvFlatFileItemReader<Trade> extends FlatFileItemReader<Trade> {

    public ZipCsvFlatFileItemReader(String filePath ){
        super();
        String path = "/tmp/";
        String filepath = path+"tmp.csv";
        try (ZipFile zipFile = new ZipFile(filePath)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if entry is a directory
                if (!entry.isDirectory()) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        // Read and process the entry contents using the inputStream

                        File file = new File(filepath);
                        FileUtils.copyInputStreamToFile(inputStream, file);
                        this.setResource(new FileSystemResource(file));
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        this.setLinesToSkip(1);

        this.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter(DELIMITER_COMMA);
                setNames("TradDt", "BizDt", "Sgmt", "Src", "FinInstrmTp", "FinInstrmId", "ISIN", "TckrSymb", "SctySrs", "XpryDt", "FininstrmActlXpryDt", "StrkPric", "OptnTp", "FinInstrmNm", "OpnPric", "HghPric", "LwPric", "ClsPric", "LastPric", "PrvsClsgPric", "UndrlygPric", "SttlmPric", "OpnIntrst", "ChngInOpnIntrst", "TtlTradgVol", "TtlTrfVal", "TtlNbOfTxsExctd", "SsnId", "NewBrdLotQty", "rmks", "rsvd1", "rsvd2", "rsvd3", "rsvd4");
                setStrict(false);
            }});

            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType((Class<? extends Trade>) com.tradingzone.services.load.processor.Trade.class);
                //setTargetType(Trade.class);
            }});
        }});

        //reader.setRecordSeparatorPolicy(new BlankLineRecordSeparatorPolicy());
    }

}

