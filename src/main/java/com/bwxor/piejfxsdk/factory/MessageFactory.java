package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.state.UIState;
import com.bwxor.piejfxsdk.type.Role;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class MessageFactory {
    public static Label createMessage(Role role, String message) {
        UIState uiState = UIState.instance;

        // todo do something with the role

        var label = new Label();

        label.setText(message);
        label.setWrapText(true);
        label.setPadding(new Insets(2));
        label.prefWidthProperty().bind(uiState.getMessageHistoryVBox().widthProperty());
        label.getStyleClass().add("piejfx-claude-agent-plugin-message");

        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_LEFT);

        return label;
    }
}
