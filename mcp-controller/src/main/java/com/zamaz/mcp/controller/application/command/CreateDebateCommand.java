package com.zamaz.mcp.controller.application.command;

import com.zamaz.mcp.controller.domain.model.DebateConfig;
import java.util.Objects;

/**
 * Command to create a new debate.
 */
public record CreateDebateCommand(
    String topic,
    DebateConfig config
) {
    
    public CreateDebateCommand {
        Objects.requireNonNull(topic, "Topic cannot be null");
        Objects.requireNonNull(config, "Config cannot be null");
        
        if (topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be empty");
        }
        
        if (topic.length() > 1000) {
            throw new IllegalArgumentException("Topic cannot exceed 1000 characters");
        }
    }
    
    public static CreateDebateCommand of(String topic, DebateConfig config) {
        return new CreateDebateCommand(topic.trim(), config);
    }
    
    public static CreateDebateCommand withDefaults(String topic) {
        return new CreateDebateCommand(topic.trim(), DebateConfig.defaultConfig());
    }
    
    public static CreateDebateCommand quickDebate(String topic) {
        return new CreateDebateCommand(topic.trim(), DebateConfig.quickDebate());
    }
    
    public static CreateDebateCommand longFormDebate(String topic) {
        return new CreateDebateCommand(topic.trim(), DebateConfig.longFormDebate());
    }
    
    public static CreateDebateCommand aiOnlyDebate(String topic) {
        return new CreateDebateCommand(topic.trim(), DebateConfig.aiOnlyDebate());
    }
}