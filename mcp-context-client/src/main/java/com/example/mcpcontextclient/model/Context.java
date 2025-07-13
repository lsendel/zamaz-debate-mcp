package com.example.mcpcontextclient.model;

import lombok.Data;

import java.util.List;

@Data
public class Context {
    private String id;
    private String name;
    private String description;
    private List<Message> messages;
}
