package com.wonderboy.printer.service;

import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SpoolerServiceTest {

    @TempDir
    Path tempSpoolDir; // JUnit will create and clean up this temporary directory

    private SpoolerService spoolerService;

    @BeforeEach
    void setUp() {
        // A new service instance is created for each test, pointing to a fresh temp directory
        spoolerService = new SpoolerService(tempSpoolDir);
    }

    @Test
    void testSubmitJobCreatesJsonFile() {
        // Arrange
        PrintJob job = new PrintJob("test.txt", "user1", PrintSettings.A4_DEFAULT_300_DPI(), List.of());

        // Act
        spoolerService.submit(job);

        // Assert
        Path expectedFile = tempSpoolDir.resolve(job.getJobId() + ".json");
        assertTrue(Files.exists(expectedFile), "Spooler should create a JSON file for the submitted job.");
        assertEquals(1, spoolerService.listJobs().size(), "Job should be in the in-memory queue.");
    }

    @Test
    void testServiceInitializationLoadsExistingJobs() {
        // Arrange: Manually create a SpoolerService to put some files on disk
        SpoolerService initialService = new SpoolerService(tempSpoolDir);
        PrintJob job1 = new PrintJob("doc1.pdf", "user1", PrintSettings.A4_DEFAULT_300_DPI(), List.of());
        PrintJob job2 = new PrintJob("doc2.png", "user2", PrintSettings.A4_DEFAULT_300_DPI(), List.of());
        initialService.submit(job1);
        initialService.submit(job2);

        // Act: Create a *new* SpoolerService instance. It should load the jobs from the disk.
        SpoolerService restartedService = new SpoolerService(tempSpoolDir);
        List<PrintJob> loadedJobs = restartedService.listJobs();

        // Assert
        assertEquals(2, loadedJobs.size(), "Restarted service should load all jobs from the spool directory.");

        Set<String> loadedJobIds = loadedJobs.stream()
                .map(PrintJob::getJobId)
                .collect(Collectors.toSet());

        assertTrue(loadedJobIds.contains(job1.getJobId()), "Job 1 should be loaded.");
        assertTrue(loadedJobIds.contains(job2.getJobId()), "Job 2 should be loaded.");
    }
}