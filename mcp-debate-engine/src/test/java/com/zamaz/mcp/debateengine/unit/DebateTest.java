package com.zamaz.mcp.debateengine.unit;

import com.zamaz.mcp.debateengine.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Debate domain model.
 */
public class DebateTest {
    
    @Test
    void shouldCreateDebateSuccessfully() {
        // Given
        DebateId id = DebateId.generate();
        OrganizationId orgId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        UUID userId = UUID.randomUUID();
        DebateTopic topic = DebateTopic.of("Should AI be regulated?");
        String description = "A debate on AI regulation";
        DebateConfiguration config = DebateConfiguration.defaults();
        
        // When
        Debate debate = Debate.create(id, orgId, userId, topic, description, config);
        
        // Then
        assertThat(debate.getId()).isEqualTo(id);
        assertThat(debate.getOrganizationId()).isEqualTo(orgId);
        assertThat(debate.getCreatedByUserId()).isEqualTo(userId);
        assertThat(debate.getTopic()).isEqualTo(topic);
        assertThat(debate.getDescription()).isEqualTo(description);
        assertThat(debate.getStatus()).isEqualTo(DebateStatus.DRAFT);
        assertThat(debate.getConfiguration()).isEqualTo(config);
        assertThat(debate.getCurrentRoundNumber()).isEqualTo(0);
        assertThat(debate.getParticipants()).isEmpty();
        assertThat(debate.getRounds()).isEmpty();
        assertThat(debate.getCreatedAt()).isNotNull();
        assertThat(debate.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void shouldAddParticipantSuccessfully() {
        // Given
        Debate debate = createTestDebate();
        Participant participant = Participant.createHuman(
            ParticipantId.generate(),
            debate.getId(),
            UUID.randomUUID(),
            Position.PRO
        );
        
        // When
        debate.addParticipant(participant);
        
        // Then
        assertThat(debate.getParticipants()).hasSize(1);
        assertThat(debate.getParticipants().get(0)).isEqualTo(participant);
    }
    
    @Test
    void shouldFailToAddDuplicateParticipant() {
        // Given
        Debate debate = createTestDebate();
        ParticipantId participantId = ParticipantId.generate();
        Participant participant1 = Participant.createHuman(
            participantId,
            debate.getId(),
            UUID.randomUUID(),
            Position.PRO
        );
        Participant participant2 = Participant.createHuman(
            participantId, // Same ID
            debate.getId(),
            UUID.randomUUID(),
            Position.CON
        );
        
        debate.addParticipant(participant1);
        
        // When & Then
        assertThatThrownBy(() -> debate.addParticipant(participant2))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Participant already exists");
    }
    
    @Test
    void shouldFailToAddParticipantWithSamePosition() {
        // Given
        Debate debate = createTestDebate();
        Participant participant1 = Participant.createHuman(
            ParticipantId.generate(),
            debate.getId(),
            UUID.randomUUID(),
            Position.PRO
        );
        Participant participant2 = Participant.createHuman(
            ParticipantId.generate(),
            debate.getId(),
            UUID.randomUUID(),
            Position.PRO // Same position
        );
        
        debate.addParticipant(participant1);
        
        // When & Then
        assertThatThrownBy(() -> debate.addParticipant(participant2))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Position already taken");
    }
    
    @Test
    void shouldStartDebateSuccessfully() {
        // Given
        Debate debate = createTestDebate();
        setupDebateForStart(debate);
        
        // When
        debate.start();
        
        // Then
        assertThat(debate.getStatus()).isEqualTo(DebateStatus.ACTIVE);
        assertThat(debate.getStartedAt()).isNotNull();
    }
    
    @Test
    void shouldFailToStartDebateWithoutContext() {
        // Given
        Debate debate = createTestDebate();
        addMinimumParticipants(debate);
        // No context set
        
        // When & Then
        assertThatThrownBy(() -> debate.start())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Context must be set");
    }
    
    @Test
    void shouldFailToStartDebateWithoutParticipants() {
        // Given
        Debate debate = createTestDebate();
        debate.setContext(ContextId.generate());
        // No participants added
        
        // When & Then
        assertThatThrownBy(() -> debate.start())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("requires at least 2 participants");
    }
    
    @Test
    void shouldStartNewRoundSuccessfully() {
        // Given
        Debate debate = createTestDebate();
        setupDebateForStart(debate);
        debate.start();
        
        // When
        Round round = debate.startNewRound("Opening arguments");
        
        // Then
        assertThat(debate.getCurrentRoundNumber()).isEqualTo(1);
        assertThat(debate.getRounds()).hasSize(1);
        assertThat(round.getRoundNumber()).isEqualTo(1);
        assertThat(round.getStatus()).isEqualTo(RoundStatus.ACTIVE);
        assertThat(round.getPromptTemplate()).isEqualTo("Opening arguments");
    }
    
    @Test
    void shouldCompleteDebateSuccessfully() {
        // Given
        Debate debate = createTestDebate();
        setupDebateForStart(debate);
        debate.start();
        
        // When
        debate.complete();
        
        // Then
        assertThat(debate.getStatus()).isEqualTo(DebateStatus.COMPLETED);
        assertThat(debate.getCompletedAt()).isNotNull();
    }
    
    @Test
    void shouldCancelDebateSuccessfully() {
        // Given
        Debate debate = createTestDebate();
        String reason = "Technical difficulties";
        
        // When
        debate.cancel(reason);
        
        // Then
        assertThat(debate.getStatus()).isEqualTo(DebateStatus.CANCELLED);
        assertThat(debate.getCompletedAt()).isNotNull();
    }
    
    @Test
    void shouldValidateDebateTopic() {
        // When & Then
        assertThatThrownBy(() -> DebateTopic.of(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Topic cannot be empty");
        
        assertThatThrownBy(() -> DebateTopic.of("Short"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be at least 10 characters");
        
        String longTopic = "A".repeat(501);
        assertThatThrownBy(() -> DebateTopic.of(longTopic))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot exceed 500 characters");
    }
    
    @Test
    void shouldValidateDebateConfiguration() {
        // When & Then
        assertThatThrownBy(() -> DebateConfiguration.of(
            1, // Less than 2 participants
            5,
            Duration.ofMinutes(5),
            DebateConfiguration.Visibility.PRIVATE,
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must have at least 2 participants");
        
        assertThatThrownBy(() -> DebateConfiguration.of(
            2,
            0, // No rounds
            Duration.ofMinutes(5),
            DebateConfiguration.Visibility.PRIVATE,
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must have at least 1 round");
        
        assertThatThrownBy(() -> DebateConfiguration.of(
            2,
            5,
            Duration.ofMinutes(-1), // Negative timeout
            DebateConfiguration.Visibility.PRIVATE,
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("timeout must be positive");
    }
    
    @Test
    void shouldValidateParticipantCreation() {
        // Given
        ParticipantId id = ParticipantId.generate();
        DebateId debateId = DebateId.generate();
        
        // When & Then - Human participant validation
        assertThatThrownBy(() -> Participant.createHuman(id, debateId, null, Position.PRO))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("User ID required for human participant");
        
        // AI participant validation
        assertThatThrownBy(() -> Participant.createAI(id, debateId, null, Position.PRO))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("AI model required for AI participant");
    }
    
    @Test
    void shouldRecordParticipantResponse() {
        // Given
        Participant participant = Participant.createHuman(
            ParticipantId.generate(),
            DebateId.generate(),
            UUID.randomUUID(),
            Position.PRO
        );
        
        // When
        participant.recordResponse(5000L);
        
        // Then
        assertThat(participant.getTotalResponses()).isEqualTo(1);
        assertThat(participant.getAverageResponseTimeMs()).isEqualTo(5000L);
    }
    
    private Debate createTestDebate() {
        DebateId id = DebateId.generate();
        OrganizationId orgId = OrganizationId.from("550e8400-e29b-41d4-a716-446655440000");
        UUID userId = UUID.randomUUID();
        DebateTopic topic = DebateTopic.of("Should AI be regulated by governments?");
        String description = "A comprehensive debate on AI regulation";
        DebateConfiguration config = DebateConfiguration.defaults();
        
        return Debate.create(id, orgId, userId, topic, description, config);
    }
    
    private void setupDebateForStart(Debate debate) {
        addMinimumParticipants(debate);
        debate.setContext(ContextId.generate());
    }
    
    private void addMinimumParticipants(Debate debate) {
        Participant proParticipant = Participant.createHuman(
            ParticipantId.generate(),
            debate.getId(),
            UUID.randomUUID(),
            Position.PRO
        );
        
        Participant conParticipant = Participant.createAI(
            ParticipantId.generate(),
            debate.getId(),
            AIModel.openAI("gpt-4"),
            Position.CON
        );
        
        debate.addParticipant(proParticipant);
        debate.addParticipant(conParticipant);
    }
}