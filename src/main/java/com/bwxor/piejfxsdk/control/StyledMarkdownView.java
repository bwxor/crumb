package com.bwxor.piejfxsdk.control;

import com.bwxor.piejfxsdk.state.ServiceState;
import one.jpro.platform.mdfx.MarkdownCodeBlock;
import one.jpro.platform.mdfx.MarkdownView;

import java.util.Objects;

public class StyledMarkdownView extends MarkdownView {
    private static final String CUSTOM_STYLESHEET =
            Objects.requireNonNull(ServiceState.instance.getResourceService().getResourceByName("styles/styles.css")).toExternalForm();

    public StyledMarkdownView(String md) {
        super(md);
    }

    @Override
    public String getUserAgentStylesheet() {
        return CUSTOM_STYLESHEET;
    }

    @Override
    public javafx.scene.Node generateFencedCodeBlock(String code, String language) {
        MarkdownCodeBlock codeBlock = new MarkdownCodeBlock(code, language);

        boolean isPlainDiagram = language == null || language.isBlank();

        System.out.println("Plain diagram: " + isPlainDiagram);

        if (isPlainDiagram) {
            applyDefaultTextColor(codeBlock);
        }

        return codeBlock;
    }

    private void applyDefaultTextColor(javafx.scene.Parent codeBlock) {
        String textFill = "-fx-fill: text-primary";

        Runnable apply = () -> {
            javafx.scene.Node flowNode = codeBlock.lookup(".code-text-flow");
            if (flowNode instanceof javafx.scene.text.TextFlow textFlow) {
                for (javafx.scene.Node child : textFlow.getChildren()) {
                    if (child instanceof javafx.scene.text.Text) {
                        child.setStyle(textFill);
                    }
                }
            }
        };

        apply.run();
        codeBlock.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) javafx.application.Platform.runLater(apply);
        });
    }

    private void overrideLookup(javafx.scene.Parent root, String selector, String style) {
        javafx.scene.Node node = root.lookup(selector);
        if (node != null) {
            node.setStyle(style);
        }
    }
}