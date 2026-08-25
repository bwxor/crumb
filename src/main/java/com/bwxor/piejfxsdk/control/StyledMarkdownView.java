package com.bwxor.piejfxsdk.control;

import com.bwxor.piejfxsdk.state.ServiceState;
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
}