package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.control.StyledMarkdownView;
import com.bwxor.piejfxsdk.state.UIState;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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

    public static Label createToolUseMessage(String toolName, String inputs) {
        UIState uiState = UIState.instance;

        var label = new Label();

        label.setText(toolName + "\nInputs: " + inputs);
        label.setPadding(new Insets(2));
        label.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());
        label.getStyleClass().add("crumb-tool-use-message");

        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);

        return label;
    }
}
