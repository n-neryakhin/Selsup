package servlets;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CrptApi extends HttpServlet {

    private final TimeUnit timeUnit;
    private int timeUnitSeconds;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final Map<UUID, LocalDateTime> threadStarts = new ConcurrentHashMap<>();

    public CrptApi(TimeUnit tUnit, int reqLimit) {
        if (tUnit == TimeUnit.MICROSECONDS || tUnit == TimeUnit.MILLISECONDS || tUnit == TimeUnit.NANOSECONDS) {
            throw new RuntimeException("Time unit " + tUnit + " is not allowed!");
        }
        if (reqLimit < 0) {
            throw new RuntimeException("Request limit must be > 0!");
        }

        timeUnit = tUnit;
        switch(timeUnit) {
            case SECONDS -> timeUnitSeconds = 1;
            case MINUTES -> timeUnitSeconds = 60;
            case HOURS -> timeUnitSeconds = 3600;
            case DAYS -> timeUnitSeconds = 86400;
        }

        requestLimit = reqLimit;
        semaphore = new Semaphore(requestLimit);

        RefreshPermitsTask taskRefreshPermits = new RefreshPermitsTask();
        Timer timerRefreshPermits = new Timer();
        timerRefreshPermits.schedule(taskRefreshPermits, 0, 500);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Можно было бы использовать класс TimedSemaphore из Apache Commons, но как я понял из задания, сторонние библиотеки нельзя было использовать.
        LocalDateTime threadStart = LocalDateTime.now();
        UUID threadUUID = UUID.randomUUID();
        threadStarts.put(threadUUID, threadStart);

        String jsonDocument = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Document document = new Document();

        // Удобнее было бы сделать, чтобы метод возвращал Document, а не принимал его в качестве аргумента, но в задании было написано
        // "Документ и подпись должны передаваться в метод в виде Java объекта и строки соответственно." Возможно, я неправильно понял задание.
        fillDocument(document, jsonDocument);

        if (threadStarts.containsKey(threadUUID)) {
            threadStarts.remove(threadUUID);
            semaphore.release();
        }
    }

    private void fillDocument(Document document, String jsonDocument) {
        Gson gson = new Gson();
        Document documentJson = gson.fromJson(jsonDocument, Document.class);

        document.setDescription(documentJson.getDescription());
        document.setDoc_id(documentJson.getDoc_id());
        document.setDoc_status(documentJson.getDoc_status());
        document.setDoc_type(documentJson.getDoc_type());
        document.setImportRequest(documentJson.getImportRequest());
        document.setOwner_inn(documentJson.getOwner_inn());
        document.setProducer_inn(documentJson.getProducer_inn());
        document.setParticipant_inn(documentJson.getParticipant_inn());
        document.setProduction_date(documentJson.getProduction_date());
        document.setProduction_type(documentJson.getProduction_type());
        document.setProducts(documentJson.getProducts());
        document.setReg_date(documentJson.getReg_date());
        document.setReg_number(documentJson.getReg_number());
    }

    class RefreshPermitsTask extends TimerTask {

        @Override
        public void run() {
            Map.Entry<UUID, LocalDateTime> entry;
            LocalDateTime start;
            LocalDateTime startPlus;
            LocalDateTime now = LocalDateTime.now();
            var iterator = threadStarts.entrySet().iterator();

            while (iterator.hasNext()) {
                entry = iterator.next();
                start = entry.getValue();
                startPlus = start.plusSeconds(timeUnitSeconds);
                if (startPlus.isBefore(now)) {
                    try {
                        iterator.remove();
                    } catch (IllegalStateException e) {
                        continue;
                    }
                    semaphore.release();
                }
            }
        }

    }

    public static class Document {

        private Participant description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private Boolean importRequest;
        private String owner_inn;
        private String producer_inn;
        private String participant_inn;
        private Date production_date;
        private String production_type;
        private Product[] products;
        private Date reg_date;
        private String reg_number;

        public Document() {
        }

        public Participant getDescription() {
            return description;
        }

        public void setDescription(Participant description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public Boolean getImportRequest() {
            return importRequest;
        }

        public void setImportRequest(Boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public Date getProduction_date() {
            return production_date;
        }

        public void setProduction_date(Date production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public Product[] getProducts() {
            return products;
        }

        public void setProducts(Product[] products) {
            this.products = products;
        }

        public Date getReg_date() {
            return reg_date;
        }

        public void setReg_date(Date reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }

    }

    public static class Participant {

        private String participantInn;

    }

    public static class Product {

        private String certificate_document;
        private Date certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private Date production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

    }

}