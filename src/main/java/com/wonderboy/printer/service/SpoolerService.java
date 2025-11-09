package com.wonderboy.printer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintJobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the print job queue, including persistence to disk.
 * This is the core "spooler" component.
 */
public class SpoolerService {

    private static final Logger logger = LoggerFactory.getLogger(SpoolerService.class);
    private final Path spoolDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, PrintJob> jobQueue;

    /**
     * Creates a new SpoolerService.
     * @param spoolDirectory The directory to store job metadata files.
     */
    public SpoolerService(Path spoolDirectory) {
        this.spoolDirectory = spoolDirectory;
        this.jobQueue = new ConcurrentHashMap<>();

        // Configure Jackson ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule()); // For LocalDateTime support

        ensureSpoolDirectoryExists();
        loadJobsFromDisk();
    }

    public Path getSpoolDirectory() {
        return spoolDirectory;
    }

    /**
     * Submits a new print job to the spooler.
     * The job is added to the in-memory queue and its metadata is saved to a JSON file.
     *
     * @param job The print job to submit.
     */
    public void submit(PrintJob job) {
        if (job == null || job.getJobId() == null) {
            throw new IllegalArgumentException("Job and Job ID cannot be null.");
        }

        jobQueue.put(job.getJobId(), job);
        this.persistJob(job);
        logger.info("Submitted and persisted job: {}", job.getJobId());
    }

    public void confirmPrint(String jobId) {
        PrintJob job = jobQueue.get(jobId);
        if (job != null && job.getStatus() == PrintJobStatus.PREVIEWING) {
            job.setStatus(PrintJobStatus.PRINTING);
            updateJob(job);
            logger.info("User confirmed printing for job: {}", jobId);
        }
    }

    /**
     * Returns a sorted list of all jobs currently managed by the spooler.
     * The list is sorted by submission time.
     *
     * @return A list of PrintJob objects.
     */
    public List<PrintJob> listJobs() {
        return jobQueue.values().stream()
                .sorted(Comparator.comparing(PrintJob::getSubmitTime))
                .collect(Collectors.toList());
    }

    /**
     * Ensures the spool directory exists, creating it if necessary.
     */
    private void ensureSpoolDirectoryExists() {
        try {
            if (Files.notExists(spoolDirectory)) {
                Files.createDirectories(spoolDirectory);
                logger.info("Created spool directory at: {}", spoolDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Could not create spool directory: {}", spoolDirectory, e);
            // This is a critical failure, the application cannot run without the spool directory.
            throw new RuntimeException("Failed to create spool directory.", e);
        }
    }

    /**
     * Loads all existing job files (.json) from the spool directory into the in-memory queue.
     * This is called on startup to recover the queue state.
     */
    private void loadJobsFromDisk() {
        logger.info("Loading existing jobs from {}...", spoolDirectory);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(spoolDirectory, "*.json")) {
            for (Path jobFile : stream) {
                try {
                    PrintJob job = objectMapper.readValue(jobFile.toFile(), PrintJob.class);
                    jobQueue.put(job.getJobId(), job);
                    logger.info("Loaded job {} from file.", job.getJobId());
                } catch (IOException e) {
                    logger.error("Failed to load job from file: {}", jobFile, e);
                }
            }
        } catch (IOException e) {
            logger.error("Could not read spool directory.", e);
            throw new RuntimeException("Failed to read spool directory.", e);
        }
    }

    /**
     * Saves a single print job's metadata to a JSON file in the spool directory.
     * The file is named after the job's ID.
     *
     * @param job The job to persist.
     */
    private void persistJob(PrintJob job) {
        Path jobFile = spoolDirectory.resolve(job.getJobId() + ".json");
        try {
            // 写入JSON文件
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jobFile.toFile(), job);
        } catch (IOException e) {
            logger.error("Failed to persist job {} to file {}", job.getJobId(), jobFile, e);
        }
    }

    /**
     * Updates an existing print job in the queue and persists the changes to its JSON file.
     * This is crucial for updating the job's status as it moves through the printing pipeline.
     *
     * @param job The print job with updated information.
     */
    public void updateJob(PrintJob job) {
        if (job == null || !jobQueue.containsKey(job.getJobId())) {
            logger.warn("Attempted to update a job that does not exist in the queue: {}", job.getJobId());
            return;
        }
        // 更新内存中的job
        jobQueue.put(job.getJobId(), job);
        // 保存更新
        persistJob(job);
        logger.debug("Updated and persisted job: {}", job.getJobId());
    }

    /**
     * Cancels a job that is QUEUED or PRINTING.
     * Note: This doesn't stop a job that's already deep into rendering,
     * but prevents the worker from continuing or starting it.
     * @param jobId The ID of the job to cancel.
     */
    public void cancelJob(String jobId) {
        PrintJob job = jobQueue.get(jobId);
        if (job != null && (job.getStatus() == PrintJobStatus.QUEUED || job.getStatus() == PrintJobStatus.PRINTING)) {
            job.setStatus(PrintJobStatus.CANCELLED);
            updateJob(job);
            logger.info("Cancelled job: {}", jobId);
        }
    }

    /**
     * Resets a FAILED job's status to QUEUED so the worker can try it again.
     * It also clears the previous error log.
     * @param jobId The ID of the job to retry.
     */
    public void retryJob(String jobId) {
        PrintJob job = jobQueue.get(jobId);
        if (job != null && job.getStatus() == PrintJobStatus.FAILED) {
            job.setStatus(PrintJobStatus.QUEUED);
            job.getErrorLog().clear(); // Clear old errors before retrying
            updateJob(job);
            logger.info("Retrying job: {}", jobId);
        }
    }

    /**
     * Removes a job from the queue and deletes its metadata file from the disk.
     * This is for jobs that are in a terminal state (COMPLETED, FAILED, CANCELLED).
     * @param jobId The ID of the job to remove.
     */
    public void removeJob(String jobId) {
        PrintJob job = jobQueue.remove(jobId);
        if (job != null) {
            Path jobFile = spoolDirectory.resolve(job.getJobId() + ".json");
            try {
                Files.deleteIfExists(jobFile);
                logger.info("Removed job {} and its metadata file.", jobId);
            } catch (IOException e) {
                logger.error("Failed to delete job metadata file: {}", jobFile, e);
            }
        }
    }
}