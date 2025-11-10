# Java 模拟打印系统

## 总体结构概述
- **UI层** (com.wonderboy.printer.ui): 用户交互，提交和处理打印任务、预览；
- **Spooler层** (com.wonderboy.printer.service): 接受打印任务，驱动每个打印任务通过其生命周期的各个阶段；
- **Renderer层** (com.wonderboy.printer.render): 执行图形渲染，将文档源转化为位图；
- **Printer层** (com.wonderboy.printer.printer): 接收渲染页，模拟纸张输出（保存为pdf），并将打印结果回调给ui.

## 实现细节

### 一、核心实体
#### 1. PrintJob
代表一个打印任务的所有元数据。
核心字段：
- jobID: 唯一标识符；
- documentName: 文档名；
- **status** ([PrintJobStatus](#2-printjobstatus)): 生命周期状态
- **settings** ([PrintSettings](#3-printsettings)): 可配置参数
- sourceFilePath: 源文件的绝对路径
- submitTime: 提交时间（用于ui中排序显示）
- errorLog: 记录异常
- user: 记录用户名，**暂未实现**

#### 2. PrintJobStatus
是一个 enum, 定义所有生命状态，包括：
- QUEUED
- PREVIEWING
- PRINTING
- COMPLETED
- FAILED
- CANCELED

#### 3. PrintSettings
是一个 record, 封装了打印任务的配置选项，包括：
- paper: 纸张大小 (A4/LETTER)
- dpi: 打印分辨率
- isColor: 是否彩打
- isDuplex: 是否双面，**暂未实现**

#### 4. PageSource
是一个Interface，提供原始内容给 Renderer.
**增强了文件的可扩展性**（可扩展为pdf/png等），Render 和 Spooler 面向这个接口编程。

---

### 二、服务层(Spooler)

#### 1. SpoolerService
管理PrintJob的状态，以及持久化（将 PrintJob 对象的状态序列化为json文件）。

关键方法：
- 构造函数：扫描并加载 spool 目录下的json文件
- submit: 提交任务
- updateJob: 状态变更
- listJobs: 为ui提供任务列表
- cancelJob, retryJob, removeJob: ui层的用户交互操作

#### 2. SpoolerWorker
SpoolerService执行的是静态的任务管理，而Worker与SpoolerService不同，执行动态的任务执行。

**processOneStep** 是 Worker 的主循环体，包含两个阶段：
##### processNextQueuedJob:
负责渲染、预览：
- (1)查找状态为QUEUED的任务，并更新为PREVIEWING
- (2)创建PageSource实例
- (3)调用renderer.getTotalPages()计算总页数
- (4)逐页调用renderer.render()生成BUfferImage
  注：BufferImage 传递给 virtualPrinter.acceptRenderedPage(), 这个方法将其保存为png文件，用于预览功能。

##### processNextPrintingJob:
负责生成最终文件：
- (0)用户预览后点击“确认打印”，任务状态更新为PRINTING
  注：processNextQueuedJob()不会等待用户确认，而是返回主循环继续处理其它QUEUED任务
- (1)查找状态为PRINTING的任务，将BufferImage/png合成为pdf
- (2)任务状态更新为COMPLETED

---

### 三、渲染器层(Renderer)

负责将文档内容绘制成位图，模拟将矢量字体转化为打印机硬件能识别的像素点阵。

定义了接口 **PageRender**，提供给Spooler使用，增强了可扩展性。
核心方法：
- render(): 这是渲染器的核心方法。它接收一个PageSource，返回一个 java.awt.image.BufferedImage 对象（在内存中创建一个空白“画布”）
- getTotalPages(): 计算总页数

**SimpleTextRenderer** 是接口的一个实现，负责处理txt文件

具体处理流程：
1. 计算画布尺寸（纸张类型、DPI）
2. 设置绘图环境：这一部分创建了Graphics2D对象。绘图主要依赖 java.awt包
3. 布局与分页计算：定义方法测量每行/单词的像素宽度，计算行数（包括超出边界自动拆分成多行），根据行数计算页数等。
4. 绘制当前页：同2，使用java.awt包处理Graphics2D对象。

---

### 四、打印机层(Printer)

**VirtualPrinter**是这一层的核心类，负责接受已经渲染好的页面图像(BufferedImage)，并将其以pdf的形式“打印”出来。

处理流程：
1. 接受渲染页 (acceptRenderedPage())
2. 保存为png(用于预览功能)
3. 文档组装：输出pdf (finishJob())

关键依赖：
- javax.imageio.ImageIO: 将内存中的BufferedImage以png格式写入硬盘
- org.apache.pdfbox: 创建pdf

**PagePrintListener**是一个简单的接口，实现后端与前端的通信。

---

### 五、UI层
分为左、中、右三个部分，左侧是文件上传和打印设置区，中间是预览区，右侧是任务列表。

#### Select&Configure区域：
可以选择文件并上传（目前只支持了txt文件），提供打印设置选项（纸张、DPI、颜色）。用户选择好了之后可以提交打印作业。

#### Preview区域
显示BufferedImage(png)，供用户预览打印效果，用户点击确认打印。

#### Job Queue区域
实时显示“作业队列”，显示任务状态；用户可以对队列中的任务进行管理(**右键**)：
- QUEUED任务可以取消
- FAILED任务可以重试
- COMPLETED任务可以移除

使用JavaFX实现，由MainController控制，前端逻辑不再赘述。

---

## 未来改进：
由于时间较短，项目完成的较为匆忙，目前想到的优化点如下：
- 增加其他类型文件的支持
- MainController行数过大，不易维护，需要重构
- PageRenderer和PageSource有些重复，可以重构使其更清晰
- 预览区增加取消任务的功能 (效果不满意可以取消)
- 已完成的任务可能需要清理，否则长期使用会大量堆积
- ……