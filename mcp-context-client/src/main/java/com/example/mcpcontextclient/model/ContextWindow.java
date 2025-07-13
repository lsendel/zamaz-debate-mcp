package com.example.mcpcontextclient.model;

import lombok.Data;

import java.util.List;

@Data
public class ContextWindow {
    private List<Message> messages;
    private int tokenCount;
}
