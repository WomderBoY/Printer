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
     * 计算总页数
     * @return 总页数
     * @throws IOException 读取源时发生错误
     */
    int getPageCount() throws IOException;

    /**
     * 将文档转化为位图（“打印机”能理解的语言）
     * 这是驱动程序的核心功能
     *
     * @param pageIndex 页数（0-based）
     * @param settings 打印设置.
     * @return BufferedImage 代表渲染后的页面
     * @throws IOException 读取源时发生错误
     * @throws IndexOutOfBoundsException 页码不合法
     */
    BufferedImage renderPage(int pageIndex, PrintSettings settings) throws IOException;

}