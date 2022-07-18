package com.techroots.pdf.service;

import com.techroots.pdf.model.Expense;
import org.jboss.logging.Logger;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeExpenseResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.S3Object;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

@ApplicationScoped
public class ExpenseAnalyserService {
    private static final Logger LOG = Logger.getLogger(ExpenseAnalyserService.class);

    public Expense analyseDocument(String uploadFileName) {
        TextractClient textractclient = TextractClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .region(Region.US_EAST_1).build();

        AnalyzeExpenseResponse result = textractclient.analyzeExpense(
                analyseExpenseRequest(uploadFileName));

        return Expense.builder()
                .amountWithTax(total(result).orElse(BigDecimal.ZERO))
                .taxAmount(taxAmount(result).orElse(BigDecimal.ZERO))
                .receiptDate(receiptDate(result).orElse(null))
                .description(description(result).orElse(null))
                .documentUrl(uploadFileName)
                .build();
    }

    private AnalyzeExpenseRequest analyseExpenseRequest(String uploadFileName) {
        return AnalyzeExpenseRequest.builder()
                .document(
                        Document.builder().s3Object(S3Object.builder().name(uploadFileName)
                                .bucket(System.getenv("INVOICE_PDF_BUCKET_NAME")).build()).build())
                .build();
    }

    private Optional<BigDecimal> total(AnalyzeExpenseResponse result) {
        try {
            return result.expenseDocuments().stream()
                    .map(expenseDocument -> expenseDocument.summaryFields())
                    .flatMap(Collection::stream)
                    .filter(expenseField -> expenseField.type().text().equalsIgnoreCase("TOTAL"))
                    .filter(expenseField -> expenseField.type().confidence() > 80)
                    .max(Comparator.comparing(expenseField -> expenseField.type().confidence()))
                    .map(expenseField -> expenseField.valueDetection().text())
                    .map(BigDecimal::new);
        } catch (Exception e) {
            LOG.error("Error while analysing expense total", e);
            return Optional.empty();
        }
    }

    private Optional<String> description(AnalyzeExpenseResponse result) {
        try {
            return result.expenseDocuments().stream()
                    .map(expenseDocument -> expenseDocument.summaryFields())
                    .flatMap(Collection::stream)
                    .filter(expenseField -> expenseField.type().text().equalsIgnoreCase("INVOICE_RECEIPT_ID"))
                    .filter(expenseField -> expenseField.type().confidence() > 80)
                    .max(Comparator.comparing(expenseField -> expenseField.type().confidence()))
                    .map(expenseField -> expenseField.valueDetection().text());
        } catch (Exception e) {
            LOG.error("Error while analysing expense description", e);
            return Optional.empty();
        }
    }

    private Optional<LocalDate> receiptDate(AnalyzeExpenseResponse result) {
        try {
            return result.expenseDocuments().stream()
                    .map(expenseDocument -> expenseDocument.summaryFields())
                    .flatMap(Collection::stream)
                    .filter(expenseField -> expenseField.type().text().equalsIgnoreCase("INVOICE_RECEIPT_DATE"))
                    .filter(expenseField -> expenseField.type().confidence() > 80)
                    .max(Comparator.comparing(expenseField -> expenseField.type().confidence()))
                    .map(expenseField -> expenseField.valueDetection().text())
                    .map(LocalDate::parse);
        } catch (Exception e) {
            LOG.error("Error while analysing expense receiptDate", e);
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> taxAmount(AnalyzeExpenseResponse result) {
        try {
            return result.expenseDocuments().stream()
                    .map(expenseDocument -> expenseDocument.summaryFields())
                    .flatMap(Collection::stream)
                    .filter(expenseField -> expenseField.type().text().equalsIgnoreCase("TAX"))
                    .filter(expenseField -> expenseField.type().confidence() > 80)
                    .max(Comparator.comparing(expenseField -> expenseField.type().confidence()))
                    .map(expenseField -> expenseField.valueDetection().text())
                    .map(BigDecimal::new);
        } catch (Exception e) {
            LOG.error("Error while analysing expense taxAmount", e);
            return Optional.empty();
        }
    }
}
