package com.wonderboy.printer.printer;

import com.wonderboy.printer.model.PrintJob;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simulates the physical printer hardware. It receives rendered pages (as images)
 * and assembles them into a final output file, such as a PDF.
 */
public class VirtualPrinter {

    private static final Logger logger = LoggerFactory.getLogger(VirtualPrinter.class);
    public static final String RENDERED_PAGES_DIR_NAME = "rendered_pages";
    private final Path outputDirectory;

    // A map to hold the pages for each active print job.
    // Key: Job ID, Value: A thread-safe list of rendered page images.
    // private final Map<String, List<BufferedImage>> activeJobPages;

    private PagePrintListener pagePrintListener;

    public VirtualPrinter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        ensureOutputDirectoryExists();
    }

    public void setPagePrintListener(PagePrintListener listener) {
        this.pagePrintListener = listener;
    }

    public void acceptRenderedPage(PrintJob job, BufferedImage pageImage, int pageNumber) {
        Path jobOutputDir = outputDirectory.resolve(job.getJobId());
        Path pagesDir = jobOutputDir.resolve(RENDERED_PAGES_DIR_NAME);
        try {
            Files.createDirectories(pagesDir);
            Path pageFile = pagesDir.resolve(String.format("page_%04d.png", pageNumber));
            ImageIO.write(pageImage, "png", pageFile.toFile());
            logger.debug("Saved rendered page {} for job {} to {}", pageNumber, job.getJobId(), pageFile);

            if (pagePrintListener != null) {
                pagePrintListener.onPagePrinted(job, pageImage, pageNumber);
            }
        } catch (IOException e) {
            logger.error("Failed to save rendered page {} for job {}", pageNumber, job.getJobId(), e);
        }
    }

    public void finishJob(PrintJob job) throws IOException {
        Path jobOutputDir = outputDirectory.resolve(job.getJobId());
        Path pagesDir = jobOutputDir.resolve(RENDERED_PAGES_DIR_NAME);
        Path pdfPath = jobOutputDir.resolve("output.pdf");

        if (!Files.exists(pagesDir)) {
            logger.warn("No rendered pages found for job {}. Cannot create PDF.", job.getJobId());
            return;
        }

        List<Path> pageFiles;
        try (Stream<Path> paths = Files.list(pagesDir)) {
            pageFiles = paths.filter(p -> p.toString().endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        }

        if (pageFiles.isEmpty()) {
            logger.warn("Rendered pages directory is empty for job {}.", job.getJobId());
            return;
        }

        logger.info("Finishing job {}. Assembling {} pages into PDF: {}", job.getJobId(), pageFiles.size(), pdfPath);

        try (PDDocument document = new PDDocument()) {
            for (Path pageFile : pageFiles) {
                BufferedImage pageImage = ImageIO.read(pageFile.toFile());
                float pointsPerPixel = 72f / job.getSettings().dpi();
                float widthInPoints = pageImage.getWidth() * pointsPerPixel;
                float heightInPoints = pageImage.getHeight() * pointsPerPixel;

                PDPage pdfPage = new PDPage(new PDRectangle(widthInPoints, heightInPoints));
                document.addPage(pdfPage);

                PDImageXObject pdImage = PDImageXObject.createFromFile(pageFile.toString(), document);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, pdfPage)) {
                    contentStream.drawImage(pdImage, 0, 0, widthInPoints, heightInPoints);
                }
            }
            document.save(pdfPath.toFile());
            logger.info("Successfully created PDF for job {}", job.getJobId());
        }
    }

    private void ensureOutputDirectoryExists() {
        try {
            if (Files.notExists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
                logger.info("Created output directory at: {}", outputDirectory.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Could not create output directory: {}", outputDirectory, e);
            throw new RuntimeException("Failed to create output directory.", e);
        }
    }
}