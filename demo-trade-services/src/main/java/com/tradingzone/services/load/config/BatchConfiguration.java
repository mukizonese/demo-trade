package com.tradingzone.services.load.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tradingzone.services.load.reader.AwsZipCsvFlatFileItemReader;
import com.tradingzone.services.load.reader.NonEqTradeException;
import com.tradingzone.services.load.job.TradeJobListener;
import com.tradingzone.services.load.processor.Trade;
import com.tradingzone.services.load.processor.TradeItemProcessor;
import com.tradingzone.services.load.reader.CsvFlatFileItemReader;
import com.tradingzone.services.load.reader.ZipCsvFlatFileItemReader;
import com.tradingzone.services.load.reader.zipextra.MyZipFileCsvFlatFileItemReader;
import com.tradingzone.services.load.util.LocalDateTimeTypeAdapter;
import com.tradingzone.services.load.writer.TradeJedisWriter;
import com.tradingzone.services.load.writer.TradeJpaWriter;
import org.springframework.batch.core.*;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    JobParameters jobParameters ;

    @Bean
    @StepScope
    public MyZipFileCsvFlatFileItemReader<Trade> awszipreader(@Value("#{jobParameters}") Map jobParameters) throws Exception {

        String filePath = String.valueOf(jobParameters.get("filePath"));
        return new MyZipFileCsvFlatFileItemReader<Trade>(filePath);
    }

    @Bean
    @StepScope
    public ZipCsvFlatFileItemReader<Trade> zipreader(@Value("#{jobParameters}") Map jobParameters) throws Exception {

        String filePath = String.valueOf(jobParameters.get("filePath"));
        return new ZipCsvFlatFileItemReader<Trade>(filePath);
    }

    @Bean
    @StepScope
    public CsvFlatFileItemReader<Trade> csvreader(@Value("#{jobParameters}") Map jobParameters) throws Exception {

        String filePath = String.valueOf(jobParameters.get("filePath"));
        return new CsvFlatFileItemReader<Trade>(filePath);
    }

    @Bean
    public TradeItemProcessor processor() {
        return new TradeItemProcessor();
    }

    @Bean
    public ItemWriter<Trade> writerDB(){
        return new TradeJpaWriter();
    }

    @Bean
    public Gson gson(){
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }

    @Bean
    public ItemWriter<Trade> writerCache(){

        //return new TradeCacheWriter();
        return new TradeJedisWriter();
    }

    @Bean
    public CompositeItemWriter<Trade> compositeItemWriter(){

        CompositeItemWriter<Trade> writer = new CompositeItemWriter<Trade>();
        writer.setDelegates(Arrays.asList(writerDB(), writerCache()));
        return writer;
    }


    /*@Autowired
    TradeJpaRepository tradeJpaRepository;

    @Bean
    public ItemWriter<TradeWriterMap> writerDB(){
        // return new InvoiceItemWriter();
        // Using lambda expression code instead of a separate implementation
        return trades -> {
            System.out.println("Saving Invoice Records: " + trades);
            try {
                tradeJpaRepository.saveAll(trades);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        };
    }*/

   /* @Bean
    public ItemWriter<TradeEntity> writerDB(){
        // return new InvoiceItemWriter();
        // Using lambda expression code instead of a separate implementation
        return trades -> {
            System.out.println("Saving Invoice Records: " + trades);
            try {
                tradeJpaRepository.saveAll(trades);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        };
    }*/

    /*@Bean
    public ItemWriter<TradeWriterMap> writerCache(){
        return new RedisItemWriter<>();
    }*/


    //Listener class Object
    @Bean
    public JobExecutionListener listener() {
        return new TradeJobListener();
    }


    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        JobParameters jobParameters = stepExecution.getJobParameters();

        String path = jobParameters.getString("filePath");
    }

    @Bean
    public Step readCSVStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager) throws Exception {
        TaskletStep sampleStep = new StepBuilder("readCSVStep", jobRepository)
                .<Trade, Trade>chunk(250, transactionManager)
                .reader(zipreader(null))
                .processor(processor())
                .writer(compositeItemWriter())
                //.writer(writerDB())
                //.writer(writerCache())
                .faultTolerant()
                //.skipLimit(1)
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .skip(NonEqTradeException.class)
                .allowStartIfComplete(true)
                .build();
        return sampleStep;
    }

    @Bean
    public Job myJob(JobRepository jobRepository, Step readCSVStep) {
        return new JobBuilder("tradesReadDailyCsvJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .start(readCSVStep)
                .build();
    }


    @Bean
    public Step readAwsCSVStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager) throws Exception {
        TaskletStep sampleAwsStep = new StepBuilder("readAwsCSVStep", jobRepository)
                .<Trade, Trade>chunk(250, transactionManager)
                .reader(awszipreader(null))
                .processor(processor())
                .writer(compositeItemWriter())
                //.writer(writerDB())
                //.writer(writerCache())
                .faultTolerant()
                //.skipLimit(1)
                .skipPolicy(new AlwaysSkipItemSkipPolicy())
                .skip(NonEqTradeException.class)
                //.allowStartIfComplete(true)
                .build();
        return sampleAwsStep;
    }


    @Bean
    public Job myAwsJob(JobRepository jobRepository, Step readAwsCSVStep) {
        return new JobBuilder("tradesReadDailyAwsCsvJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .start(readAwsCSVStep)
                .build();
    }

}
