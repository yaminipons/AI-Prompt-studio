package com.promptstudio.enums;

/**
 * Represents the six prompt engineering styles the system can generate.
 * Used by the Prompt Generator and Prompt Battle Arena features, and
 * stored on each Prompt entity to indicate which technique was used.
 */
public enum PromptType {

    /** Direct instruction with no examples provided. */
    ZERO_SHOT("Zero-Shot Prompt", "Direct instruction with no examples"),

    /** Includes input/output examples to guide the model's response pattern. */
    FEW_SHOT("Few-Shot Prompt", "Includes examples to guide the model's response pattern"),

    /** Encourages the model to reason step-by-step before the final answer. */
    CHAIN_OF_THOUGHT("Chain-of-Thought Prompt", "Encourages step-by-step reasoning before the final answer"),

    /** Assigns a specific persona or expert role to the model. */
    ROLE_BASED("Role Prompt", "Assigns a specific persona or expert role to the model"),

    /** Breaks the task into explicit sequential steps. */
    STEP_BY_STEP("Step-by-Step Prompt", "Breaks the task into explicit sequential steps"),

    /** Clear structured instructions with defined constraints and output format. */
    INSTRUCTION("Instruction Prompt", "Clear structured instructions with defined constraints and output format");

    private final String displayName;
    private final String description;

    PromptType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}