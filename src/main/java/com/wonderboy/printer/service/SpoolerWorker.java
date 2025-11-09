package com.wonderboy.printer.service;

import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintJobStatus;
import com.wonderboy.printer.printer.VirtualPrinter;
import com.wonderboy.printer.renderer.PageRenderer;
import com.wonderboy.printer.source.PageSource;
import com.wonderboy.printer.source.TextPageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * A worker that processes a single print job from start to finish.
 * It orchestrates the spooler, renderer, and virtual printer.
 */
public class SpoolerWorker {

    private static final Logger logger = LoggerFactory.getLogger(SpoolerWorker.class);

    private final SpoolerService spoolerService;
    private final PageRenderer renderer;
    private final VirtualPrinter virtualPrinter;

    public SpoolerWorker(SpoolerService spoolerService, PageRenderer renderer, VirtualPrinter virtualPrinter) {
        this.spoolerService = spoolerService;
        this.renderer = renderer;
        this.virtualPrinter = virtualPrinter;
    }

    /**
     * Processes one stage of the next available job.
     * Returns true if any work was done.
     */
    public boolean processOneStep() {
        // Priority 1: Handle user-confirmed print jobs (fast operation)
        if (processNextPrintingJob()) {
            return true;
        }
        // Priority 2: Handle new jobs that need rendering (slow operation)
        return processNextQueuedJob();
    }

    private boolean processNextQueuedJob() {
        Optional<PrintJob> jobOptional = findFirstJobByStatus(PrintJobStatus.QUEUED);
        if (jobOptional.isEmpty()) return false;

        PrintJob job = jobOptional.get();
        logger.info("Stage 1: Starting to render job for preview: {}", job.getJobId());

        try {
            job.setStatus(PrintJobStatus.PREVIEWING);
            spoolerService.updateJob(job);

            PageSource source = new TextPageSource(Paths.get(job.getSourceFilePaths().getFirst()));
            int totalPages = renderer.getTotalPages(source, job.getSettings());

            for (int i = 0; i < totalPages; i++) {
                logger.info("Rendering page {} of {} for job {}", i + 1, totalPages, job.getJobId());
                BufferedImage pageImage = renderer.render(source, i, job.getSettings());
                virtualPrinter.acceptRenderedPage(job, pageImage, i + 1);
            }
            logger.info("Finished rendering job {} for preview.", job.getJobId());
        } catch (Exception e) {
            handleFailure(job, e);
        }
        return true;
    }

    private boolean processNextPrintingJob() {
        Optional<PrintJob> jobOptional = findFirstJobByStatus(PrintJobStatus.PRINTING);
        if (jobOptional.isEmpty()) return false;

        PrintJob job = jobOptional.get();
        logger.info("Stage 2: Finalizing PDF for job: {}", job.getJobId());

        try {
            virtualPrinter.finishJob(job);
            job.setStatus(PrintJobStatus.COMPLETED);
            spoolerService.updateJob(job);
            logger.info("Successfully completed job: {}", job.getJobId());
        } catch (Exception e) {
            handleFailure(job, e);
        }
        return true;
    }

    private void handleFailure(PrintJob job, Exception e) {
        logger.error("Failed to process job {}: {}", job.getJobId(), e.getMessage(), e);
        String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
        job.appendErrorLog(errorMessage);
        job.setStatus(PrintJobStatus.FAILED);
        spoolerService.updateJob(job);
    }

    private Optional<PrintJob> findFirstJobByStatus(PrintJobStatus status) {
        return spoolerService.listJobs().stream()
                .filter(job -> job.getStatus() == status)
                .findFirst();
    }
}