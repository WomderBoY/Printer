// Replace the entire contents of Main.java with this enhanced version:
package com.wonderboy.printer;

import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintJobStatus;
import com.wonderboy.printer.model.PrintSettings;
import com.wonderboy.printer.printer.VirtualPrinter;
import com.wonderboy.printer.renderer.SimpleTextRenderer;
import com.wonderboy.printer.service.SpoolerService;
import com.wonderboy.printer.service.SpoolerWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Path SPOOL_DIR = Paths.get("spool");
    private static final Path OUTPUT_DIR = Paths.get("output");

    public static void main(String[] args) throws IOException {
        logger.info("--- Virtual Printer Simulation Starting ---");

        // 1. Initialize all core components
        SpoolerService spooler = new SpoolerService(SPOOL_DIR);
        SimpleTextRenderer renderer = new SimpleTextRenderer();
        VirtualPrinter printer = new VirtualPrinter(OUTPUT_DIR);
        SpoolerWorker worker = new SpoolerWorker(spooler, renderer, printer);

        // 2. Display initial state
        logger.info("--- Initial State of Spooler ---");
        printJobSummary(spooler);

        // 3. Setup sample jobs if none are queued
        boolean hasQueuedJobs = spooler.listJobs().stream()
                .anyMatch(j -> j.getStatus() == PrintJobStatus.QUEUED);
        if (!hasQueuedJobs) {
            setupSuccessfulJob(spooler);
            setupFailingJob(spooler); // <-- Add the failing job
            logger.info("--- State after submitting new jobs ---");
            printJobSummary(spooler);
        }

        // 4. Run the worker to process ALL available jobs
        logger.info("\n--- Processing All Queued Jobs... ---");
        while (worker.processOneStep()) {
            // The loop continues as long as the worker finds and processes a job.
        }

        // 5. Display final state
        logger.info("\n--- Final State of Spooler ---");
        printJobSummary(spooler);
        logger.info("--- Virtual Printer Simulation Finished ---");
        logger.info("Check the '{}' directory for outputs.", OUTPUT_DIR.toAbsolutePath());
    }

    /** Creates a job that should process correctly. */
    private static void setupSuccessfulJob(SpoolerService spooler) throws IOException {
        logger.info("Setting up a new successful sample print job...");
        Files.createDirectories(SPOOL_DIR);
        Path sourceFile = SPOOL_DIR.resolve("successful_job.txt");
        Files.writeString(sourceFile, "This is a test document that should print successfully.");
        PrintJob newJob = new PrintJob("Successful Doc", "system", PrintSettings.A4_DEFAULT_300_DPI(), List.of(sourceFile.toAbsolutePath().toString()));
        spooler.submit(newJob);
    }

    /** Creates a job that is guaranteed to fail. */
    private static void setupFailingJob(SpoolerService spooler) {
        logger.info("Setting up a new failing sample print job...");
        // This file does not exist, which will cause an exception during processing.
        Path nonExistentFile = SPOOL_DIR.resolve("non_existent_file.txt");
        PrintJob newJob = new PrintJob("Failing Doc", "system", PrintSettings.A4_DEFAULT_300_DPI(), List.of(nonExistentFile.toAbsolutePath().toString()));
        spooler.submit(newJob);
    }

    private static void printJobSummary(SpoolerService spooler) {
        List<PrintJob> jobs = spooler.listJobs();
        if (jobs.isEmpty()) {
            logger.info("Job queue is empty.");
            return;
        }
        logger.info("Current Jobs ({} total):", jobs.size());
        for (PrintJob job : jobs) {
            logger.info("  - Job ID: {}, Status: {}, Document: {}, Errors: {}",
                    job.getJobId().substring(0, 8), job.getStatus(), job.getDocumentName(), job.getErrorLog().size());
        }
    }
}