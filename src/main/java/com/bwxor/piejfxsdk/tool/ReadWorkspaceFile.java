package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JsonClassDescription("Reads a file from the currently opened directory. The path of the file can be obtained by using the list_workspace_files tool.")
public class ReadWorkspaceFile implements BuiltInTool<String> {
    private static final String EMPTY_STRING = "";

    @JsonPropertyDescription("The path of the file")
    public String path;

    public String execute() {
        OpenedFolderState openedFolderState = OpenedFolderState.instance;

        File openedFolder = openedFolderState.getOpenedFolder();

        if (path == null || openedFolder == null || !openedFolder.isDirectory()) {
            return EMPTY_STRING;
        }

        Path openedPath = openedFolder.toPath().normalize();
        Path targetPath = Path.of(path).normalize();

        if (!targetPath.startsWith(openedPath)) {
            return EMPTY_STRING;
        }

        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            return EMPTY_STRING;
        }
    }
}
