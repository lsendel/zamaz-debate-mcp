package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DebateService {
    private final DebateRepository debateRepository;

    @Autowired
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
        Debate debate = debateRepository.findById(debateId).orElse(null);
        if (debate != null) {
            debate.setStatus(DebateStatus.ACTIVE);
            debateRepository.save(debate);
        }
        return debate;
    }

    public Debate getDebate(String debateId) {
        return debateRepository.findById(debateId).orElse(null);
    }
}
