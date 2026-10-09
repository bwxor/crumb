package com.bwxor.piejfxsdk.tool;

import com.anthropic.core.JsonValue;
import com.bwxor.piejfxsdk.dto.DelegateTaskReturnItem;
import com.bwxor.piejfxsdk.dto.Session;
import com.bwxor.piejfxsdk.dto.ToolUseDisplayItem;
import com.bwxor.piejfxsdk.state.ServiceState;
import com.bwxor.piejfxsdk.type.Role;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@JsonClassDescription("""
        Delegate a self-contained subtask to a subagent with its own fresh context.
        Use it for work that needs a lot of reading or exploration, so only a summary comes back.
        The subagent cannot see this conversation: include every detail it needs.""")
public class DelegateTask implements BuiltInTool<DelegateTaskReturnItem> {
    private static class NoDisplay<T> {
        private Consumer<T> display() {
            return (_) -> {
            };
        }
    }

    private static class NoRunnable {
        private Runnable runnable() {
            return () -> {
            };
        }
    }

    private static final int MAX_SUBAGENT_ITERATIONS = 25;
    private static final String SYSTEM_PROMPT = """
            You are a subagent working inside the user's workspace in the Pie editor.
            Another agent delegated a task to you. You cannot ask it questions: if
            something is unclear, make a reasonable choice and mention it in your report.
            
            Rules:
            - Do only the task you were given. Do not make unrelated changes.
            - Read files before modifying them.
            - Do not create or edit CHANGES.md or any documentation files.
            
            When you are done, reply with a concise report for the other agent:
            - What you did or found, with exact file paths and names where relevant.
            - Anything you could not finish, or anything that looked wrong but was out of scope.
            Do not repeat file contents in the report unless they are short and essential.
            """;

    @JsonPropertyDescription("Complete instructions for the subagent, including the files involved and the expected result.")
    public String task;

    public DelegateTaskReturnItem execute() {
        ServiceState serviceState = ServiceState.instance;

        var tempSession = new Session();
        tempSession.setConversation(com.anthropic.models.beta.messages.MessageCreateParams.builder()
                .model("anthropic/claude-haiku-5.5")
                .maxTokens(16000)
                .system(SYSTEM_PROMPT)
                .addTool(ListWorkspaceFiles.class)
                .addTool(ReadWorkspaceFile.class)
                .addTool(WriteWorkspaceFile.class)
                .addTool(ExecuteTerminalCommand.class)
                .putAdditionalBodyProperty("session_id", JsonValue.from(tempSession.getId()))
        );

        tempSession.getConversation().addUserMessage(task);

        Set<String> changedItems = new HashSet<>();

        try {
            serviceState.getClaudeMessageService().runLoop(tempSession,
                    true,
                    false,
                    changedItems,
                    MAX_SUBAGENT_ITERATIONS,
                    new NoDisplay<String>().display(),
                    new NoDisplay<ToolUseDisplayItem>().display(),
                    new NoDisplay<Double>().display(),
                    new NoDisplay<Long>().display(),
                    new NoRunnable().runnable()
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            return result(changedItems, "Status: failed\nError: " + ex.getMessage());
        }

        var sb = new StringBuilder();
        for (var m : tempSession.getMessages()) {
            if (m.role().equals(Role.ASSISTANT)) {
                sb.append(m.content());
            }
        }

        var report = sb.toString().strip();
        return result(changedItems, report.isEmpty()
                ? "Status: incomplete (no report; the subagent may have hit the iteration limit)"
                : report);
    }

    private static DelegateTaskReturnItem result(Set<String> changedItems, String report) {
        String files = changedItems.isEmpty() ? "none" : String.join(", ", changedItems);
        return new DelegateTaskReturnItem(changedItems, "Files written: " + files + "\n\n" + report);
    }

}
