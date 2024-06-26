package com.example.blackjack;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Server extends Application {
    @Override
    public void start(Stage stage)  throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Server.class.getResource("server-control-panel.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("BlackJack");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("blackjackicon.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}