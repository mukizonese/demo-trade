package com.tradingzone.services.load.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitAwsJob {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    Job myJob;

    @Autowired
    private Environment environment;

    @Value("${init.job.s3.override}")
    private boolean jobOverride;

    private final S3Client amazonS3;

    private String bucketName = "muki-trade-bucket";
    private String path = "/tmp/";

    public String loadDataAll(){

        JobParametersBuilder jobParametersBuilder;
        String status = "FAILURE";
        path = getLocalPath(path);
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(1)
                .build();

        ListObjectsV2Iterable listRes = amazonS3.listObjectsV2Paginator(listObjectsV2Request);
        listRes.stream()
                .flatMap(r -> r.contents().stream())
                .forEach(content -> {
                    System.out.println(" Key: " + content.key() + " size = " + content.size());

                    String filepath = path+content.key();
                    System.out.println(" filepath: " + filepath );

                        getObjectBytes(amazonS3, bucketName, content.key(), filepath);

                        try {
                            jobLauncher.run(myJob, getJobParametersBuilder(filepath).toJobParameters());
                            Path fpath = Paths.get(filepath);
                            Files.deleteIfExists(fpath);
                        } catch (JobExecutionAlreadyRunningException e) {
                            throw new RuntimeException(e);
                        } catch (JobRestartException e) {
                            throw new RuntimeException(e);
                        } catch (JobInstanceAlreadyCompleteException e) {
                            throw new RuntimeException(e);
                        } catch (JobParametersInvalidException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                });
        status = "SUCCESS";
        return status;
    }


    public String loadData(List<String> files){

        JobParametersBuilder jobParametersBuilder;
        String status = "FAILURE";

        if(files != null && !files.isEmpty() ){
            Map<String, String> fileMap = new HashMap<>();
            for (String file : files) {
                fileMap.put(file, file);
            }


            path = getLocalPath(path);

            fileMap.forEach((k, v) ->
                    {
                        //logger.info("Key: {}, Value: {}", k, v)
                        System.out.println(" Key: " + k + " Value = " + v);

                        String filepath = path+k;
                        //String filepath = content.key();
                        System.out.println(" filepath: " + filepath );

                        getObjectBytes(amazonS3, bucketName, v, filepath);

                        try {
                            jobLauncher.run(myJob, getJobParametersBuilder(filepath).toJobParameters());
                            Path fpath = Paths.get(filepath);
                            Files.deleteIfExists(fpath);
                        } catch (JobExecutionAlreadyRunningException e) {
                            throw new RuntimeException(e);
                        } catch (JobRestartException e) {
                            throw new RuntimeException(e);
                        } catch (JobInstanceAlreadyCompleteException e) {
                            throw new RuntimeException(e);
                        } catch (JobParametersInvalidException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }
            );

        }

        status = "SUCCESS";
        return status;
    }

    private String getLocalPath(String path){
        //Check if Active profiles contains "local" or "test"
        if(Arrays.stream(environment.getActiveProfiles()).anyMatch(
                env -> (env.equalsIgnoreCase("test")
                        || env.equalsIgnoreCase("local")) ))
        {
            path = "";
        }

        return path;
    }

    /**
     * Retrieves the bytes of an object stored in an Amazon S3 bucket and saves them to a local file.
     *
     * @param s3 The S3Client instance used to interact with the Amazon S3 service.
     * @param bucketName The name of the S3 bucket where the object is stored.
     * @param keyName The key (or name) of the S3 object.
     * @param path The local file path where the object's bytes will be saved.
     * @throws IOException If an I/O error occurs while writing the bytes to the local file.
     * @throws S3Exception If an error occurs while retrieving the object from the S3 bucket.
     */
    private void getObjectBytes(S3Client s3, String bucketName, String keyName, String path) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3.getObject(objectRequest, ResponseTransformer.toBytes());
            byte[] data = objectBytes.asByteArray();

            // Write the data to a local file.
            File myFile = new File(path);
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            System.out.println("Successfully obtained bytes from an S3 object");
            os.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            //System.exit(1);
        }
    }

    private static JobParametersBuilder getJobParametersBuilder(String filePath) {
        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString("filePath", filePath);
        jobParametersBuilder.addLong("time",System.currentTimeMillis()).toJobParameters();
        return jobParametersBuilder;
    }


}
