package com.wonderboy.printer.model;

/**
 * Represents the status of a print job in the spooler queue.
 */
public enum PrintJobStatus {

    QUEUED,  // 队列中

    PREVIEWING,  // 预览中

    PRINTING,  // 打印中

    COMPLETED,  // 已完成

    CANCELLED,  // 已取消

    FAILED,  // 失败

    PAUSED  // 已暂停
}