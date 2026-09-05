package com.bwxor.piejfxsdk.state;

import java.io.File;

public class OpenedFolderState {
    private File openedFolder;
    public static final OpenedFolderState instance = new OpenedFolderState();
    private OpenedFolderState() {}

    public File getOpenedFolder() {
        return openedFolder;
    }

    public void setOpenedFolder(File openedFolder) {
        this.openedFolder = openedFolder;
    }
}
