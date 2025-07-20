package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowExecution;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowExecutionEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowMapper;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowExecutionRepository;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PostgresAgenticFlowAnalyticsRepository.
 */
@ExtendWith(MockitoExtension.class)
class PostgresAgenticFlowAnalyticsRepositoryTest {

    @Mock
    private SpringDataAgenticFlowExecutionRepository executionRepository;

    @Mock
    private SpringDataAgenticFlowRepository flowRepository;

    @Mock
    private AgenticFlowMapper mapper;

    @InjectMocks
    private PostgresAgenticFlowAnalyticsRepository repository;

    private AgenticFlowExecution testExecution;
    private AgenticFlowExecutionEntity testExecutionEntity;
    private AgenticFlowEntity testFlowEntity;
    private AgenticFlowId flowId;
    private UUID executionId;
    private UUID debateId;
    private UUID participantId;

    @BeforeEach
    void setUp() {
        flowId = new AgenticFlowId();
        executionId = UUID.randomUUID();
        debateId = UUID.randomUUID();
        participantId = UUID.randomUUID();

        Map<String, Object> result = Map.of(
                "finalAnswer", "Test answer",
                "reasoning", "Test reasoning",
                "confidence", 0.85);

        testExecution = new AgenticFlowExecution(
                executionId,
                flowId,
                debateId,
                participantId,
                "Test prompt",
                result,
                1500L,
                true,
                null,
                Instant.now());

        testFlowEntity = AgenticFlowEntity.builder()
                .id(UUID.fromString(flowId.getValue()))
                .flowType("INTERNAL_MONOLOGUE")
                .name("Test Flow")
                .build();

        testExecutionEntity = AgenticFlowExecutionEntity.builder()
                .id(executionId)
                .flow(testFlowEntity)
                .debateId(debateId)
                .participantId(participantId)
                .prompt("Test prompt")
                .result(result)
                .processingTimeMs(1500L)
                .responseChanged(true)
                .errorMessage(null)
                .createdAt(testExecution.getCreatedAt())
                .build();
    }

    @Test
    void save_ValidExecution_ShouldSaveSuccessfully() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(flowRepository.findById(flowUuid)).thenReturn(Optional.of(testFlowEntity));
        when(mapper.toExecutionEntity(testExecution, testFlowEntity)).thenReturn(testExecutionEntity);
        when(executionRepository.save(testExecutionEntity)).thenReturn(testExecutionEntity);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        AgenticFlowExecution result = repository.save(testExecution);

        // Then
        assertThat(result).isEqualTo(testExecution);
        verify(flowRepository).findById(flowUuid);
        verify(mapper).toExecutionEntity(testExecution, testFlowEntity);
        verify(executionRepository).save(testExecutionEntity);
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void save_FlowNotFound_ShouldThrowException() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(flowRepository.findById(flowUuid)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> repository.save(testExecution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Flow not found");

        verify(flowRepository).findById(flowUuid);
    }

    @Test
    void findById_ExistingExecution_ShouldReturnExecution() {
        // Given
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(testExecutionEntity));
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        Optional<AgenticFlowExecution> result = repository.findById(executionId);

        // Then
        assertThat(result).isPresent().contains(testExecution);
        verify(executionRepository).findById(executionId);
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void findById_NonExistingExecution_ShouldReturnEmpty() {
        // Given
        when(executionRepository.findById(executionId)).thenReturn(Optional.empty());

        // When
        Optional<AgenticFlowExecution> result = repository.findById(executionId);

        // Then
        assertThat(result).isEmpty();
        verify(executionRepository).findById(executionId);
    }

    @Test
    void findByFlowId_WithLimit_ShouldReturnLimitedResults() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        int limit = 5;
        Page<AgenticFlowExecutionEntity> page = new PageImpl<>(List.of(testExecutionEntity));
        when(executionRepository.findByFlowId(eq(flowUuid), any(Pageable.class))).thenReturn(page);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        List<AgenticFlowExecution> result = repository.findByFlowId(flowId, limit);

        // Then
        assertThat(result).hasSize(1).contains(testExecution);
        verify(executionRepository).findByFlowId(eq(flowUuid), eq(PageRequest.of(0, limit)));
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void findByFlowId_WithoutLimit_ShouldReturnAllResults() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        Page<AgenticFlowExecutionEntity> page = new PageImpl<>(List.of(testExecutionEntity));
        when(executionRepository.findByFlowId(eq(flowUuid), any(Pageable.class))).thenReturn(page);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        List<AgenticFlowExecution> result = repository.findByFlowId(flowId, 0);

        // Then
        assertThat(result).hasSize(1).contains(testExecution);
        verify(executionRepository).findByFlowId(eq(flowUuid), eq(Pageable.unpaged()));
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void findByDebateId_ShouldReturnExecutions() {
        // Given
        List<AgenticFlowExecutionEntity> entities = List.of(testExecutionEntity);
        when(executionRepository.findByDebateId(debateId)).thenReturn(entities);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        List<AgenticFlowExecution> result = repository.findByDebateId(debateId);

        // Then
        assertThat(result).hasSize(1).contains(testExecution);
        verify(executionRepository).findByDebateId(debateId);
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void countExecutions_ShouldReturnCount() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        long expectedCount = 42L;
        when(executionRepository.countByFlowId(flowUuid)).thenReturn(expectedCount);

        // When
        long result = repository.countExecutions(flowId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(executionRepository).countByFlowId(flowUuid);
    }

    @Test
    void countResponseChanges_ShouldReturnCount() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        long expectedCount = 15L;
        when(executionRepository.countByFlowIdAndResponseChangedTrue(flowUuid)).thenReturn(expectedCount);

        // When
        long result = repository.countResponseChanges(flowId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(executionRepository).countByFlowIdAndResponseChangedTrue(flowUuid);
    }

    @Test
    void calculateAverageProcessingTime_ShouldReturnAverage() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        Double expectedAverage = 1250.5;
        when(executionRepository.calculateAverageProcessingTime(flowUuid)).thenReturn(expectedAverage);

        // When
        Double result = repository.calculateAverageProcessingTime(flowId);

        // Then
        assertThat(result).isEqualTo(expectedAverage);
        verify(executionRepository).calculateAverageProcessingTime(flowUuid);
    }

    @Test
    void calculateResponseChangeRate_ShouldReturnRate() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        Double expectedRate = 0.75;
        when(executionRepository.calculateResponseChangeRate(flowUuid)).thenReturn(expectedRate);

        // When
        Double result = repository.calculateResponseChangeRate(flowId);

        // Then
        assertThat(result).isEqualTo(expectedRate);
        verify(executionRepository).calculateResponseChangeRate(flowUuid);
    }

    @Test
    void findExecutionsWithErrors_ShouldReturnErrorExecutions() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        List<AgenticFlowExecutionEntity> entities = List.of(testExecutionEntity);
        when(executionRepository.findExecutionsWithErrors(flowUuid)).thenReturn(entities);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        List<AgenticFlowExecution> result = repository.findExecutionsWithErrors(flowId);

        // Then
        assertThat(result).hasSize(1).contains(testExecution);
        verify(executionRepository).findExecutionsWithErrors(flowUuid);
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void findSlowestExecutions_ShouldReturnSlowestExecutions() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        int limit = 10;
        List<AgenticFlowExecutionEntity> entities = List.of(testExecutionEntity);
        when(executionRepository.findSlowestExecutions(eq(flowUuid), any(Pageable.class)))
                .thenReturn(entities);
        when(mapper.toExecutionDomain(testExecutionEntity)).thenReturn(testExecution);

        // When
        List<AgenticFlowExecution> result = repository.findSlowestExecutions(flowId, limit);

        // Then
        assertThat(result).hasSize(1).contains(testExecution);
        verify(executionRepository).findSlowestExecutions(eq(flowUuid), eq(PageRequest.of(0, limit)));
        verify(mapper).toExecutionDomain(testExecutionEntity);
    }

    @Test
    void deleteExecutionsBefore_ShouldDeleteAndReturnCount() {
        // Given
        Instant cutoffTime = Instant.now().minusSeconds(3600);
        long countBefore = 100L;
        long countAfter = 80L;
        when(executionRepository.count()).thenReturn(countBefore, countAfter);

        // When
        long result = repository.deleteExecutionsBefore(cutoffTime);

        // Then
        assertThat(result).isEqualTo(20L);
        verify(executionRepository).deleteByCreatedAtBefore(cutoffTime);
    }
}