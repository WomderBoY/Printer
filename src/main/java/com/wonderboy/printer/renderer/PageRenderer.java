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
     * 将文档渲染成位图
     *
     * @param source 源文件
     * @param pageIndex 页码
     * @param settings 打印设置
     * @return 渲染得到的位图
     * @throws IOException 读取源时发生错误
     */
    BufferedImage render(PageSource source, int pageIndex, PrintSettings settings) throws IOException;

    /**
     * 计算总页数
     *
     * @param source 源文件
     * @param settings 打印设置
     * @return 总页数
     * @throws IOException 读取源时发生错误
     */
    int getTotalPages(PageSource source, PrintSettings settings) throws IOException;
}