package com.wonderboy.printer.renderer;

import com.wonderboy.printer.model.PrintSettings;
import com.wonderboy.printer.source.TextPageSource;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleTextRendererTest {

    @Test
    void testRenderTextToPngForVisualInspection() throws Exception {
        // 1. Arrange: Find our test file in resources
        URL resource = getClass().getClassLoader().getResource("sample.txt");
        assertNotNull(resource, "Test resource 'sample.txt' not found.");
        Path sourcePath = Paths.get(resource.toURI());

        TextPageSource textSource = new TextPageSource(sourcePath);
        PrintSettings settings = PrintSettings.A4_DEFAULT_300_DPI();
        SimpleTextRenderer renderer = new SimpleTextRenderer();

        // Create an output directory
        Path outputDir = Paths.get("output");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("rendered_text_page.png");

        // 2. Act: Render the first page
        BufferedImage pageImage = renderer.render(textSource, 0, settings);

        // 3. Assert and Save
        assertNotNull(pageImage, "The rendered image should not be null.");

        boolean success = ImageIO.write(pageImage, "png", outputPath.toFile());
        assertTrue(success, "Should successfully write image to file.");

        System.out.println("---- Visual Test Output ----");
        System.out.println("Successfully rendered text to PNG for visual inspection.");
        System.out.println("Please check the file: " + outputPath.toAbsolutePath());
        System.out.println("----------------------------");
    }
}