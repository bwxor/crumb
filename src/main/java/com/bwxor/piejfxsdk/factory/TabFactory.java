package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.state.ServiceState;
import com.bwxor.piejfxsdk.state.UIState;
import com.bwxor.piejfxsdk.type.Role;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class TabFactory {
    public static Tab createClaudeAgentTab() {
        Tab tab = new Tab("Claude");

        SplitPane mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.VERTICAL);
        mainSplitPane.setDividerPosition(0, 0.75);

        tab.setContent(mainSplitPane);

        mainSplitPane.getItems().add(createClaudeAnswerVBox());
        mainSplitPane.getItems().add(createClaudeAgentChatPane());

        return tab;
    }

    private static VBox createClaudeAnswerVBox() {
        UIState uiState = UIState.instance;

        var vBox = new VBox();
        VBox.setVgrow(vBox, Priority.ALWAYS);

        uiState.setClaudeAnswerHomeVBox(createClaudeAnswerHomeVBox());
        uiState.setClaudeAnswerTextVBox(createClaudeAnswerTextVBox());

        vBox.getChildren().add(uiState.getClaudeAnswerHomeVBox());
        vBox.getChildren().add(uiState.getClaudeAnswerTextVBox());

        uiState.getClaudeAnswerTextVBox().setVisible(false);
        uiState.getClaudeAnswerTextVBox().setManaged(false);

        return vBox;
    }

    private static VBox createClaudeAnswerHomeVBox() {
        UIState uiState = UIState.instance;
        ServiceState serviceState = ServiceState.instance;

        var vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        vBox.setPadding(new Insets(15));
        vBox.setSpacing(10);

        ImageView imageView = new ImageView(new Image(serviceState.getResourceService().getResourceByNameAsStream("img/agent_icon.png")));
        imageView.setFitWidth(125);
        imageView.setFitHeight(125);
        imageView.setPreserveRatio(true);

        Label label = new Label("Hi there, I'm your specialized Claude Agent for Pie. How can I help you today?");
        label.setFont(javafx.scene.text.Font.font(14));
        label.setWrapText(true);
        label.setAlignment(Pos.BASELINE_CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);

        var button = new Button("Read Documentation");

        ScrollPane sessionScrollPane = new ScrollPane();
        VBox sessionVBox = new VBox();
        uiState.setSessionVBox(sessionVBox);

        sessionScrollPane.setContent(sessionVBox);

        vBox.getChildren().add(imageView);
        vBox.getChildren().add(label);
        vBox.getChildren().add(button);
        vBox.getChildren().add(sessionScrollPane);

        return vBox;
    }

    private static VBox createClaudeAnswerTextVBox() {
        UIState uiState = UIState.instance;
        ServiceState serviceState = ServiceState.instance;

        var vBox = new VBox();
        VBox.setVgrow(vBox, Priority.ALWAYS);
        vBox.setPadding(new Insets(3));
        vBox.setSpacing(3);

        var hBox = new HBox();
        var region = new Region(); // for button to float right
        HBox.setHgrow(region, Priority.ALWAYS);

        var button = new Button("Close");
        button.setOnMouseClicked(
                e -> {
                    // todo close / save claude context
                    uiState.getClaudeAnswerTextVBox().setVisible(false);
                    uiState.getClaudeAnswerTextVBox().setManaged(false);
                    uiState.getClaudeAnswerHomeVBox().setVisible(true);
                    uiState.getClaudeAnswerHomeVBox().setManaged(true);
                }
        );

        hBox.getChildren().addAll(region, button);

        var scrollPane = new ScrollPane();

        var messageHistoryVBox = new VBox();
        messageHistoryVBox.setSpacing(5);
        messageHistoryVBox.getStylesheets().add(serviceState.getResourceService().getResourceByName("styles/styles.css").toExternalForm());
        scrollPane.setContent(messageHistoryVBox);
        scrollPane.setFitToWidth(true);

        uiState.setMessageHistoryVBox(messageHistoryVBox);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        vBox.getChildren().addAll(hBox, scrollPane);

        return vBox;
    }

    private static VBox createClaudeAgentChatPane() {
        UIState uiState = UIState.instance;

        var textArea = new TextArea();
        textArea.setPromptText("How can I help you today?");
        textArea.setWrapText(true);
        textArea.setOnKeyPressed(
                e -> {
                    if (e.getCode().equals(KeyCode.ENTER) && !e.isShiftDown()) {
                        // todo check if session active, if not create a new one
                        uiState.getClaudeAnswerHomeVBox().setVisible(false);
                        uiState.getClaudeAnswerHomeVBox().setManaged(false);
                        uiState.getClaudeAnswerTextVBox().setVisible(true);
                        uiState.getClaudeAnswerTextVBox().setManaged(true);

                        // todo dummy output
                        uiState.getMessageHistoryVBox().getChildren().add(MessageFactory.createMessage(Role.USER, textArea.getText()));

                        textArea.clear();
                        e.consume();
                    }
                }
        );
        VBox.setVgrow(textArea, Priority.ALWAYS);
        var vbox = new VBox(textArea);
        return vbox;
    }

}
