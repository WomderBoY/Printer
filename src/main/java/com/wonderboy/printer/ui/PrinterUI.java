package com.wonderboy.printer.ui;

import com.wonderboy.printer.printer.VirtualPrinter;
import com.wonderboy.printer.renderer.SimpleTextRenderer;
import com.wonderboy.printer.service.SpoolerService;
import com.wonderboy.printer.service.SpoolerWorker;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PrinterUI extends Application {
    private static final Logger logger = LoggerFactory.getLogger(PrinterUI.class);

    // --- All backend components are managed here ---
    private SpoolerService spoolerService;
    private SpoolerWorker spoolerWorker;
    private ExecutorService workerExecutor;
    private MainController controller;
    private VirtualPrinter virtualPrinter;

    @Override
    public void init() {
        logger.info("Initializing application backend services...");
        // 1. Create the backend components
        spoolerService = new SpoolerService(Paths.get("spool"));
        virtualPrinter = new VirtualPrinter(Paths.get("output"));
        SimpleTextRenderer renderer = new SimpleTextRenderer();
        spoolerWorker = new SpoolerWorker(spoolerService, renderer, virtualPrinter);

        // 2. Create a single-threaded executor for our worker
        workerExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlLocation = getClass().getResource("/com.wonderboy.printer.ui/MainView.fxml");
        Objects.requireNonNull(fxmlLocation, "Cannot find FXML file. Check the path.");

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        controller = loader.getController();
        controller.setSpoolerService(spoolerService);
        controller.setVirtualPrinter(virtualPrinter);

        // 3. Start the background worker thread
        startBackgroundWorker();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Wonderboy Virtual Printer");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> logger.info("Shutdown requested."));
        primaryStage.show();
    }

    /**
     * Submits a long-running task to the executor service that continuously
     * checks for and processes new print jobs.
     */
    private void startBackgroundWorker() {
        workerExecutor.submit(() -> {
            logger.info("Background SpoolerWorker thread started.");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    boolean workDone = spoolerWorker.processOneStep();
                    // If no job was found, wait a bit to prevent busy-waiting
                    if (!workDone) {
                        Thread.sleep(1000); // Poll every second
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve the interrupted status
                logger.info("Background SpoolerWorker thread interrupted and shutting down.");
            }
        });
    }

    /**
     * This method is called when the application is closed.
     * It's crucial for a clean shutdown.
     */
    @Override
    public void stop() {
        logger.info("Stopping application...");

        // Stop the UI timeline
        if (controller != null) {
            controller.stopTimeline();
        }

        // Shut down the background worker thread gracefully
        workerExecutor.shutdownNow(); // Use shutdownNow to interrupt the sleeping thread
        try {
            if (!workerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Worker executor did not terminate in 5 seconds.");
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for executor to terminate.", e);
        }
        logger.info("Application stopped.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}