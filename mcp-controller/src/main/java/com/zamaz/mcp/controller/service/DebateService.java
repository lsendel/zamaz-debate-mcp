package com.zamaz.mcp.controller.service;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.DebateResultDto;
import com.zamaz.mcp.controller.dto.DebateWithParticipantCount;
import com.zamaz.mcp.controller.dto.ParticipantDto;
import com.zamaz.mcp.controller.dto.ParticipantResultDto;
import com.zamaz.mcp.controller.dto.ResponseDto;
import com.zamaz.mcp.controller.dto.RoundDto;
import com.zamaz.mcp.controller.entity.Debate;
import com.zamaz.mcp.controller.entity.Participant;
import com.zamaz.mcp.controller.entity.Round;
import com.zamaz.mcp.controller.entity.Response;
import com.zamaz.mcp.controller.entity.DebateStatus;
import com.zamaz.mcp.controller.exception.ResourceNotFoundException;
import com.zamaz.mcp.controller.repository.DebateRepository;
import com.zamaz.mcp.controller.repository.ParticipantRepository;
import com.zamaz.mcp.controller.repository.RoundRepository;
import com.zamaz.mcp.controller.repository.ResponseRepository;
import com.zamaz.mcp.controller.repository.specification.DebateSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DebateService {
    
    private final DebateRepository debateRepository;
    private final ParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final ResponseRepository responseRepository;
    private final OrchestrationService orchestrationService;

    public DebateDto createDebate(DebateDto.CreateDebateRequest request) {
        log.debug("Creating new debate: {}", request.getTitle());
        
        Debate debate = Debate.builder()
                .organizationId(request.getOrganizationId())
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(request.getTopic())
                .format(request.getFormat())
                .maxRounds(request.getMaxRounds())
                .status(DebateStatus.DRAFT)
                .settings(request.getSettings())
                .build();
        
        debate = debateRepository.save(debate);
        log.info("Created debate with ID: {}", debate.getId());
        
        return toDto(debate);
    }

    @Cacheable(value = "debates", key = "#id")
    public DebateDto getDebate(UUID id) {
        log.debug("Getting debate with ID: {} (cache miss)", id);
        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));
        return toDto(debate);
    }
    
    @Cacheable(value = "debate-lists", key = "#organizationId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<DebateDto> listDebates(UUID organizationId, DebateStatus status, Pageable pageable) {
        log.debug("Listing debates for organization: {}, status: {} (cache miss)", organizationId, status);
        
        Specification<Debate> spec = Specification.where(DebateSpecifications.hasOrganizationId(organizationId))
                .and(DebateSpecifications.hasStatus(status));
        
        Page<DebateWithParticipantCount> debates = debateRepository.findAll(spec, pageable)
                .map(this::toDtoFromProjection);
        
        return debates;
    }
    
    @CachePut(value = "debates", key = "#id")
    @CacheEvict(value = "debate-lists", allEntries = true)
    public DebateDto updateDebate(UUID id, DebateDto.UpdateDebateRequest request) {
        log.debug("Updating debate with ID: {}", id);
        
        Debate debate = debateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + id));
        
        if (request.getTitle() != null) {
            debate.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            debate.setDescription(request.getDescription());
        }
        if (request.getTopic() != null) {
            debate.setTopic(request.getTopic());
        }
        if (request.getMaxRounds() != null) {
            debate.setMaxRounds(request.getMaxRounds());
        }
        if (request.getSettings() != null) {
            debate.setSettings(request.getSettings());
        }
        if (request.getStatus() != null) {
            debate.setStatus(DebateStatus.valueOf(request.getStatus()));
        }
        
        debate = debateRepository.save(debate);
        log.info("Updated debate with ID: {}", debate.getId());
        
        return toDto(debate);
    }
    
    @CacheEvict(value = {"debates", "debate-lists", "debate-results"}, key = "#id")
    public void deleteDebate(UUID id) {
        log.debug("Deleting debate with ID: {}", id);
        
        if (!debateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Debate not found with ID: " + id);
        }
        
        debateRepository.deleteById(id);
        log.info("Deleted debate with ID: {}", id);
    }
    
    public ParticipantDto addParticipant(UUID debateId, ParticipantDto.CreateParticipantRequest request) {
        log.debug("Adding participant to debate: {}", debateId);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + debateId));
        
        if ("ai".equals(request.getType()) && (request.getProvider() == null || request.getModel() == null)) {
            throw new IllegalArgumentException("Provider and model are required for AI participants");
        }
        
        Participant participant = Participant.builder()
                .debate(debate)
                .name(request.getName())
                .type(request.getType())
                .provider(request.getProvider())
                .model(request.getModel())
                .position(request.getPosition())
                .settings(request.getSettings())
                .build();
        
        participant = participantRepository.save(participant);
        log.info("Added participant {} to debate {}", participant.getId(), debateId);
        
        return toParticipantDto(participant);
    }
    
    public void removeParticipant(UUID debateId, UUID participantId) {
        log.debug("Removing participant {} from debate {}", participantId, debateId);
        
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + participantId));
        
        if (!participant.getDebate().getId().equals(debateId)) {
            throw new IllegalArgumentException("Participant does not belong to this debate");
        }
        
        participantRepository.delete(participant);
        log.info("Removed participant {} from debate {}", participantId, debateId);
    }
    
    public ResponseDto submitResponse(UUID debateId, UUID roundId, ResponseDto.CreateResponseRequest request) {
        log.debug("Submitting response for debate: {}, round: {}", debateId, roundId);
        
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with ID: " + roundId));
        
        if (!round.getDebate().getId().equals(debateId)) {
            throw new IllegalArgumentException("Round does not belong to this debate");
        }
        
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with ID: " + request.getParticipantId()));
        
        Response response = Response.builder()
                .round(round)
                .participant(participant)
                .content(request.getContent())
                .tokenCount(request.getContent().split("\s+").length) // Simple token count
                .build();
        
        response = responseRepository.save(response);
        log.info("Submitted response {} for round {}", response.getId(), roundId);
        
        // Check if round is complete
        orchestrationService.checkRoundCompletion(debateId, roundId);
        
        return toResponseDto(response);
    }
    
    public List<RoundDto> listRounds(UUID debateId) {
        log.debug("Listing rounds for debate: {}", debateId);
        
        return roundRepository.findByDebateIdOrderByRoundNumber(debateId).stream()
                .map(this::toRoundDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "debate-results", key = "#debateId", condition = "#result?.status?.name() == 'COMPLETED'")
    public DebateResultDto getResults(UUID debateId) {
        log.debug("Getting results for debate: {} (cache miss)", debateId);
        
        Debate debate = debateRepository.findById(debateId)
                .orElseThrow(() -> new ResourceNotFoundException("Debate not found with ID: " + debateId));
        
        return toDebateResultDto(debate);
    }
    
    private DebateDto toDto(Debate debate) {
        return DebateDto.builder()
                .id(debate.getId())
                .organizationId(debate.getOrganizationId())
                .title(debate.getTitle())
                .description(debate.getDescription())
                .topic(debate.getTopic())
                .format(debate.getFormat())
                .maxRounds(debate.getMaxRounds())
                .currentRound(debate.getCurrentRound())
                .status(debate.getStatus().name())
                .settings(debate.getSettings())
                .createdAt(debate.getCreatedAt())
                .updatedAt(debate.getUpdatedAt())
                .startedAt(debate.getStartedAt())
                .completedAt(debate.getCompletedAt())
                .participantCount(debate.getParticipants().size())
                .build();
    }
    
    private ParticipantDto toParticipantDto(Participant participant) {
        return ParticipantDto.builder()
                .id(participant.getId())
                .debateId(participant.getDebate().getId())
                .name(participant.getName())
                .type(participant.getType())
                .provider(participant.getProvider())
                .model(participant.getModel())
                .position(participant.getPosition())
                .settings(participant.getSettings())
                .createdAt(participant.getCreatedAt())
                .build();
    }
    
    private ResponseDto toResponseDto(Response response) {
        return ResponseDto.builder()
                .id(response.getId())
                .roundId(response.getRound().getId())
                .participantId(response.getParticipant().getId())
                .participantName(response.getParticipant().getName())
                .content(response.getContent())
                .tokenCount(response.getTokenCount())
                .createdAt(response.getCreatedAt())
                .build();
    }

    private RoundDto toRoundDto(Round round) {
        return RoundDto.builder()
                .id(round.getId())
                .roundNumber(round.getRoundNumber())
                .status(round.getStatus())
                .startedAt(round.getStartedAt())
                .completedAt(round.getCompletedAt())
                .responses(round.getResponses().stream().map(this::toResponseDto).collect(Collectors.toList()))
                .build();
    }

    private DebateResultDto toDebateResultDto(Debate debate) {
        return DebateResultDto.builder()
                .debateId(debate.getId())
                .status(debate.getStatus().name())
                .totalRounds(debate.getCurrentRound())
                .participants(debate.getParticipants().stream().map(this::toParticipantResultDto).collect(Collectors.toList()))
                .build();
    }

    private ParticipantResultDto toParticipantResultDto(Participant participant) {
        return ParticipantResultDto.builder()
                .name(participant.getName())
                .position(participant.getPosition())
                .responseCount(participant.getResponses().size())
                .build();
    }

    private DebateDto toDtoFromProjection(DebateWithParticipantCount projection) {
        return DebateDto.builder()
                .id(projection.getId())
                .organizationId(projection.getOrganizationId())
                .title(projection.getTitle())
                .description(projection.getDescription())
                .topic(projection.getTopic())
                .format(projection.getFormat())
                .maxRounds(projection.getMaxRounds())
                .currentRound(projection.getCurrentRound())
                .status(projection.getStatus())
                .settings(projection.getSettings())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .startedAt(projection.getStartedAt())
                .completedAt(projection.getCompletedAt())
                .participantCount((int) projection.getParticipantCount())
                .build();
    }
}