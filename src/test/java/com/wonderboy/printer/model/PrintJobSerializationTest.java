package com.wonderboy.printer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrintJobSerializationTest {

    @Test
    void testPrintJobSerializationAndDeserialization() throws Exception {
        // 1. Setup ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Important for LocalDateTime

        // 2. Create a sample PrintJob
        PrintSettings settings = PrintSettings.A4_DEFAULT_300_DPI();
        List<String> filePaths = List.of("C:/spool/job123/document.txt");
        PrintJob originalJob = new PrintJob("My First Document", "wonderboy", settings, filePaths);
        originalJob.setStatus(PrintJobStatus.QUEUED);

        // 3. Serialize to JSON string
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(originalJob);
        System.out.println("Serialized JSON:\n" + json);

        // 4. Deserialize back to a PrintJob object
        PrintJob deserializedJob = objectMapper.readValue(json, PrintJob.class);

        // 5. Assert that the deserialized object matches the original
        assertEquals(originalJob.getJobId(), deserializedJob.getJobId());
        assertEquals(originalJob.getDocumentName(), deserializedJob.getDocumentName());
        assertEquals(originalJob.getUser(), deserializedJob.getUser());
        assertEquals(originalJob.getStatus(), deserializedJob.getStatus());
        assertEquals(originalJob.getSubmitTime(), deserializedJob.getSubmitTime());
        assertEquals(originalJob.getSourceFilePaths(), deserializedJob.getSourceFilePaths());

        // Assertions for the nested record object
        assertEquals(originalJob.getSettings().paper(), deserializedJob.getSettings().paper());
        assertEquals(originalJob.getSettings().dpi(), deserializedJob.getSettings().dpi());
        assertEquals(originalJob.getSettings().isColor(), deserializedJob.getSettings().isColor());
    }
}