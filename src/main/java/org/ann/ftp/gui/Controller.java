package org.ann.ftp.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.ann.ftp.client.FTPClient;
import org.ann.ftp.client.FTPFile;
import org.ann.ftp.util.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {

    //==== FXML UI components
    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Button btnConnect;

    @FXML private ListView<String> listLocal;
    @FXML private TableView<FTPFile> tableRemote;
    @FXML private TableColumn<FTPFile, String> colName;
    @FXML private TableColumn<FTPFile, String> colSize;
    @FXML private TableColumn<FTPFile, String> colDate;
    @FXML private TableColumn<FTPFile, String> colPerms;
    @FXML private Label lblLocalPath;
    @FXML private Label lblRemotePath;
    @FXML private TextArea txtLog;

    // -==== application state ==
    private FTPClient ftpClient;
    private File currentLocalDir;
    private String currentRemoteDir = "/";
    private boolean isBusy = false;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * automatically called by JavaFX after the FXML file is loaded
     */
    @FXML
    public void initialize() {
        ftpClient = new FTPClient();
        currentLocalDir = new File(System.getProperty("user.home"));
        setupTableViewColumns();
        setupTableViewDoubleClicks();
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

        log("Connecting to " + host + ":" + port);
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
        log("Disconnecting");
        runFtpTask(() -> ftpClient.quit(), () -> {
            tableRemote.getItems().clear();
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
                log("Uploading " + selection);
                runFtpTask(() -> ftpClient.put(localFile.getAbsolutePath(), selection),
                        this::refreshRemoteDirectory);
            } else {
                log("Cannot upload directories yet.");
            }
        }
    }

    @FXML
    void handleDownload(ActionEvent event) {
        FTPFile selection = tableRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")){
            String remoteFile = selection.getName();
            File targetFile = new File(currentLocalDir, remoteFile);
            log("Downloading " + remoteFile);
            runFtpTask(() -> ftpClient.get(remoteFile, targetFile.getAbsolutePath()),
                    () -> {
                        log("Download complete.");
                        loadLocalDirectory();
                    });
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        FTPFile selection = tableRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            String remoteFile = selection.getName();
            log("Deleting file: " + remoteFile);
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
        FTPFile selection = tableRemote.getSelectionModel().getSelectedItem();
        if (selection != null && !selection.equals("..")) {
            String targetDir = selection.getName();
            log("Removing directory: " + targetDir);
            runFtpTask(() -> ftpClient.rmdir(targetDir), this::refreshRemoteDirectory);
        } else {
            log("Please select a directory to remove.");
        }
    }

    // =========================================================================
    // UI BEHAVIOR & HELPERS
    // =========================================================================
    private void setupTableViewColumns(){
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeFormatted"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colPerms.setCellValueFactory(new PropertyValueFactory<>("permissions"));

    }

    private void setupTableViewDoubleClicks() {
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

        tableRemote.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                FTPFile selection = tableRemote.getSelectionModel().getSelectedItem();
                if (selection != null) {
                    String targetDir = selection.equals("..") ? ".." : selection.getName();
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
        log("Fetching remote directory list");
        runFtpTask(() -> {
            List<String> rawFiles = ftpClient.list("");

            Platform.runLater(() -> {
                lblRemotePath.setText(currentRemoteDir);
                tableRemote.getItems().clear();

                // "go back" entry
                tableRemote.getItems().add(new FTPFile("..", 0, "--", "drwxrwxrwx", true, ""));

                for (String rawLine : rawFiles) {
                    FTPFile parsedFile = ListParser.parse(rawLine);
                    if (parsedFile != null) {
                        tableRemote.getItems().add(parsedFile);
                    }
                }
            });
        }, () -> log("Remote directory updated."));
    }

    private void log(String message) {
        Platform.runLater(() -> txtLog.appendText(message + "\n"));
    }


    private void runFtpTask(NetworkTask taskLogic, Runnable onSuccess) {
        if (isBusy) {
            log("Command ignored: Waiting for previous task to finish");
            return;
        }
        isBusy = true;

        if (btnConnect.getScene() != null) {
            btnConnect.getScene().setCursor(javafx.scene.Cursor.WAIT); // loading spinner
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                taskLogic.execute();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            isBusy = false;
            if (btnConnect.getScene() != null) {
                btnConnect.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
            }
            if (onSuccess != null) onSuccess.run();
        });

        task.setOnFailed(e -> {
            isBusy = false;
            if (btnConnect.getScene() != null) {
                btnConnect.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
            }
            Throwable ex = task.getException();
            log("ERROR: " + ex.getMessage());

            if (!(ex instanceof org.ann.ftp.util.FTPException)) {
                System.err.println("\n--- SYSTEM ERROR ---");
                ex.printStackTrace();
            }
        });
        executor.submit(task);
    }

    public void shutdown() {
        System.out.println("Initiating shutdown");

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        if (ftpClient != null) {
            try {
                ftpClient.quit();
            } catch (Exception e) {
            }
        }
    }

    @FunctionalInterface
    private interface NetworkTask {
        void execute() throws Exception;
    }
}
