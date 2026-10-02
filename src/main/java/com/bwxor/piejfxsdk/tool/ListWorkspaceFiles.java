package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JsonClassDescription("Lists the files in the currently opened directory as paths relative to the workspace root. Build output, dependency and hidden folders (node_modules, target, build, .git, ...) are skipped. Use it to find the paths of files to read.")
public class ListWorkspaceFiles implements BuiltInTool<List<String>> {
    private static final Set<String> IGNORED_DIRS = Set.of(
            "node_modules", "target", "build", "out", "dist", "bin", "obj",
            "__pycache__", "venv", "vendor"
    );

    private static final int MAX_FILES = 1000;

    @JsonPropertyDescription("Unused placeholder parameter; pass an empty string.")
    public String placeholder = "";

    public List<String> execute() {
        OpenedFolderState openedFolderState = OpenedFolderState.instance;

        File openedFolder = openedFolderState.getOpenedFolder();

        if (openedFolder == null || !openedFolder.isDirectory()) {
            return List.of();
        }

        Path root = openedFolder.toPath();
        List<String> files = new ArrayList<>();
        boolean[] truncated = {false};

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    // skip ignored folders and hidden ones (.git, .idea, .gradle, ...)
                    if (IGNORED_DIRS.contains(name) || name.startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (files.size() >= MAX_FILES) {
                        truncated[0] = true;
                        return FileVisitResult.TERMINATE;
                    }
                    files.add(root.relativize(file).toString().replace('\\', '/'));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE; // skip unreadable files instead of failing
                }
            });
        } catch (IOException e) {
            return List.of();
        }

        if (truncated[0]) {
            files.add("... (list truncated at " + MAX_FILES + " files, more exist)");
        }
        return files;
    }
}
