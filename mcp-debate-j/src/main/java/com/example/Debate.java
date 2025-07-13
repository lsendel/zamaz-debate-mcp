package com.example;

import java.util.List;

public class Debate {
    private String id;
    private String name;
    private String topic;
    private String description;
    private DebateStatus status;
    private List<Participant> participants;

    public Debate(String id, String name, String topic, String description, List<Participant> participants) {
        this.id = id;
        this.name = name;
        this.topic = topic;
        this.description = description;
        this.status = DebateStatus.DRAFT;
        this.participants = participants;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DebateStatus getStatus() {
        return status;
    }

    public void setStatus(DebateStatus status) {
        this.status = status;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }
}
