package com.techroots.pdf.service;

import com.techroots.pdf.model.Expense;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


@ApplicationScoped
public class DocumentUploadService {

    @Inject
    AwsS3Service awsS3Service;
    @Inject
    ExpenseAnalyserService expenseAnalyserService;

    @SneakyThrows
    public Expense uploadFile(MultipartFormDataInput input) {
        for (InputPart inputPart : input.getFormDataMap().get("file")) {
            String documentUrl = uploadFile(inputPart);
            return expenseAnalyserService.analyseDocument(documentUrl);
        }
        return null;
    }

    private String uploadFile(InputPart inputPart) throws IOException {
        InputStream inputStream = inputPart.getBody(InputStream.class, null);
        String fileName = getFileName(inputPart.getHeaders());
        writeFile(inputStream, fileName);
        return fileName;
    }

    private void writeFile(InputStream inputStream,String fileName)
            throws IOException {
        byte[] bytes = IOUtils.toByteArray(inputStream);
        awsS3Service.saveFile(fileName, bytes);
    }


    private String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.
                getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return UUID.randomUUID().toString();
    }
}
