package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.control.StyledMarkdownView;
import com.bwxor.piejfxsdk.state.UIState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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
}
