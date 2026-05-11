package org.ann.ftp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        Parent root = loader.load();

        org.ann.ftp.gui.Controller controller = loader.getController();

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm()
        );
        primaryStage.setTitle("ann's FTP Client");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
            javafx.application.Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }
}