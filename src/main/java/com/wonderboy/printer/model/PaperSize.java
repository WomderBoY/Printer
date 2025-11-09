package com.wonderboy.printer.model;

/**
 * Enumeration of common paper sizes, including their physical dimensions in millimeters.
 */
public enum PaperSize {
    A4(210, 297),
    A5(148, 210),
    LETTER(215.9, 279.4),
    LEGAL(215.9, 355.6);

    private final double widthInMm;
    private final double heightInMm;

    PaperSize(double widthInMm, double heightInMm) {
        this.widthInMm = widthInMm;
        this.heightInMm = heightInMm;
    }

    public double getWidthInMm() {
        return widthInMm;
    }

    public double getHeightInMm() {
        return heightInMm;
    }
}