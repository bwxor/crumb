package com.bwxor.piejfxsdk.state;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class UIState {
    private ListView stagedListView;
    private ListView unstagedListView;
    private TabPane sidebarTabPane;
    private Tab claudeAgentTab;
    private VBox claudeAnswerHomeVBox;
    private VBox claudeAnswerTextVBox;
    private VBox sessionVBox;
    private VBox messageHistoryVBox;
    private ScrollPane messageHistoryScrollPane;
    private TextArea claudePromptTextArea;
    private ProgressBar tokenProgressBar;
    private Label tokenUsageLabel;

    public static final UIState instance = new UIState();

    public ListView getStagedListView() {
        return stagedListView;
    }

    public void setStagedListView(ListView stagedListView) {
        this.stagedListView = stagedListView;
    }

    public ListView getUnstagedListView() {
        return unstagedListView;
    }

    public void setUnstagedListView(ListView unstagedListView) {
        this.unstagedListView = unstagedListView;
    }

    public TabPane getSidebarTabPane() {
        return sidebarTabPane;
    }

    public void setSidebarTabPane(TabPane sidebarTabPane) {
        this.sidebarTabPane = sidebarTabPane;
    }

    public Tab getClaudeAgentTab() {
        return claudeAgentTab;
    }

    public void setClaudeAgentTab(Tab claudeAgentTab) {
        this.claudeAgentTab = claudeAgentTab;
    }

    public VBox getClaudeAnswerHomeVBox() {
        return claudeAnswerHomeVBox;
    }

    public void setClaudeAnswerHomeVBox(VBox claudeAnswerHomeVBox) {
        this.claudeAnswerHomeVBox = claudeAnswerHomeVBox;
    }

    public VBox getClaudeAnswerTextVBox() {
        return claudeAnswerTextVBox;
    }

    public void setClaudeAnswerTextVBox(VBox claudeAnswerTextVBox) {
        this.claudeAnswerTextVBox = claudeAnswerTextVBox;
    }

    public VBox getSessionVBox() {
        return sessionVBox;
    }

    public void setSessionVBox(VBox sessionVBox) {
        this.sessionVBox = sessionVBox;
    }

    public VBox getMessageHistoryVBox() {
        return messageHistoryVBox;
    }

    public void setMessageHistoryVBox(VBox messageHistoryVBox) {
        this.messageHistoryVBox = messageHistoryVBox;
    }

    public ScrollPane getMessageHistoryScrollPane() {
        return messageHistoryScrollPane;
    }

    public void setMessageHistoryScrollPane(ScrollPane messageHistoryScrollPane) {
        this.messageHistoryScrollPane = messageHistoryScrollPane;
    }

    public TextArea getClaudePromptTextArea() {
        return claudePromptTextArea;
    }

    public void setClaudePromptTextArea(TextArea claudePromptTextArea) {
        this.claudePromptTextArea = claudePromptTextArea;
    }

    public ProgressBar getTokenProgressBar() {
        return tokenProgressBar;
    }

    public void setTokenProgressBar(ProgressBar tokenProgressBar) {
        this.tokenProgressBar = tokenProgressBar;
    }

    public Label getTokenUsageLabel() {
        return tokenUsageLabel;
    }

    public void setTokenUsageLabel(Label tokenUsageLabel) {
        this.tokenUsageLabel = tokenUsageLabel;
    }
}
