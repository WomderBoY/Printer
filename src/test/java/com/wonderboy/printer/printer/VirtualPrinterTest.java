package com.wonderboy.printer.printer;

import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintSettings;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualPrinterTest {

    @TempDir
    Path tempOutputDir; // JUnit will provide a temporary directory

    private VirtualPrinter virtualPrinter;
    private PrintJob testJob;

    @BeforeEach
    void setUp() {
        virtualPrinter = new VirtualPrinter(tempOutputDir);
        testJob = new PrintJob("test-doc", "test-user", PrintSettings.A4_DEFAULT_300_DPI(), List.of());
    }

    @Test
    void finishJob_createsPdfFromAcceptedPages() throws IOException {
        // Arrange: Create two sample page images
        BufferedImage page1 = createTestImage(600, 800, "Page 1");
        BufferedImage page2 = createTestImage(600, 800, "Page 2");

        // Act: Simulate the printing process
        virtualPrinter.acceptRenderedPage(testJob, page1, 1);
        virtualPrinter.acceptRenderedPage(testJob, page2, 2);
        virtualPrinter.finishJob(testJob);

        // Assert: Check if the PDF was created and is valid
        Path pdfPath = tempOutputDir.resolve(testJob.getJobId()).resolve("output.pdf");
        assertTrue(Files.exists(pdfPath), "PDF file should be created.");

        // Verify the PDF content (e.g., page count)
        try (PDDocument loadedPdf = PDDocument.load(pdfPath.toFile())) {
            assertEquals(2, loadedPdf.getNumberOfPages(), "PDF should have 2 pages.");
        }

        System.out.println("---- VirtualPrinter Test Output ----");
        System.out.println("Successfully created a 2-page test PDF.");
        System.out.println("Test PDF located in temporary directory: " + pdfPath.getParent());
        System.out.println("------------------------------------");
    }

    @Test
    void finishJob_handlesJobWithNoPagesGracefully() throws IOException {
        // Act & Assert: Calling finishJob on a job with no pages should not throw an exception.
        assertDoesNotThrow(() -> virtualPrinter.finishJob(testJob));

        // Also assert that no directory or file was created for this empty job
        Path jobOutputDir = tempOutputDir.resolve(testJob.getJobId());
        assertFalse(Files.exists(jobOutputDir), "No output directory should be created for a job with no pages.");
    }

    /**
     * Helper method to create a simple BufferedImage with some text.
     */
    private BufferedImage createTestImage(int width, int height, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 40));

        FontMetrics fm = g2d.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        int y = (fm.getAscent() + (height - (fm.getAscent() + fm.getDescent())) / 2);

        g2d.drawString(text, x, y);
        g2d.dispose();

        return image;
    }
}