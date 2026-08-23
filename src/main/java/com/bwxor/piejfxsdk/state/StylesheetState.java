package com.bwxor.piejfxsdk.state;

import java.net.URL;

public class StylesheetState {
    private URL themeURL;
    private URL defaultStylesheetURL;
    private URL defaultMaximizedStylesheetURL;

    public static final StylesheetState instance = new StylesheetState();
    private StylesheetState() {}

    public URL getThemeURL() {
        return themeURL;
    }

    public void setThemeURL(URL themeURL) {
        this.themeURL = themeURL;
    }

    public URL getDefaultStylesheetURL() {
        return defaultStylesheetURL;
    }

    public void setDefaultStylesheetURL(URL defaultStylesheetURL) {
        this.defaultStylesheetURL = defaultStylesheetURL;
    }

    public URL getDefaultMaximizedStylesheetURL() {
        return defaultMaximizedStylesheetURL;
    }

    public void setDefaultMaximizedStylesheetURL(URL defaultMaximizedStylesheetURL) {
        this.defaultMaximizedStylesheetURL = defaultMaximizedStylesheetURL;
    }
}
