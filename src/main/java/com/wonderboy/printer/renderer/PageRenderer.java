package com.wonderboy.printer.renderer;

import com.wonderboy.printer.model.PrintSettings;
import com.wonderboy.printer.source.PageSource;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Defines the contract for a component that can render a page from a PageSource
 * into a BufferedImage. This represents the core logic of a "print driver".
 */
public interface PageRenderer {

    /**
     * Renders a single page from the given source document.
     *
     * @param source The document source to render from.
     * @param pageIndex The 0-based index of the page to render.
     * @param settings The print settings to apply (DPI, paper size, etc.).
     * @return A BufferedImage containing the rendered page.
     * @throws IOException if an error occurs while reading from the source.
     */
    BufferedImage render(PageSource source, int pageIndex, PrintSettings settings) throws IOException;

    /**
     * Calculates the total number of pages the document will have when rendered
     * with the specified settings. This can be an expensive operation.
     *
     * @param source The document source.
     * @param settings The print settings that affect pagination.
     * @return The total number of pages.
     * @throws IOException if an error occurs while reading from the source.
     */
    int getTotalPages(PageSource source, PrintSettings settings) throws IOException;
}