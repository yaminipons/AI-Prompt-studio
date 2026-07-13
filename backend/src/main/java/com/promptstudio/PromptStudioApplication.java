package com.promptstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the AI Prompt Engineering Studio backend application.
 * <p>
 * Provides REST APIs for authentication, AI-powered prompt generation,
 * optimization, analysis, battle comparison, library/history/collections
 * management, AI chat, export, and admin operations.
 * </p>
 */
@SpringBootApplication
@EnableAsync
public class PromptStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptStudioApplication.class, args);
    }

}