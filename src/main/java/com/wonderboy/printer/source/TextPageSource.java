package com.wonderboy.printer.source;

import com.wonderboy.printer.model.PrintSettings;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A PageSource for plain text files. Its primary role is to provide the text lines to a renderer.
 */
public class TextPageSource implements PageSource {

    private final Path textFilePath;
    private List<String> lines; // Cache the lines to avoid re-reading the file

    public TextPageSource(Path textFilePath) {
        if (textFilePath == null || !Files.isReadable(textFilePath)) {
            throw new IllegalArgumentException("Text file path is invalid or file is not readable.");
        }
        this.textFilePath = textFilePath;
    }

    /**
     * Provides all lines from the source text file.
     * @return A list of strings, where each string is a line from the file.
     * @throws IOException if an error occurs reading the file.
     */
    public List<String> getLines() throws IOException {
        if (lines == null) {
            this.lines = Files.readAllLines(textFilePath, StandardCharsets.UTF_8);
        }
        return lines;
    }

    // This method is part of the interface but delegates the real work to a renderer.
    // In a full application, a PageSource might be paired with a default renderer.
    // For now, we make it clear that it's not meant to be called directly.
    @Override
    public BufferedImage renderPage(int pageIndex, PrintSettings settings) {
        throw new UnsupportedOperationException("TextPageSource does not render itself. Use a PageRenderer instead.");
    }

    // As per the PageSource interface, but its implementation is now a placeholder.
    // The true page count is determined by a PageRenderer.
    @Override
    public int getPageCount() {
        throw new UnsupportedOperationException("Page count must be calculated by a PageRenderer with specific PrintSettings.");
    }
}