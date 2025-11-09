package com.wonderboy.printer.printer;

import com.wonderboy.printer.model.PrintJob;
import java.awt.image.BufferedImage;

/**
 * A functional interface for components that want to be notified
 * when a single page has been rendered and accepted by the VirtualPrinter.
 */
@FunctionalInterface
public interface PagePrintListener {
    /**
     * Called when a page is ready for processing or preview.
     * This method will be invoked on the background worker thread.
     *
     * @param job The job the page belongs to.
     * @param image The rendered image of the page.
     * @param pageNumber The 1-based page number.
     */
    void onPagePrinted(PrintJob job, BufferedImage image, int pageNumber);
}