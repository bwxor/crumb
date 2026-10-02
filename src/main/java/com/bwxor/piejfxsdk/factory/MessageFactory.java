package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.control.StyledMarkdownView;
import com.bwxor.piejfxsdk.state.UIState;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import one.jpro.platform.mdfx.MarkdownView;

public class MessageFactory {
    public static Label createUserMessage(String message) {
        UIState uiState = UIState.instance;

        var label = new Label();

        label.setText(message);
        label.setWrapText(true);
        label.setPadding(new Insets(2));
        label.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());
        label.getStyleClass().add("crumb-user-message");

        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);

        return label;
    }

    public static MarkdownView createAssistantMessage(String message) {
        var markdown = new StyledMarkdownView(message);
        return markdown;
    }

    public static Label createWorkingMessage() {
        UIState uiState = UIState.instance;

        var label = new Label();
        label.setPadding(new Insets(2));
        label.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());

        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        int[] index = {0};

        var timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            index[0] = (index[0] + 1) % frames.length;
            label.setText(frames[index[0]] + " Working");
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        label.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene == null) timeline.stop();
            else timeline.play();
        });

        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);

        return label;
    }

    public static VBox createToolUseMessage(String toolName, String inputs) {
        UIState uiState = UIState.instance;

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(2));
        vbox.getStyleClass().add("crumb-tool-use-message-vbox");
        vbox.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());
        vbox.setMinWidth(0);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.setSpacing(5);

        HBox hbox = new HBox();
        hbox.setStyle("-fx-background-color: transparent;");

        var titleLabel = new Label();
        titleLabel.getStyleClass().add("crumb-tool-use-message-label");
        titleLabel.setText(toolName);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var moreLabel = new Label();
        moreLabel.setText("More");
        moreLabel.getStyleClass().add("crumb-tool-use-message-label");
        moreLabel.setOnMouseClicked(e -> {
            if (moreLabel.getText().equals("More")) {
                moreLabel.setText("Less");
                var inputsLabel = new Label();
                inputsLabel.setText(inputs);
                inputsLabel.setWrapText(true);
                inputsLabel.getStyleClass().add("crumb-tool-use-message-label");
                vbox.getChildren().add(inputsLabel);
            }
            else if (moreLabel.getText().equals("Less")) {
                moreLabel.setText("More");
                vbox.getChildren().removeLast();
            }
        });
        moreLabel.setMinWidth(Region.USE_PREF_SIZE);

        hbox.getChildren().addAll(titleLabel, spacer, moreLabel);

        vbox.getChildren().add(hbox);

        return vbox;
    }

    public static VBox createErrorMessage(String message) {
        UIState uiState = UIState.instance;

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(2));
        vbox.getStyleClass().add("crumb-error-message-vbox");
        vbox.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());
        vbox.setMinWidth(0);
        vbox.setMaxWidth(Double.MAX_VALUE);
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.setSpacing(5);

        HBox hbox = new HBox();
        hbox.setStyle("-fx-background-color: transparent;");

        var titleLabel = new Label();
        titleLabel.getStyleClass().add("crumb-special-message-label");
        titleLabel.setText("Error");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var moreLabel = new Label();
        moreLabel.setText("More");
        moreLabel.getStyleClass().add("crumb-special-message-label");
        moreLabel.setOnMouseClicked(e -> {
            if (moreLabel.getText().equals("More")) {
                moreLabel.setText("Less");
                var inputsLabel = new Label();
                inputsLabel.setText(message);
                inputsLabel.setWrapText(true);
                inputsLabel.getStyleClass().add("crumb-special-message-label");
                vbox.getChildren().add(inputsLabel);
            }
            else if (moreLabel.getText().equals("Less")) {
                moreLabel.setText("More");
                vbox.getChildren().removeLast();
            }
        });
        moreLabel.setMinWidth(Region.USE_PREF_SIZE);

        hbox.getChildren().addAll(titleLabel, spacer, moreLabel);

        vbox.getChildren().add(hbox);

        return vbox;
    }
}
