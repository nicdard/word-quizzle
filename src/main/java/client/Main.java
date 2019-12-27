package client;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.Dimension;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        /*final double rem = Math.rint(new Text("").getLayoutBounds().getHeight());
        System.out.println(rem);
        */
        Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth() / 3;
        double height = screenSize.getHeight() / 2;

        primaryStage.setTitle("Wold-Quizzle");
        GridPane formContainer = new GridPane();
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setHgap(10);
        formContainer.setVgap(10);
        formContainer.setPadding(new Insets(25, 25, 25, 25));

        Text sceneTitle = new Text("Welcome");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        formContainer.add(sceneTitle, 0, 0, 2, 1);

        Label userName = new Label("User Name:");
        formContainer.add(userName, 0, 1);

        TextField userTextField = new TextField();
        formContainer.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        formContainer.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        formContainer.add(pwBox, 1, 2);

        Button btn = new Button("Register");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        formContainer.add(hbBtn, 1, 4);
        final Text actiontarget = new Text();
        formContainer.add(actiontarget, 1, 6);

        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                // TODO RMI registration
                actiontarget.setFill(Color.FIREBRICK);
                actiontarget.setText("Sign in button pressed");
            }
        });

        Scene scene = new Scene(formContainer, width, height);
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
