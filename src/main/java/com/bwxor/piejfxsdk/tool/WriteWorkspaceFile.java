package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@JsonClassDescription("Writes (creates or overwrites) a file inside the currently opened workspace folder. "
        + "Use a path relative to the workspace root with forward slashes, e.g. 'src/main/java/App.java'. "
        + "Missing parent folders are created automatically.")
public class WriteWorkspaceFile {
    private static final String ERROR_NO_FOLDER = "error: no workspace folder is open";

    @JsonPropertyDescription("File path relative to the workspace root, using forward slashes, e.g. 'src/main/java/App.java'")
    public String path;

    @JsonPropertyDescription("The full content to write to the file")
    public String content;

    public String execute() {
        File openedFolder = OpenedFolderState.instance.getOpenedFolder();
        if (openedFolder == null || !openedFolder.isDirectory()) {
            return ERROR_NO_FOLDER;
        }

        if (path == null || path.isBlank()) {
            return "error: 'path' is required";
        }
        if (content == null) {
            return "error: 'content' is required";
        }

        Path root = openedFolder.toPath().toAbsolutePath().normalize();

        Path target;
        try {
            // "/src/App.java" and "\src\App.java" are treated as relative to the workspace root
            String cleaned = path.trim().replace('\\', '/').replaceFirst("^/+", "");
            Path given = Path.of(cleaned);
            target = (given.isAbsolute() ? given : root.resolve(given))
                    .toAbsolutePath()
                    .normalize();
        } catch (InvalidPathException e) {
            return "error: invalid path '" + path + "': " + e.getReason();
        }

        if (!target.startsWith(root)) {
            return "error: " + target + " is outside the workspace folder " + root
                    + ". Use a path relative to the workspace root, e.g. 'src/main/java/App.java'.";
        }
        if (target.equals(root) || Files.isDirectory(target)) {
            return "error: " + root.relativize(target) + " is a directory, not a file";
        }

        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, content);
            return "ok: wrote " + content.length() + " chars to "
                    + root.relativize(target).toString().replace('\\', '/');
        } catch (IOException e) {
            return "error: could not write " + root.relativize(target) + ": " + e;
        }
    }
}