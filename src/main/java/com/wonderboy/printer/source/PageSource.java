package com.wonderboy.printer.source;

import com.wonderboy.printer.model.PrintSettings;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * An abstraction for a printable document source.
 * Implementations will handle specific file types like text, images, or PDFs.
 */
public interface PageSource {

    /**
     * Gets the total number of pages in this source document.
     * @return The page count.
     * @throws IOException if there is an error reading the source.
     */
    int getPageCount() throws IOException;

    /**
     * Renders a specific page of the document into a raster image.
     * This is the core responsibility of the "Print Driver".
     *
     * @param pageIndex The 0-based index of the page to render.
     * @param settings The print settings (like DPI) to use for rendering.
     * @return A BufferedImage representing the rendered page.
     * @throws IOException if there is an error rendering the page.
     * @throws IndexOutOfBoundsException if the pageIndex is invalid.
     */
    BufferedImage renderPage(int pageIndex, PrintSettings settings) throws IOException;

}