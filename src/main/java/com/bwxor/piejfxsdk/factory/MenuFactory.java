package com.bwxor.piejfxsdk.factory;

import com.bwxor.piejfxsdk.state.ConfigurationState;
import com.bwxor.piejfxsdk.state.ServiceState;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.nio.file.Paths;

public class MenuFactory {
    public static Menu createClaudeAgentMenu() {
        ServiceState serviceState = ServiceState.instance;
        ConfigurationState configurationState = ConfigurationState.instance;

        Menu menu = new Menu("Crumb");

        MenuItem settingsMenuItem = new MenuItem("Settings");
        settingsMenuItem.setOnAction(_ -> serviceState.getFileService().openFile(Paths.get(configurationState.getConfigurationDirectory().toString(), "config.json").toFile()));

        MenuItem mcpServersMenuItem = new MenuItem("MCP Servers");
        mcpServersMenuItem.setOnAction(_ -> serviceState.getFileService().openFile(Paths.get(configurationState.getConfigurationDirectory().toString(), "mcp_servers.json").toFile()));

        MenuItem permissionsMenuItem = new MenuItem("Permissions");
        permissionsMenuItem.setOnAction(_ -> serviceState.getFileService().openFile(Paths.get(configurationState.getConfigurationDirectory().toString(), "permissions.json").toFile()));

        menu.getItems().addAll(settingsMenuItem, mcpServersMenuItem, permissionsMenuItem);

        return menu;
    }
}
