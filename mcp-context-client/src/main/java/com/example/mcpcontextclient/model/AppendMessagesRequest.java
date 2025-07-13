package com.example.mcpcontextclient.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppendMessagesRequest {
    private String contextId;
    private List<Message> messages;
}
