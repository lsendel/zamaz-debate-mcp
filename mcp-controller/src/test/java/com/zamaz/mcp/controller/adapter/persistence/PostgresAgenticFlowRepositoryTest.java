package com.zamaz.mcp.controller.adapter.persistence;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;
import com.zamaz.mcp.controller.adapter.persistence.entity.AgenticFlowEntity;
import com.zamaz.mcp.controller.adapter.persistence.mapper.AgenticFlowMapper;
import com.zamaz.mcp.controller.adapter.persistence.repository.SpringDataAgenticFlowRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PostgresAgenticFlowRepository.
 */
@ExtendWith(MockitoExtension.class)
class PostgresAgenticFlowRepositoryTest {

    @Mock
    private SpringDataAgenticFlowRepository springDataRepository;

    @Mock
    private AgenticFlowMapper mapper;

    @InjectMocks
    private PostgresAgenticFlowRepository repository;

    private AgenticFlow testFlow;
    private AgenticFlowEntity testEntity;
    private OrganizationId organizationId;
    private AgenticFlowId flowId;

    @BeforeEach
    void setUp() {
        organizationId = new OrganizationId(UUID.randomUUID().toString());
        flowId = new AgenticFlowId();

        Map<String, Object> config = Map.of(
                "temperature", 0.7,
                "iterations", 3);

        testFlow = new AgenticFlow(
                flowId,
                AgenticFlowType.INTERNAL_MONOLOGUE,
                new AgenticFlowConfiguration(config),
                AgenticFlowStatus.ACTIVE,
                organizationId,
                Instant.now(),
                Instant.now());

        testEntity = AgenticFlowEntity.builder()
                .id(UUID.fromString(flowId.getValue()))
                .flowType(AgenticFlowType.INTERNAL_MONOLOGUE.name())
                .name("Internal Monologue")
                .description("Test flow")
                .configuration(config)
                .organizationId(UUID.fromString(organizationId.getValue()))
                .status(AgenticFlowStatus.ACTIVE.name())
                .createdAt(testFlow.getCreatedAt())
                .updatedAt(testFlow.getUpdatedAt())
                .version(1L)
                .build();
    }

    @Test
    void save_NewFlow_ShouldCreateNewEntity() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(springDataRepository.findById(flowUuid)).thenReturn(Optional.empty());
        when(mapper.toEntity(testFlow)).thenReturn(testEntity);
        when(springDataRepository.save(testEntity)).thenReturn(testEntity);
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        AgenticFlow result = repository.save(testFlow);

        // Then
        assertThat(result).isEqualTo(testFlow);
        verify(springDataRepository).findById(flowUuid);
        verify(mapper).toEntity(testFlow);
        verify(springDataRepository).save(testEntity);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void save_ExistingFlow_ShouldUpdateEntity() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        AgenticFlowEntity existingEntity = AgenticFlowEntity.builder()
                .id(flowUuid)
                .flowType(AgenticFlowType.INTERNAL_MONOLOGUE.name())
                .name("Old Name")
                .status(AgenticFlowStatus.INACTIVE.name())
                .build();

        when(springDataRepository.findById(flowUuid)).thenReturn(Optional.of(existingEntity));
        when(springDataRepository.save(existingEntity)).thenReturn(existingEntity);
        when(mapper.toDomain(existingEntity)).thenReturn(testFlow);

        // When
        AgenticFlow result = repository.save(testFlow);

        // Then
        assertThat(result).isEqualTo(testFlow);
        verify(springDataRepository).findById(flowUuid);
        verify(mapper).updateEntity(existingEntity, testFlow);
        verify(springDataRepository).save(existingEntity);
        verify(mapper).toDomain(existingEntity);
    }

    @Test
    void findById_ExistingFlow_ShouldReturnFlow() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(springDataRepository.findById(flowUuid)).thenReturn(Optional.of(testEntity));
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        Optional<AgenticFlow> result = repository.findById(flowId);

        // Then
        assertThat(result).isPresent().contains(testFlow);
        verify(springDataRepository).findById(flowUuid);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void findById_NonExistingFlow_ShouldReturnEmpty() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(springDataRepository.findById(flowUuid)).thenReturn(Optional.empty());

        // When
        Optional<AgenticFlow> result = repository.findById(flowId);

        // Then
        assertThat(result).isEmpty();
        verify(springDataRepository).findById(flowUuid);
    }

    @Test
    void findByOrganization_ShouldReturnFlows() {
        // Given
        UUID orgUuid = UUID.fromString(organizationId.getValue());
        List<AgenticFlowEntity> entities = List.of(testEntity);
        when(springDataRepository.findByOrganizationId(orgUuid)).thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        List<AgenticFlow> result = repository.findByOrganization(organizationId);

        // Then
        assertThat(result).hasSize(1).contains(testFlow);
        verify(springDataRepository).findByOrganizationId(orgUuid);
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void findByType_ShouldReturnFlows() {
        // Given
        List<AgenticFlowEntity> entities = List.of(testEntity);
        when(springDataRepository.findByFlowType(AgenticFlowType.INTERNAL_MONOLOGUE.name()))
                .thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        List<AgenticFlow> result = repository.findByType(AgenticFlowType.INTERNAL_MONOLOGUE);

        // Then
        assertThat(result).hasSize(1).contains(testFlow);
        verify(springDataRepository).findByFlowType(AgenticFlowType.INTERNAL_MONOLOGUE.name());
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void findByOrganizationAndType_ShouldReturnFlows() {
        // Given
        UUID orgUuid = UUID.fromString(organizationId.getValue());
        List<AgenticFlowEntity> entities = List.of(testEntity);
        when(springDataRepository.findByOrganizationIdAndFlowType(
                orgUuid, AgenticFlowType.INTERNAL_MONOLOGUE.name()))
                .thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        List<AgenticFlow> result = repository.findByOrganizationAndType(
                organizationId, AgenticFlowType.INTERNAL_MONOLOGUE);

        // Then
        assertThat(result).hasSize(1).contains(testFlow);
        verify(springDataRepository).findByOrganizationIdAndFlowType(
                orgUuid, AgenticFlowType.INTERNAL_MONOLOGUE.name());
        verify(mapper).toDomain(testEntity);
    }

    @Test
    void delete_ExistingFlow_ShouldReturnTrue() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(springDataRepository.existsById(flowUuid)).thenReturn(true);

        // When
        boolean result = repository.delete(flowId);

        // Then
        assertThat(result).isTrue();
        verify(springDataRepository).existsById(flowUuid);
        verify(springDataRepository).deleteById(flowUuid);
    }

    @Test
    void delete_NonExistingFlow_ShouldReturnFalse() {
        // Given
        UUID flowUuid = UUID.fromString(flowId.getValue());
        when(springDataRepository.existsById(flowUuid)).thenReturn(false);

        // When
        boolean result = repository.delete(flowId);

        // Then
        assertThat(result).isFalse();
        verify(springDataRepository).existsById(flowUuid);
    }

    @Test
    void findByOrganizationAndStatus_ShouldReturnFlows() {
        // Given
        UUID orgUuid = UUID.fromString(organizationId.getValue());
        List<AgenticFlowEntity> entities = List.of(testEntity);
        when(springDataRepository.findByOrganizationIdAndStatus(
                orgUuid, AgenticFlowStatus.ACTIVE.name()))
                .thenReturn(entities);
        when(mapper.toDomain(testEntity)).thenReturn(testFlow);

        // When
        List<AgenticFlow> result = repository.findByOrganizationAndStatus(
                organizationId, AgenticFlowStatus.ACTIVE);

        // Then
        assertThat(result).hasSize(1).contains(testFlow);
        verify(springDataRepository).findByOrganizationIdAndStatus(
                orgUuid, AgenticFlowStatus.ACTIVE.name());
        verify(mapper).toDomain(testEntity);
    }
}