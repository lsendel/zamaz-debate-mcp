package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class DebateServiceTest {

    @Autowired
    private DebateService debateService;

    @MockBean
    private DebateRepository debateRepository;

    @Test
    public void testCreateDebate() {
        Debate debateToSave = new Debate("1", "Test Debate", "Test Topic", "Test Description", Collections.emptyList());
        when(debateRepository.save(any(Debate.class))).thenReturn(debateToSave);

        Debate debate = debateService.createDebate("Test Debate", "Test Topic", "Test Description", Collections.emptyList());

        assertNotNull(debate);
        assertEquals("Test Debate", debate.getName());
        assertEquals(DebateStatus.DRAFT, debate.getStatus());
    }
}
