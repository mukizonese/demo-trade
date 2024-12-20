package com.tradingzone.services.load.reader.zipextra;

import org.apache.commons.io.FileUtils;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MyZipFileCsvFlatFileItemReader<Trade> extends ZipFileCsvFlatFileItemReader{

    public MyZipFileCsvFlatFileItemReader(String filePath ){
        super();

        try (ZipFile zipFile = new ZipFile(filePath)) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Check if entry is a directory
                if (!entry.isDirectory()) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        // Read and process the entry contents using the inputStream
                        //this.setResource(new InputStreamResource(inputStream));
                        //ZipResource zipResource = new ZipResource(new InputStreamResource(inputStream));
                        //this.setResource(zipResource);
                        File file = new File("tmp.csv");
                        FileUtils.copyInputStreamToFile(inputStream, file);
                        this.setResource(new FileSystemResource(file));
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


        //this.setResource(new ClassPathResource(filePath));
        //this.setResource(new FileSystemResource(filePath));

        this.setLinesToSkip(1);


        this.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter(DELIMITER_COMMA);
                setNames("TradDt", "BizDt", "Sgmt", "Src", "FinInstrmTp", "FinInstrmId", "ISIN", "TckrSymb", "SctySrs", "XpryDt", "FininstrmActlXpryDt", "StrkPric", "OptnTp", "FinInstrmNm", "OpnPric", "HghPric", "LwPric", "ClsPric", "LastPric", "PrvsClsgPric", "UndrlygPric", "SttlmPric", "OpnIntrst", "ChngInOpnIntrst", "TtlTradgVol", "TtlTrfVal", "TtlNbOfTxsExctd", "SsnId", "NewBrdLotQty", "rmks", "rsvd1", "rsvd2", "rsvd3", "rsvd4");
                //setStrict(false);
            }});

            setFieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                setTargetType((Class<? extends Trade>) com.tradingzone.services.load.processor.Trade.class);
                //setTargetType(Trade.class);
            }});
        }});

        this.setLineMapper(new PassThroughLineMapper());

        //reader.setRecordSeparatorPolicy(new BlankLineRecordSeparatorPolicy());
    }
}
