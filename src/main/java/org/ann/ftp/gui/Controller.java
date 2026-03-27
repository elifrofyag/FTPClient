package org.ann.ftp.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ann.ftp.client.FTPClient;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class Controller {

    // --- FXML Injected UI Components ---
    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Button btnConnect;

    @FXML private ListView<String> listLocal;
    @FXML private ListView<String> listRemote;
    @FXML private Label lblLocalPath;
    @FXML private Label lblRemotePath;
    @FXML private TextArea txtLog;

    // --- Application State ---
    private FTPClient ftpClient;
    private File currentLocalDir;
    private String currentRemoteDir = "/";

    /**
     * This method is automatically called by JavaFX after the FXML file is loaded.
     * It is the equivalent of a constructor for your UI logic.
     */
    @FXML
    public void initialize() {
        ftpClient = new FTPClient();
        currentLocalDir = new File(System.getProperty("user.home"));

        setupListViewDoubleClicks();
        loadLocalDirectory();
    }

    // =========================================================================
    // BUTTON ACTION HANDLERS
    // =========================================================================

    @FXML
    void handleConnect(ActionEvent event) {
        String host = txtHost.getText();
        int port = Integer.parseInt(txtPort.getText());
        String user = txtUser.getText();
        String pass = txtPass.getText();

        log("Connecting to " + host + ":" + port + "...");
        runFtpTask(() -> {
            ftpClient.connect(host, port);
            ftpClient.login(user, pass);
            currentRemoteDir = ftpClient.pwd();
        }, () -> {
            log("Login successful!");
            refreshRemoteDirectory();
        });
    }

    @FXML
    void handleDisconnect(ActionEvent event) {
        log("Disconnecting...");
        runFtpTask(() -> ftpClient.quit(), () -> {
            listRemote.getItems().clear();
            lblRemotePath.setText("Disconnected");
            log("Disconnected safely.");
        });
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        refreshRemoteDirectory();
    }

    @FXML
    void handleUpload(ActionEvent event) {
        String selection = listLocal.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            File localFile = new File(currentLocalDir, selection);
            if (localFile.isFile()) {
                log("Uploading " + selection + "...");
                runFtpTask(() -> ftpClient.put(localFile.getAbsolutePath(), selection),
                        this::refreshRemoteDirectory);
            } else {
                log("Cannot upload directories yet.");
            }
        }
    }

    @FXML
    void handleDownload(ActionEvent event) {
        String selection = listRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            String remoteFile = extractFileName(selection);
            File targetFile = new File(currentLocalDir, remoteFile);
            log("Downloading " + remoteFile + "...");
            runFtpTask(() -> ftpClient.get(remoteFile, targetFile.getAbsolutePath()),
                    () -> {
                        log("Download complete.");
                        loadLocalDirectory();
                    });
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        String selection = listRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            String remoteFile = extractFileName(selection);
            log("Deleting file: " + remoteFile + "...");
            runFtpTask(() -> ftpClient.delete(remoteFile), this::refreshRemoteDirectory);
        }
    }

    @FXML
    void handleMkdir(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog("new_folder");
        dialog.setTitle("Create Directory");
        dialog.setHeaderText("Enter name for new directory:");
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(dirName -> {
            log("Creating directory: " + dirName);
            runFtpTask(() -> ftpClient.mkdir(dirName), this::refreshRemoteDirectory);
        });
    }

    @FXML
    void handleRmdir(ActionEvent event) {
        String selection = listRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            String targetDir = extractFileName(selection);
            log("Removing directory: " + targetDir + "...");
            runFtpTask(() -> ftpClient.rmdir(targetDir), this::refreshRemoteDirectory);
        } else {
            log("Please select a directory to remove.");
        }
    }

    // =========================================================================
    // UI BEHAVIOR & HELPERS
    // =========================================================================

    private void setupListViewDoubleClicks() {
        listLocal.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selection = listLocal.getSelectionModel().getSelectedItem();
                if (selection != null) {
                    if (selection.equals("..")) {
                        currentLocalDir = currentLocalDir.getParentFile();
                    } else {
                        File clickedFile = new File(currentLocalDir, selection);
                        if (clickedFile.isDirectory()) {
                            currentLocalDir = clickedFile;
                        }
                    }
                    loadLocalDirectory();
                }
            }
        });

        listRemote.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String selection = listRemote.getSelectionModel().getSelectedItem();
                if (selection != null) {
                    String targetDir = selection.equals("..") ? ".." : extractFileName(selection);
                    log("Navigating to: " + targetDir);
                    runFtpTask(() -> {
                        ftpClient.cd(targetDir);
                        currentRemoteDir = ftpClient.pwd();
                    }, this::refreshRemoteDirectory);
                }
            }
        });
    }

    private void loadLocalDirectory() {
        if (currentLocalDir == null) return;

        lblLocalPath.setText(currentLocalDir.getAbsolutePath());
        listLocal.getItems().clear();

        if (currentLocalDir.getParentFile() != null) {
            listLocal.getItems().add("..");
        }

        File[] files = currentLocalDir.listFiles();
        if (files != null) {
            for (File f : files) {
                listLocal.getItems().add(f.getName() + (f.isDirectory() ? "/" : ""));
            }
        }
    }

    private void refreshRemoteDirectory() {
        log("Fetching remote directory list...");
        runFtpTask(() -> {
            List<String> files = ftpClient.list("");
            Platform.runLater(() -> {
                lblRemotePath.setText(currentRemoteDir);
                listRemote.getItems().clear();
                listRemote.getItems().add("..");
                listRemote.getItems().addAll(files);
            });
        }, () -> log("Remote directory updated."));
    }

    private void log(String message) {
        Platform.runLater(() -> txtLog.appendText(message + "\n"));
    }

    private String extractFileName(String rawListLine) {
        if (rawListLine == null || rawListLine.trim().isEmpty()) return "";
        String[] parts = rawListLine.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private void runFtpTask(NetworkTask taskLogic, Runnable onSuccess) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                taskLogic.execute();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            if (onSuccess != null) onSuccess.run();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();

            log("ERROR: " + ex.getMessage());

            if (!(ex instanceof org.ann.ftp.util.FTPException)) {
                System.err.println("\n--- UNEXPECTED SYSTEM ERROR ---");
                ex.printStackTrace();
            }
        });

        new Thread(task).start();
    }

    @FunctionalInterface
    private interface NetworkTask {
        void execute() throws Exception;
    }
}
