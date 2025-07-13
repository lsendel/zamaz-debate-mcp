package com.example;

import java.util.HashMap;
import java.util.Map;

public class InMemoryDebateRepository implements DebateRepository {
    private final Map<String, Debate> debates = new HashMap<>();

    @Override
    public void save(Debate debate) {
        debates.put(debate.getId(), debate);
    }

    @Override
    public Debate findById(String debateId) {
        return debates.get(debateId);
    }
}
