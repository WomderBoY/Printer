package com.wonderboy.printer.renderer;

import com.wonderboy.printer.model.PaperSize;
import com.wonderboy.printer.model.PrintSettings;
import com.wonderboy.printer.source.PageSource;
import com.wonderboy.printer.source.TextPageSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SimpleTextRenderer implements PageRenderer {

    private static final double INCH_TO_MM = 25.4;

    @Override
    public BufferedImage render(PageSource source, int pageIndex, PrintSettings settings) throws IOException {
        if (!(source instanceof TextPageSource textSource)) {
            throw new IllegalArgumentException("SimpleTextRenderer only supports TextPageSource.");
        }

        // 1. Calculate page dimensions in pixels
        PaperSize paper = settings.paper();
        int dpi = settings.dpi();
        int pageWidth = (int) Math.round(paper.getWidthInMm() / INCH_TO_MM * dpi);
        int pageHeight = (int) Math.round(paper.getHeightInMm() / INCH_TO_MM * dpi);

        // 2. Create blank page image (our canvas)
        BufferedImage pageImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = pageImage.createGraphics();

        try {
            // 3. Setup graphics context for high-quality rendering
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, pageWidth, pageHeight);
            g2d.setColor(Color.BLACK);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 4. Set font and get font metrics
            Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12 * dpi / 72); // 12pt font
            g2d.setFont(font);
            FontMetrics metrics = g2d.getFontMetrics();
            int lineHeight = metrics.getHeight();

            // 5. Define printable area with 1-inch margins
            int margin = dpi; // 1 inch = dpi pixels
            int contentWidth = pageWidth - (2 * margin);
            int contentHeight = pageHeight - (2 * margin);
            int currentX = margin;
            int currentY = margin + metrics.getAscent();

            // 6. Paginate and draw the content
            List<String> wrappedLines = wordWrap(textSource.getLines(), metrics, contentWidth);
            int totalPages = (int) Math.ceil((double) (wrappedLines.size() * lineHeight) / contentHeight);

            int linesPerPage = contentHeight / lineHeight;
            int startLine = pageIndex * linesPerPage;
            int endLine = Math.min(startLine + linesPerPage, wrappedLines.size());

            if (startLine >= wrappedLines.size()) {
                // Requesting a page that is out of bounds, return a blank page.
                return pageImage;
            }

            for (int i = startLine; i < endLine; i++) {
                g2d.drawString(wrappedLines.get(i), currentX, currentY);
                currentY += lineHeight;
            }

            // 7. Draw page number
            String pageNumberText = String.format("Page %d of %d", pageIndex + 1, totalPages);
            int textWidth = metrics.stringWidth(pageNumberText);
            g2d.drawString(pageNumberText, (pageWidth - textWidth) / 2, pageHeight - margin / 2);

        } finally {
            // 8. Clean up resources
            g2d.dispose();
        }

        return pageImage;
    }

    @Override
    public int getTotalPages(PageSource source, PrintSettings settings) throws IOException {
        if (!(source instanceof TextPageSource textSource)) {
            throw new IllegalArgumentException("SimpleTextRenderer only supports TextPageSource.");
        }

        int dpi = settings.dpi();
        int margin = dpi;
        int pageHeight = (int) Math.round(settings.paper().getHeightInMm() / INCH_TO_MM * dpi);
        int contentHeight = pageHeight - (2 * margin);

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12 * dpi / 72);
        // Create a temporary image to get FontMetrics
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();
        g2d.dispose();

        int lineHeight = metrics.getHeight();
        if (lineHeight == 0) return 0;

        int contentWidth = (int) Math.round(settings.paper().getWidthInMm() / INCH_TO_MM * dpi) - (2*margin);
        List<String> wrappedLines = wordWrap(textSource.getLines(), metrics, contentWidth);

        return (int) Math.ceil((double) (wrappedLines.size() * lineHeight) / contentHeight);
    }

    /**
     * A simple word-wrap algorithm.
     */
    private List<String> wordWrap(List<String> originalLines, FontMetrics metrics, int maxWidth) {
        List<String> wrappedLines = new ArrayList<>();
        for (String line : originalLines) {
            if (metrics.stringWidth(line) <= maxWidth) {
                wrappedLines.add(line);
            } else {
                String[] words = line.split(" ");
                StringBuilder currentLine = new StringBuilder();
                for (String word : words) {
                    if (metrics.stringWidth(currentLine + " " + word) > maxWidth) {
                        wrappedLines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        if (!currentLine.isEmpty()) {
                            currentLine.append(" ");
                        }
                        currentLine.append(word);
                    }
                }
                if (!currentLine.isEmpty()) {
                    wrappedLines.add(currentLine.toString());
                }
            }
        }
        return wrappedLines;
    }
}