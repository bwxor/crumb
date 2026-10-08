package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@JsonClassDescription("Reads a file from the currently opened directory. The path of the file can be obtained by using the list_workspace_files tool.")
public class ReadWorkspaceFile implements BuiltInTool<String> {

    @JsonPropertyDescription("The path of the file")
    public String path;

    public String execute() {
        if (path == null || path.isBlank()) {
            return "Error: no file path was provided.";
        }

        File openedFolder = OpenedFolderState.instance.getOpenedFolder();
        if (openedFolder == null || !openedFolder.isDirectory()) {
            return "Error: no folder is currently open.";
        }

        // Accept both relative paths (as returned by list_workspace_files) and absolute paths.
        Path root = openedFolder.toPath().toAbsolutePath().normalize();
        Path target = root.resolve(path).normalize();

        if (!target.startsWith(root)) {
            return "Error: path '" + path + "' is outside the opened folder.";
        }

        try {
            return Files.readString(target);
        } catch (IOException e) {
            return "Error: could not read '" + path + "': " + e.getMessage();
        }
    }
}
