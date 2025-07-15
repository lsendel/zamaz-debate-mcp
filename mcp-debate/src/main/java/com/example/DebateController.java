package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/debates")
public class DebateController {

    private final DebateService debateService;

    @Autowired
    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @PostMapping
    public Debate createDebate(@RequestBody CreateDebateRequest request) {
        return debateService.createDebate(request.getName(), request.getTopic(), request.getDescription(), request.getParticipants());
    }

    @GetMapping("/{debateId}")
    public Debate getDebate(@PathVariable String debateId) {
        return debateService.getDebate(debateId);
    }

    @PostMapping("/{debateId}/start")
    public Debate startDebate(@PathVariable String debateId) {
        return debateService.startDebate(debateId);
    }

    public static class CreateDebateRequest {
        private String name;
        private String topic;
        private String description;
        private List<Participant> participants;

        // Getters and setters
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

        public List<Participant> getParticipants() {
            return participants;
        }

        public void setParticipants(List<Participant> participants) {
            this.participants = participants;
        }
    }
}
