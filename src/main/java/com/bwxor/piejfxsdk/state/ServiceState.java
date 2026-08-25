package com.bwxor.piejfxsdk.state;

import com.bwxor.piejfxsdk.service.ClaudeMessageService;
import com.bwxor.piejfxsdk.service.ConfigurationService;
import com.bwxor.piejfxsdk.service.ResourceService;
import com.bwxor.plugin.service.PluginFileService;
import com.bwxor.plugin.service.PluginNotificationService;

public class ServiceState {
    private ConfigurationService configurationService;
    private PluginFileService fileService;
    private PluginNotificationService notificationService;
    private ResourceService resourceService;
    private ClaudeMessageService claudeMessageService;
    public static final ServiceState instance = new ServiceState();

    private ServiceState() {}

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public PluginFileService getFileService() {
        return fileService;
    }

    public void setFileService(PluginFileService fileService) {
        this.fileService = fileService;
    }

    public PluginNotificationService getNotificationService() {
        return notificationService;
    }

    public void setNotificationService(PluginNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public ClaudeMessageService getClaudeMessageService() {
        return claudeMessageService;
    }

    public void setClaudeMessageService(ClaudeMessageService claudeMessageService) {
        this.claudeMessageService = claudeMessageService;
    }
}
