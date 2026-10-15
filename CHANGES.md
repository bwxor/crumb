# Changes

- `src/main/java/com/bwxor/piejfxsdk/factory/TabFactory.java`: the Crumb message history now re-scrolls to the bottom (`Platform.runLater`) whenever its height changes, so new messages are fully scrolled into view instead of leaving a gap. The previous `setVvalue(1.0)` calls ran before layout updated the content height.
