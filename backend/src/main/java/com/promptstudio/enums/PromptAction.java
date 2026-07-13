package com.promptstudio.enums;

/**
 * Represents the action/operation that produced a given Prompt record.
 * Distinguishes Generator, Optimizer, Analyzer, and Battle Arena
 * results within the single unified Prompt collection, and also flags
 * manually created/saved library entries.
 */
public enum PromptAction {

    /** Created via the Prompt Generator feature. */
    GENERATE("Prompt Generator"),

    /** Created via the Prompt Optimizer feature. */
    OPTIMIZE("Prompt Optimizer"),

    /** Created via the Prompt Analyzer feature. */
    ANALYZE("Prompt Analyzer"),

    /** Created via the Prompt Battle Arena feature. */
    BATTLE("Prompt Battle Arena"),

    /** Manually saved directly to the library by the user. */
    MANUAL("Manually Saved");

    private final String label;

    PromptAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}