# Changes

- `src/main/java/com/bwxor/piejfxsdk/tool/ReadWorkspaceFile.java`: Fixed the tool returning an empty string for every read. Relative paths from `list_workspace_files` are now resolved against the opened folder before the containment check, and the same resolved path is read. Failures (no path, no open folder, path outside the folder, I/O error) now return a descriptive `Error: ...` message instead of `""`. Removed the unused `EMPTY_STRING` constant and `List` import.
