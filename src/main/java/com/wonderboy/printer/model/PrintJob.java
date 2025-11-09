package com.wonderboy.printer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Represents the metadata for a single print job.
 * This class is designed to be serialized to JSON for persistence in the spooler.
 */
public class PrintJob {

    private String jobId;  // 唯一标识符
    private String documentName;  // 文档名
    private String user;  // 提交用户
    private PrintSettings settings;  // 可配置参数
    private PrintJobStatus status;  // 生命周期状态
    private List<String> sourceFilePaths; // 源文件的绝对路径
    private LocalDateTime submitTime;  // 提交时间
    private List<String> errorLog = new java.util.ArrayList<>();  // 错误日志

    // Default constructor for Jackson deserialization
    public PrintJob() {
        this.errorLog = new ArrayList<>();
    }

    // A convenient constructor for creating a new job
    public PrintJob(String documentName, String user, PrintSettings settings, List<String> sourceFilePaths) {
        this.jobId = UUID.randomUUID().toString();
        this.documentName = documentName;
        this.user = user;
        this.settings = settings;
        this.sourceFilePaths = sourceFilePaths;
        this.status = PrintJobStatus.QUEUED;
        this.submitTime = LocalDateTime.now();
        this.errorLog = new ArrayList<>();
    }

    // Getters and Setters for all fields (required for Jackson)

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public PrintSettings getSettings() {
        return settings;
    }

    public void setSettings(PrintSettings settings) {
        this.settings = settings;
    }

    public PrintJobStatus getStatus() {
        return status;
    }

    public void setStatus(PrintJobStatus status) {
        this.status = status;
    }

    public List<String> getSourceFilePaths() {
        return sourceFilePaths;
    }

    public void setSourceFilePaths(List<String> sourceFilePaths) {
        this.sourceFilePaths = sourceFilePaths;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public java.util.List<String> getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(java.util.List<String> errorLog) {
        this.errorLog = errorLog;
    }

    /**
     * A convenience method to add a new error message to the log.
     * @param message The error message to add.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore // We don't want this helper method in the JSON
    public void appendErrorLog(String message) {
        if (this.errorLog == null) {
            this.errorLog = new java.util.ArrayList<>();
        }
        this.errorLog.add(java.time.LocalDateTime.now() + ": " + message);
    }
}