package com.example;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DebateServiceTest {

    @Test
    public void testCreateDebate() {
        DebateRepository debateRepository = new InMemoryDebateRepository();
        DebateService debateService = new DebateService(debateRepository);

        Debate debate = debateService.createDebate("Test Debate", "Test Topic", "Test Description", Collections.emptyList());

        assertNotNull(debate);
        assertEquals("Test Debate", debate.getName());
        assertEquals(DebateStatus.DRAFT, debate.getStatus());
    }
}
