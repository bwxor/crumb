package com.bwxor.piejfxsdk.dto;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured result the reviewer must hand back. Registered as a tool so the model is forced
 * into a fixed shape instead of free text. It is never "executed": the harness just reads the input.
 */
@JsonClassDescription("Report the result of your review. Call this exactly once, when your review is complete.")
public class ReportReview {

    public enum Verdict { PASS, FAIL }

    public enum Severity { BLOCKING, MINOR }

    @JsonPropertyDescription("PASS if there are no blocking issues, FAIL if there is at least one.")
    public Verdict verdict;

    @JsonPropertyDescription("One or two sentences summarizing the review.")
    public String summary;

    @JsonPropertyDescription("Issues found. Empty list if none.")
    public List<Issue> issues;

    public static class Issue {
        @JsonPropertyDescription("Workspace-relative path of the file.")
        public String file;

        @JsonPropertyDescription("Method, class or line the issue is in.")
        public String location;

        @JsonPropertyDescription("BLOCKING for bugs, missing requirements, compile/runtime errors or an inaccurate CHANGES.md. MINOR for anything else worth mentioning.")
        public Severity severity;

        @JsonPropertyDescription("What is wrong, the evidence in the code, and how to fix it.")
        public String description;

        @Override
        public String toString() {
            return "- [" + severity + "] " + file + (location != null && !location.isBlank() ? " (" + location + ")" : "")
                    + ": " + description;
        }
    }

    public List<Issue> blockingIssues() {
        return issues == null ? List.of() : issues.stream().filter(i -> i.severity == Severity.BLOCKING).toList();
    }

    public List<Issue> minorIssues() {
        return issues == null ? List.of() : issues.stream().filter(i -> i.severity != Severity.BLOCKING).toList();
    }
}