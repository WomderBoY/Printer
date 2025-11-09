package com.wonderboy.printer.ui;

import com.wonderboy.printer.model.PaperSize;
import com.wonderboy.printer.model.PrintJob;
import com.wonderboy.printer.model.PrintJobStatus;
import com.wonderboy.printer.model.PrintSettings;
import com.wonderboy.printer.printer.VirtualPrinter;
import com.wonderboy.printer.service.SpoolerService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // --- Backend Services ---
    private SpoolerService spoolerService;
    private VirtualPrinter virtualPrinter;
    private File selectedSourceFile;

    // --- UI State ---
    private List<Image> previewPages = new ArrayList<>();
    private int currentPageIndex = -1;
    private PrintJobStatus selectedJobLastStatus = null; // Track status to detect changes

    private boolean suppressSelectionEvents = false;
    // --- FXML Injected Fields ---
    @FXML private Label selectedFileLabel;
    @FXML private ComboBox<PaperSize> paperSizeComboBox;
    @FXML private ComboBox<Integer> dpiComboBox;
    @FXML private CheckBox colorCheckBox;
    @FXML private CheckBox duplexCheckBox;
    @FXML private Button submitButton;
    @FXML private ImageView previewImageView;
    @FXML private StackPane previewContainer;
    @FXML private TableView<PrintJob> jobTableView;
    @FXML private TableColumn<PrintJob, String> jobIdColumn;
    @FXML private TableColumn<PrintJob, String> documentNameColumn;
    @FXML private TableColumn<PrintJob, PrintJobStatus> statusColumn;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;
    @FXML private Button confirmPrintButton;

    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        logger.info("Initializing MainController...");

        paperSizeComboBox.getItems().setAll(PaperSize.A4, PaperSize.LETTER);
        paperSizeComboBox.setValue(PaperSize.A4);
        dpiComboBox.getItems().setAll(150, 300, 600);
        dpiComboBox.setValue(300);
        submitButton.setDisable(true);

        previewImageView.fitWidthProperty().bind(previewContainer.widthProperty());
        previewImageView.fitHeightProperty().bind(previewContainer.heightProperty());

        confirmPrintButton.managedProperty().bind(confirmPrintButton.visibleProperty());
        
        setupJobTable();
        startTimeline();
        jobTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (suppressSelectionEvents) return;
                    onJobSelectionChanged(newSelection);
                }
        );
        updatePreviewImageAndControls();
    }

    public void setSpoolerService(SpoolerService spoolerService) {
        this.spoolerService = spoolerService;
        refreshJobQueue();
    }

    public void setVirtualPrinter(VirtualPrinter virtualPrinter) {
        this.virtualPrinter = virtualPrinter;
        this.virtualPrinter.setPagePrintListener(this::handlePagePrinted);
    }

    private void setupJobTable() {
        jobIdColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getJobId().substring(0, 8) + "...")
        );
        documentNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDocumentName())
        );
        statusColumn.setCellValueFactory(cellData ->
            new SimpleObjectProperty<>(cellData.getValue().getStatus())
        );

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(PrintJobStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null); setStyle("");
                } else {
                    setText(status.name());
                    setTextFill(switch (status) {
                        case QUEUED -> Color.GRAY;
                        case PREVIEWING -> Color.ORANGE;
                        case PRINTING -> Color.BLACK;
                        case COMPLETED -> Color.GREEN;
                        case FAILED -> Color.RED;
                        case CANCELLED, PAUSED -> Color.DARKGRAY;
                    });
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        jobTableView.setRowFactory(tv -> {
            final TableRow<PrintJob> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            final Tooltip tooltip = new Tooltip();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setContextMenu(null);
                    row.setTooltip(null);
                } else {
                    rowMenu.getItems().clear();
                    switch (newItem.getStatus()) {
                        case QUEUED, PREVIEWING, PRINTING -> {
                            MenuItem cancelItem = new MenuItem("Cancel Job");
                            cancelItem.setOnAction(e -> spoolerService.cancelJob(newItem.getJobId()));
                            rowMenu.getItems().add(cancelItem);
                        }
                        case FAILED -> {
                            MenuItem retryItem = new MenuItem("Retry Job");
                            retryItem.setOnAction(e -> spoolerService.retryJob(newItem.getJobId()));
                            MenuItem removeItem = new MenuItem("Remove Job");
                            removeItem.setOnAction(e -> {
                                spoolerService.removeJob(newItem.getJobId());
                                refreshJobQueue();
                            });
                            rowMenu.getItems().addAll(retryItem, new SeparatorMenuItem(), removeItem);
                        }
                        case COMPLETED, CANCELLED -> {
                            MenuItem removeItem = new MenuItem("Remove Job");
                            removeItem.setOnAction(e -> {
                                spoolerService.removeJob(newItem.getJobId());
                                refreshJobQueue();
                            });
                            rowMenu.getItems().add(removeItem);
                        }
                    }
                    row.setContextMenu(rowMenu);
                    if (newItem.getStatus() == PrintJobStatus.FAILED && !newItem.getErrorLog().isEmpty()) {
                        tooltip.setText(String.join("\n", newItem.getErrorLog()));
                        row.setTooltip(tooltip);
                    } else {
                        row.setTooltip(null);
                    }
                }
            });
            return row;
        });
    }

    private void startTimeline() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshJobQueue()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    /**
     * 仅刷新表格数据并保持选择稳定；必要时再刷新预览，避免每秒闪烁。
     */
    private void refreshJobQueue() {
        if (spoolerService == null) return;

        // 记录当前选中项
        PrintJob beforeSelected = jobTableView.getSelectionModel().getSelectedItem();
        String selectedJobId = beforeSelected != null ? beforeSelected.getJobId() : null;

        List<PrintJob> jobs = spoolerService.listJobs();

        // 屏蔽选择事件，避免 onJobSelectionChanged(null) 清空预览
        suppressSelectionEvents = true;
        try {
            jobTableView.getItems().setAll(jobs);

            // 按 ID 恢复选择
            if (selectedJobId != null) {
                for (int i = 0; i < jobs.size(); i++) {
                    if (jobs.get(i).getJobId().equals(selectedJobId)) {
                        jobTableView.getSelectionModel().select(i);
                        break;
                    }
                }
            }
        } finally {
            suppressSelectionEvents = false;
        }

        // 仅当选中作业的状态变化时，才触发预览刷新
        PrintJob nowSelected = jobTableView.getSelectionModel().getSelectedItem();
        if (nowSelected != null && selectedJobLastStatus != nowSelected.getStatus()) {
            logger.debug("Status of selected job changed from {} to {}. Triggering preview refresh.",
                    selectedJobLastStatus, nowSelected.getStatus());
            onJobSelectionChanged(nowSelected);
        }
    }
    
    /**
     * Handles the real-time page rendering event from the background.
     */
    private void handlePagePrinted(PrintJob job, BufferedImage renderedPage, int pageNumber) {
        PrintJob selectedJob = jobTableView.getSelectionModel().getSelectedItem();
        if (selectedJob != null && selectedJob.getJobId().equals(job.getJobId())) {
            final Image fxImage = SwingFXUtils.toFXImage(renderedPage, null);
            Platform.runLater(() -> {
                // This is a live-update, so we directly manipulate the preview state
                previewPages.add(fxImage);
                currentPageIndex = previewPages.size() - 1;
                updatePreviewImageAndControls();
            });
        }
    }

    /**
     * Triggered ONLY by user selection change or a status change of the selected item.
     */
    private void onJobSelectionChanged(PrintJob selectedJob) {
        previewPages.clear();
        currentPageIndex = -1;
        selectedJobLastStatus = selectedJob != null ? selectedJob.getStatus() : null;

        if (selectedJob == null) {
            updatePreviewImageAndControls();
            return;
        }

        boolean canPreview = selectedJob.getStatus() == PrintJobStatus.PREVIEWING ||
                             selectedJob.getStatus() == PrintJobStatus.COMPLETED;

        if (canPreview) {
            loadPreviewPagesForJob(selectedJob);
        } else {
            updatePreviewImageAndControls();
        }
    }

    private void loadPreviewPagesForJob(PrintJob job) {
        Path pagesDir = Paths.get("output", job.getJobId(), VirtualPrinter.RENDERED_PAGES_DIR_NAME);
        if (!Files.exists(pagesDir)) {
            Platform.runLater(this::updatePreviewImageAndControls);
            return;
        }

        Task<List<Image>> loadTask = new Task<>() {
            @Override
            protected List<Image> call() throws Exception {
                try (Stream<Path> paths = Files.list(pagesDir)) {
                    return paths.filter(p -> p.toString().endsWith(".png"))
                                .sorted(Comparator.naturalOrder())
                                .map(p -> new Image(p.toUri().toString(), true)) // Load in background
                                .collect(Collectors.toList());
                }
            }
        };
        loadTask.setOnSucceeded(event -> {
            previewPages = loadTask.getValue();
            if (!previewPages.isEmpty()) {
                currentPageIndex = 0;
            }
            updatePreviewImageAndControls();
        });
        loadTask.setOnFailed(event -> {
            logger.error("Failed to load preview pages for job {}", job.getJobId(), loadTask.getException());
            updatePreviewImageAndControls();
        });
        new Thread(loadTask).start();
    }
    
    /**
     * Centralized method to update all preview-related UI elements.
     * This is the ONLY place where previewImageView.setImage() should be called.
     */
    private void updatePreviewImageAndControls() {
        PrintJob selectedJob = jobTableView.getSelectionModel().getSelectedItem();
        boolean isPreviewing = selectedJob != null && selectedJob.getStatus() == PrintJobStatus.PREVIEWING;
        confirmPrintButton.setVisible(isPreviewing);

        Image imageToShow = null;
        if (currentPageIndex != -1 && !previewPages.isEmpty()) {
            imageToShow = previewPages.get(currentPageIndex);
        }
        
        if (previewImageView.getImage() != imageToShow) {
            previewImageView.setImage(imageToShow);
        }
        
        pageInfoLabel.setText(String.format("Page: %d / %d", currentPageIndex + 1, previewPages.size()));
        prevPageButton.setDisable(currentPageIndex <= 0);
        nextPageButton.setDisable(currentPageIndex >= previewPages.size() - 1);
    }
    
    @FXML private void handlePrevPage() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            updatePreviewImageAndControls();
        }
    }

    @FXML private void handleNextPage() {
        if (currentPageIndex < previewPages.size() - 1) {
            currentPageIndex++;
            updatePreviewImageAndControls();
        }
    }

    @FXML
    private void handleConfirmPrint() {
        PrintJob selectedJob = jobTableView.getSelectionModel().getSelectedItem();
        if (selectedJob != null && selectedJob.getStatus() == PrintJobStatus.PREVIEWING) {
            spoolerService.confirmPrint(selectedJob.getJobId());
        }
    }

    @FXML
    private void handleSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Text File to Print");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(selectedFileLabel.getScene().getWindow());
        if (file != null) {
            previewImageView.setImage(null);
            selectedSourceFile = file;
            selectedFileLabel.setText(file.getName());
            submitButton.setDisable(false);
            logger.info("User selected file: {}", file.getAbsolutePath());
        }
    }

    @FXML
    private void handleSubmitJob() {
        if (selectedSourceFile == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "No file selected.");
            return;
        }
        try {
            PrintSettings settings = createSettingsFromUI();
            String jobSpecificId = UUID.randomUUID().toString();
            Path jobSpoolDir = spoolerService.getSpoolDirectory().resolve(jobSpecificId);
            Files.createDirectories(jobSpoolDir);
            Path spoolFilePath = jobSpoolDir.resolve(selectedSourceFile.getName());
            Files.copy(selectedSourceFile.toPath(), spoolFilePath, StandardCopyOption.REPLACE_EXISTING);

            PrintJob newJob = new PrintJob(selectedSourceFile.getName(), System.getProperty("user.name"), settings, List.of(spoolFilePath.toAbsolutePath().toString()));
            spoolerService.submit(newJob);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Print job submitted.");
            resetInputFields();
        } catch (IOException e) {
            logger.error("Failed to submit job", e);
            showAlert(Alert.AlertType.ERROR, "Submission Failed", "Could not process the file.");
        }
    }

    private PrintSettings createSettingsFromUI() {
        return new PrintSettings(paperSizeComboBox.getValue(), dpiComboBox.getValue(), colorCheckBox.isSelected(), duplexCheckBox.isSelected(), 1.0, 1);
    }

    private void resetInputFields() {
        selectedSourceFile = null;
        selectedFileLabel.setText("No file selected.");
        submitButton.setDisable(true);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public void stopTimeline() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}