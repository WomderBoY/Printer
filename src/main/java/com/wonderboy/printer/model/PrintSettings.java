package com.wonderboy.printer.model;

/**
 * 打印设置
 * @param paper   纸张大小
 * @param dpi     分辨率
 * @param isColor 是否彩打
 */
public record PrintSettings(
        PaperSize paper,
        int dpi,
        boolean isColor,
        boolean isDuplex,
        double scale,
        int copies
) {
    /**
     * 默认设置
     * @return A default PrintSettings object.
     */
    public static PrintSettings A4_DEFAULT_300_DPI() {
        return new PrintSettings(PaperSize.A4, 300, true, false, 1.0, 1);
    }
}