package com.techroots.pdf.service;


import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

@ApplicationScoped
public class AwsS3Service {

    public void saveFile(String fileName, byte[] bytes) {
        String awsRegion = Optional.ofNullable(System.getenv("AWS_REGION")).orElse("us-east-1");
        S3Client s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(System.getenv("PDF_BUCKET_NAME"))
                .key(fileName)
                .build(), RequestBody.fromBytes(bytes));
    }
}
