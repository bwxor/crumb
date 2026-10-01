package com.bwxor.piejfxsdk.tool;

import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@JsonClassDescription("Executes a terminal command in the currently opened workspace directory and returns its combined stdout and stderr output.")
public class ExecuteTerminalCommand implements BuiltInTool<String>{
    private static final String ERROR_NO_FOLDER = "error: no workspace folder is open";
    private static final String ERROR_EXEC = "error: could not execute command";

    @JsonPropertyDescription("The command to execute, passed as a shell command string")
    public String command;

    public String execute() {
        OpenedFolderState openedFolderState = OpenedFolderState.instance;
        File openedFolder = openedFolderState.getOpenedFolder();

        if (openedFolder == null || !openedFolder.isDirectory()) {
            return ERROR_NO_FOLDER;
        }

        if (command == null || command.isBlank()) {
            return ERROR_EXEC;
        }

        try {
            List<String> shellCommand = buildShellCommand(command);
            ProcessBuilder processBuilder = new ProcessBuilder(shellCommand);
            processBuilder.directory(openedFolder);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            process.waitFor();
            return output;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ERROR_EXEC;
        }
    }

    private static List<String> buildShellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return List.of("cmd.exe", "/c", command);
        } else {
            return List.of("/bin/sh", "-c", command);
        }
    }
}
