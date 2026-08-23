package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.state.ServiceState;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class MenuFactory {
    public static Menu createClaudeAgentMenu() {
        ServiceState serviceState = ServiceState.instance;

        Menu menu = new Menu("Claude");
        MenuItem settingsMenuItem = new MenuItem("Settings");
        settingsMenuItem.setOnAction(_ -> {});
        menu.getItems().add(settingsMenuItem);

        return menu;
    }
}
