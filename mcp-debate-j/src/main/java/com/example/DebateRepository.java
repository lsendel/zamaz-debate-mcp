package com.example;

public interface DebateRepository {
    void save(Debate debate);
    Debate findById(String debateId);
}
