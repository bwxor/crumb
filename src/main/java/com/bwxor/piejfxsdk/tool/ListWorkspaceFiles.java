package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@JsonClassDescription("Lists all files from the currently opened directory. Use it when you want to find out the paths of the files to access. Is usually called before the file reading tool.")
public class ListWorkspaceFiles {
    @JsonPropertyDescription("Unused placeholder parameter; pass an empty string.")
    public String placeholder = "";

    public List<String> execute() {
        OpenedFolderState openedFolderState = OpenedFolderState.instance;

        File openedFolder = openedFolderState.getOpenedFolder();

        if (openedFolder == null || !openedFolder.isDirectory()) {
            return List.of();
        }

        try(var paths = Files.walk(openedFolder.toPath())) {
            return paths.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
