package com.zamaz.mcp.modulith.debate;

import com.zamaz.mcp.modulith.llm.LlmService;
import com.zamaz.mcp.modulith.organization.OrganizationService;
import com.zamaz.mcp.modulith.shared.events.OrganizationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing debates.
 * Orchestrates debates between LLM participants.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DebateService {
    
    private final DebateRepository debateRepository;
    private final DebateParticipantRepository participantRepository;
    private final DebateTurnRepository turnRepository;
    private final LlmService llmService;
    private final OrganizationService organizationService;
    
    /**
     * Creates a new debate.
     */
    public Debate createDebate(UUID organizationId, String topic, String description) {
        // Verify organization exists
        organizationService.findById(organizationId)
            .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));
        
        log.info("Creating debate for organization {} on topic: {}", organizationId, topic);
        
        Debate debate = Debate.builder()
            .organizationId(organizationId)
            .topic(topic)
            .description(description)
            .status(Debate.DebateStatus.DRAFT)
            .format(Debate.DebateFormat.TURN_BASED)
            .maxRounds(5)
            .maxTurnLength(500)
            .build();
        
        return debateRepository.save(debate);
    }
    
    /**
     * Adds a participant to a debate.
     */
    public DebateParticipant addParticipant(UUID debateId, String name, String llmProvider, 
                                           DebateParticipant.ParticipantRole role) {
        Debate debate = debateRepository.findById(debateId)
            .orElseThrow(() -> new IllegalArgumentException("Debate not found: " + debateId));
        
        DebateParticipant participant = DebateParticipant.builder()
            .debate(debate)
            .name(name)
            .llmProvider(llmProvider)
            .role(role)
            .turnOrder(debate.getParticipants().size())
            .build();
        
        return participantRepository.save(participant);
    }
    
    /**
     * Starts a debate.
     */
    public Debate startDebate(UUID debateId) {
        Debate debate = debateRepository.findById(debateId)
            .orElseThrow(() -> new IllegalArgumentException("Debate not found: " + debateId));
        
        if (debate.getParticipants().isEmpty()) {
            throw new IllegalStateException("Cannot start debate without participants");
        }
        
        log.info("Starting debate: {}", debateId);
        
        debate.setStatus(Debate.DebateStatus.IN_PROGRESS);
        debate.setStartedAt(LocalDateTime.now());
        debate.setCurrentRound(1);
        
        return debateRepository.save(debate);
    }
    
    /**
     * Processes the next turn in a debate.
     */
    public DebateTurn processNextTurn(UUID debateId) {
        Debate debate = debateRepository.findById(debateId)
            .orElseThrow(() -> new IllegalArgumentException("Debate not found: " + debateId));
        
        if (debate.getStatus() != Debate.DebateStatus.IN_PROGRESS) {
            throw new IllegalStateException("Debate is not in progress");
        }
        
        // Get the next participant
        DebateParticipant nextParticipant = getNextParticipant(debate);
        
        // Get conversation history
        List<DebateTurn> history = turnRepository.findByDebateOrderByCreatedAtAsc(debate);
        
        // Generate response using LLM
        String response = llmService.generateResponse(
            nextParticipant.getLlmProvider(),
            nextParticipant.getSystemPrompt(),
            buildConversationContext(debate, history)
        );
        
        // Create and save the turn
        DebateTurn turn = DebateTurn.builder()
            .debate(debate)
            .participant(nextParticipant)
            .content(response)
            .roundNumber(debate.getCurrentRound())
            .turnNumber(history.size() + 1)
            .build();
        
        turn = turnRepository.save(turn);
        
        // Check if round is complete
        checkAndUpdateRound(debate);
        
        return turn;
    }
    
    /**
     * Finds all debates for an organization.
     */
    @Transactional(readOnly = true)
    public List<Debate> findByOrganization(UUID organizationId) {
        return debateRepository.findByOrganizationId(organizationId);
    }
    
    /**
     * Listens for organization creation events.
     */
    @EventListener
    public void onOrganizationCreated(OrganizationCreatedEvent event) {
        log.info("Organization created: {}, creating welcome debate", event.organizationName());
        
        // Create a welcome debate for new organizations
        createDebate(
            event.organizationId(),
            "Welcome to MCP Debates!",
            "A sample debate to demonstrate the platform capabilities"
        );
    }
    
    private DebateParticipant getNextParticipant(Debate debate) {
        // Simple round-robin for now
        List<DebateParticipant> debaters = debate.getParticipants().stream()
            .filter(p -> p.getRole() == DebateParticipant.ParticipantRole.DEBATER)
            .toList();
        
        if (debaters.isEmpty()) {
            throw new IllegalStateException("No debaters found");
        }
        
        int currentTurns = turnRepository.countByDebate(debate);
        return debaters.get(currentTurns % debaters.size());
    }
    
    private String buildConversationContext(Debate debate, List<DebateTurn> history) {
        StringBuilder context = new StringBuilder();
        context.append("Debate Topic: ").append(debate.getTopic()).append("\n\n");
        
        if (!history.isEmpty()) {
            context.append("Previous turns:\n");
            for (DebateTurn turn : history) {
                context.append(turn.getParticipant().getName())
                       .append(": ")
                       .append(turn.getContent())
                       .append("\n\n");
            }
        }
        
        return context.toString();
    }
    
    private void checkAndUpdateRound(Debate debate) {
        List<DebateParticipant> debaters = debate.getParticipants().stream()
            .filter(p -> p.getRole() == DebateParticipant.ParticipantRole.DEBATER)
            .toList();
        
        int turnsInRound = turnRepository.countByDebateAndRoundNumber(debate, debate.getCurrentRound());
        
        if (turnsInRound >= debaters.size()) {
            // Round complete
            if (debate.getCurrentRound() >= debate.getMaxRounds()) {
                // Debate complete
                debate.setStatus(Debate.DebateStatus.COMPLETED);
                debate.setCompletedAt(LocalDateTime.now());
            } else {
                // Move to next round
                debate.setCurrentRound(debate.getCurrentRound() + 1);
            }
            debateRepository.save(debate);
        }
    }
}