package com.bwxor.piejfxsdk;

import com.bwxor.piejfxsdk.factory.MenuFactory;
import com.bwxor.piejfxsdk.factory.TabFactory;
import com.bwxor.piejfxsdk.service.ClaudeMessageService;
import com.bwxor.piejfxsdk.service.ConfigurationService;
import com.bwxor.piejfxsdk.service.ResourceService;
import com.bwxor.piejfxsdk.state.*;
import com.bwxor.plugin.Plugin;
import com.bwxor.plugin.input.PluginContext;
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

public class CrumbPlugin implements Plugin {
    private PluginContext pluginContext;

    @Override
    public void onLoad(PluginContext pluginContext) {
        ServiceState serviceState = ServiceState.instance;
        StylesheetState stylesheetState = StylesheetState.instance;
        UIState uiState = UIState.instance;
        ConfigurationState configurationState = ConfigurationState.instance;

        serviceState.setConfigurationService(new ConfigurationService());
        serviceState.setFileService(pluginContext.getServiceContainer().getFileService());
        serviceState.setNotificationService(pluginContext.getServiceContainer().getNotificationService());
        serviceState.setResourceService(new ResourceService());

        stylesheetState.setThemeURL(pluginContext.getStylesheets().getThemeURL());
        stylesheetState.setDefaultStylesheetURL(pluginContext.getStylesheets().getDefaultStylesURL());
        stylesheetState.setDefaultMaximizedStylesheetURL(pluginContext.getStylesheets().getDefaultMaximizedURL());
        uiState.setSidebarTabPane(pluginContext.getApplicationWindow().getSidebarTabPane());

        this.pluginContext = pluginContext;

        ConfigurationState.instance.setConfigurationDirectory(pluginContext.getConfigurationDirectoryPath());

        try {
            serviceState.getConfigurationService().loadCredentials();

            if (configurationState.getApiKey() != null && !configurationState.getApiKey().trim().isEmpty()) {
                serviceState.setClaudeMessageService(new ClaudeMessageService(configurationState.getApiKey()));
            } else {
                serviceState.getNotificationService().showNotificationOk("Please insert your Claude API Key in the configuration file.");
            }
        } catch (IOException e) {
            serviceState.getNotificationService().showNotificationOk("Error loading your Claude Agent configuration.");
        }

        pluginContext.getApplicationWindow().getMenuBar().getMenus().add(MenuFactory.createClaudeAgentMenu());
        uiState.setClaudeAgentTab(TabFactory.createClaudeAgentTab());
    }

    @Override
    public void onKeyPress(KeyEvent keyEvent) {
    }

    @Override
    public void onSaveFile(File file) {
        ConfigurationState configurationState = ConfigurationState.instance;
        ServiceState serviceState = ServiceState.instance;

        if (file.getPath().equals(Paths.get(configurationState.getConfigurationDirectory().toString(), "config.json").toString())) {
            try {
                serviceState.getConfigurationService().loadCredentials();

                if (configurationState.getApiKey() != null && !configurationState.getApiKey().trim().isEmpty()) {
                    serviceState.setClaudeMessageService(new ClaudeMessageService(configurationState.getApiKey()));
                }
            } catch (IOException e) {
                serviceState.getNotificationService().showNotificationOk("Error loading your Crumb configuration.");
            }
        }
    }

    @Override
    public void onOpenFile(File file) {
    }

    @Override
    public void onOpenFolder(File file) {
        UIState uiState = UIState.instance;

        if (!uiState.getSidebarTabPane().getTabs().contains(uiState.getClaudeAgentTab())) {
            uiState.getSidebarTabPane().getTabs().add(uiState.getClaudeAgentTab());
        }
    }

    @Override
    public void onCreateFile(File file) {
    }

    @Override
    public void onCreateFolder(File file) {
    }

    @Override
    public void onRenameFile(File file) {

    }

    @Override
    public void onDeleteFile(File file) {
    }

    @Override
    public void onThemeChange(URL url) {
        StylesheetState.instance.setThemeURL(url);
    }
}
