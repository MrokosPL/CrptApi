package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final Semaphore semaphore;
    private final long intervalInMillis;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.semaphore = new Semaphore(requestLimit);
        this.intervalInMillis = timeUnit.toMillis(1);
    }

    public void createDocument(Document document, String signature) throws InterruptedException {
        semaphore.acquire();
        try {
            sendRequest(document, signature);
        } finally {
            schedulePermitRelease();
        }
    }

    private void sendRequest(Document document, String signature) {
        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String jsonDocument = objectMapper.writeValueAsString(document);

            StringEntity entity = new StringEntity(jsonDocument);
            httpPost.setEntity(entity);

            client.execute(httpPost);

            System.out.println("Request sent: " + jsonDocument);
            System.out.println("Signature: " + signature);
        } catch (IOException e) {
            System.out.println("ОШИБКААААА");
        }
    }

    private void schedulePermitRelease() {
        new Thread(() -> {
            try {
                Thread.sleep(intervalInMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                semaphore.release();
            }
        }).start();
    }

    @Data
    public static class Document {
        @JsonProperty("description")
        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private String docType;

        @JsonProperty("importRequest")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private LocalDate productionDate;

        @JsonProperty("production_type")
        private String productionType;

        @JsonProperty("products")
        private List<Product> products;

        @JsonProperty("reg_date")
        private LocalDate regDate;

        @JsonProperty("reg_number")
        private String regNumber;
    }
    @Data
    public static class Description {
        @JsonProperty("participantInn")
        private String participantInn;
    }
    @Data
    public static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;

        @JsonProperty("certificate_document_date")
        private LocalDate certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private LocalDate productionDate;

        @JsonProperty("tnved_code")
        private String tnvedCode;

        @JsonProperty("uit_code")
        private String uitCode;

        @JsonProperty("uitu_code")
        private String uituCode;
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 2);

        CrptApi.Document document = new CrptApi.Document();
        document.setDocId("doc123");
        document.setDocStatus("draft");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("1234567890");
        document.setParticipantInn("0987654321");
        document.setProducerInn("1357924680");
        document.setProductionDate(LocalDate.now());
        document.setProductionType("production");

        CrptApi.Description description = new CrptApi.Description();
        description.setParticipantInn("0987654321");
        document.setDescription(description);

        CrptApi.Product product = new CrptApi.Product();
        product.setCertificateDocument("cert123");
        product.setCertificateDocumentDate(LocalDate.now());
        product.setCertificateDocumentNumber("cert-num-123");
        product.setOwnerInn("1234567890");
        product.setProducerInn("1357924680");
        product.setProductionDate(LocalDate.now());
        product.setTnvedCode("tnved123");
        product.setUitCode("uit123");
        product.setUituCode("uitu123");

        document.setProducts(List.of(product));

        document.setRegDate(LocalDate.now());
        document.setRegNumber("reg123");


        String signature = "test-signature";

        // тестс
        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + index + " starting request");
                    api.createDocument(document, signature);
                    System.out.println("Thread " + index + " finished request");
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }).start();
        }
    }
}
