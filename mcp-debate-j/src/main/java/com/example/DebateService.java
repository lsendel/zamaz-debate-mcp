package com.example;

import java.util.List;
import java.util.UUID;

public class DebateService {
    private final DebateRepository debateRepository;

    public DebateService(DebateRepository debateRepository) {
        this.debateRepository = debateRepository;
    }

    public Debate createDebate(String name, String topic, String description, List<Participant> participants) {
        String id = UUID.randomUUID().toString();
        Debate debate = new Debate(id, name, topic, description, participants);
        debateRepository.save(debate);
        return debate;
    }

    public Debate startDebate(String debateId) {
        Debate debate = debateRepository.findById(debateId);
        if (debate != null) {
            debate.setStatus(DebateStatus.ACTIVE);
            debateRepository.save(debate);
        }
        return debate;
    }

    public Debate getDebate(String debateId) {
        return debateRepository.findById(debateId);
    }
}
