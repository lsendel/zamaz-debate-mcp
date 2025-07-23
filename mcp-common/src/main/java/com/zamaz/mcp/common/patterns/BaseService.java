package com.zamaz.mcp.common.patterns;

import com.zamaz.mcp.common.audit.Auditable;
import com.zamaz.mcp.common.audit.AuditEvent;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLogger;
import com.zamaz.mcp.common.infrastructure.logging.StructuredLoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base service class providing common functionality for all business services.
 * Implements standard patterns for CRUD operations, validation, and logging.
 */
public abstract class BaseService<T, ID> {

    protected final StructuredLogger logger;
    protected final JpaRepository<T, ID> repository;

    protected BaseService(JpaRepository<T, ID> repository, StructuredLoggerFactory loggerFactory) {
        this.repository = repository;
        this.logger = loggerFactory.getLogger(this.getClass());
    }

    /**
     * Create a new entity.
     */
    @Transactional
    @Auditable(action = AuditEvent.AuditAction.CREATE, resourceType = "Entity", description = "Create new entity", riskLevel = AuditEvent.RiskLevel.MEDIUM)
    public T create(T entity) {
        long startTime = System.currentTimeMillis();

        logger.info("Creating entity")
                .field("service", this.getClass().getSimpleName())
                .field("entityType", entity.getClass().getSimpleName())
                .field("startTime", startTime)
                .log();

        try {
            validateForCreate(entity);
            T savedEntity = repository.save(entity);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Entity created successfully")
                    .field("service", this.getClass().getSimpleName())
                    .field("entityType", entity.getClass().getSimpleName())
                    .field("entityId", getEntityId(savedEntity))
                    .duration(duration)
                    .log();

            return savedEntity;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Failed to create entity", e)
                    .field("service", this.getClass().getSimpleName())
                    .field("entityType", entity.getClass().getSimpleName())
                    .duration(duration)
                    .log();
            throw e;
        }
    }

    /**
     * Find entity by ID.
     */
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        logger.debug("Finding entity by ID")
                .field("service", this.getClass().getSimpleName())
                .field("entityId", id)
                .log();

        return repository.findById(id);
    }

    /**
     * Find entity by ID or throw exception.
     */
    @Transactional(readOnly = true)
    public T getById(ID id) {
        return findById(id).orElseThrow(() -> createNotFoundException(id));
    }

    /**
     * Find all entities.
     */
    @Transactional(readOnly = true)
    public List<T> findAll() {
        logger.debug("Finding all entities")
                .field("service", this.getClass().getSimpleName())
                .log();

        return repository.findAll();
    }

    /**
     * Find entities with pagination.
     */
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        logger.debug("Finding entities with pagination")
                .field("service", this.getClass().getSimpleName())
                .field("page", pageable.getPageNumber())
                .field("size", pageable.getPageSize())
                .field("sort", pageable.getSort().toString())
                .log();

        return repository.findAll(pageable);
    }

    /**
     * Update an existing entity.
     */
    @Transactional
    @Auditable(action = AuditEvent.AuditAction.UPDATE, resourceType = "Entity", description = "Update existing entity", riskLevel = AuditEvent.RiskLevel.MEDIUM)
    public T update(ID id, T entity) {
        long startTime = System.currentTimeMillis();

        logger.info("Updating entity")
                .field("service", this.getClass().getSimpleName())
                .field("entityId", id)
                .field("entityType", entity.getClass().getSimpleName())
                .field("startTime", startTime)
                .log();

        try {
            T existingEntity = getById(id);
            validateForUpdate(existingEntity, entity);

            T updatedEntity = performUpdate(existingEntity, entity);
            T savedEntity = repository.save(updatedEntity);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Entity updated successfully")
                    .field("service", this.getClass().getSimpleName())
                    .field("entityId", id)
                    .field("entityType", entity.getClass().getSimpleName())
                    .duration(duration)
                    .log();

            return savedEntity;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Failed to update entity", e)
                    .field("service", this.getClass().getSimpleName())
                    .field("entityId", id)
                    .field("entityType", entity.getClass().getSimpleName())
                    .duration(duration)
                    .log();
            throw e;
        }
    }

    /**
     * Update an entity using a function.
     */
    @Transactional
    public T update(ID id, Function<T, T> updateFunction) {
        logger.info("Updating entity with function")
                .field("service", this.getClass().getSimpleName())
                .field("entityId", id)
                .log();

        T existingEntity = getById(id);
        T updatedEntity = updateFunction.apply(existingEntity);
        T savedEntity = repository.save(updatedEntity);

        logger.info("Entity updated successfully")
                .field("service", this.getClass().getSimpleName())
                .field("entityId", id)
                .log();

        return savedEntity;
    }

    /**
     * Delete an entity by ID.
     */
    @Transactional
    @Auditable(action = AuditEvent.AuditAction.DELETE, resourceType = "Entity", description = "Delete entity by ID", riskLevel = AuditEvent.RiskLevel.HIGH)
    public void deleteById(ID id) {
        long startTime = System.currentTimeMillis();

        logger.info("Deleting entity")
                .field("service", this.getClass().getSimpleName())
                .field("entityId", id)
                .field("startTime", startTime)
                .log();

        try {
            T entity = getById(id);
            validateForDelete(entity);
            repository.deleteById(id);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Entity deleted successfully")
                    .field("service", this.getClass().getSimpleName())
                    .field("entityId", id)
                    .duration(duration)
                    .log();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Failed to delete entity", e)
                    .field("service", this.getClass().getSimpleName())
                    .field("entityId", id)
                    .duration(duration)
                    .log();
            throw e;
        }
    }

    /**
     * Check if entity exists by ID.
     */
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    /**
     * Count all entities.
     */
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }

    /**
     * Validate entity before creation.
     * Override in subclasses to add custom validation.
     */
    protected void validateForCreate(T entity) {
        // Default implementation - no validation
        logger.debug("Validating entity for creation")
                .field("service", this.getClass().getSimpleName())
                .field("entityType", entity.getClass().getSimpleName())
                .log();
    }

    /**
     * Validate entity before update.
     * Override in subclasses to add custom validation.
     */
    protected void validateForUpdate(T existingEntity, T updatedEntity) {
        // Default implementation - no validation
        logger.debug("Validating entity for update")
                .field("service", this.getClass().getSimpleName())
                .field("entityType", existingEntity.getClass().getSimpleName())
                .log();
    }

    /**
     * Validate entity before deletion.
     * Override in subclasses to add custom validation.
     */
    protected void validateForDelete(T entity) {
        // Default implementation - no validation
        logger.debug("Validating entity for deletion")
                .field("service", this.getClass().getSimpleName())
                .field("entityType", entity.getClass().getSimpleName())
                .log();
    }

    /**
     * Perform the actual update operation.
     * Override in subclasses to customize update behavior.
     */
    protected T performUpdate(T existingEntity, T updatedEntity) {
        // Default implementation - return the updated entity
        return updatedEntity;
    }

    /**
     * Get the ID of an entity.
     * Override in subclasses to provide entity-specific ID extraction.
     */
    protected abstract Object getEntityId(T entity);

    /**
     * Create a not found exception for the entity.
     * Override in subclasses to provide entity-specific exceptions.
     */
    protected abstract RuntimeException createNotFoundException(ID id);

    /**
     * Log a service operation with structured logging.
     */
    protected void logOperation(String operation, Object... params) {
        var logBuilder = logger.info("Service operation: " + operation)
                .field("service", this.getClass().getSimpleName())
                .field("operation", operation);

        // Add parameters as fields
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                logBuilder.field(params[i].toString(), params[i + 1]);
            }
        }

        logBuilder.log();
    }

    /**
     * Log a service error with structured logging.
     */
    protected void logError(String operation, Exception exception, Object... params) {
        var logBuilder = logger.error("Service error: " + operation, exception)
                .field("service", this.getClass().getSimpleName())
                .field("operation", operation)
                .field("exceptionType", exception.getClass().getSimpleName())
                .field("exceptionMessage", exception.getMessage());

        // Add parameters as fields
        for (int i = 0; i < params.length; i += 2) {
            if (i + 1 < params.length) {
                logBuilder.field(params[i].toString(), params[i + 1]);
            }
        }

        logBuilder.log();
    }

    /**
     * Execute an operation with error logging.
     */
    protected <R> R executeWithLogging(String operation, java.util.function.Supplier<R> supplier, Object... params) {
        try {
            logOperation(operation, params);
            return supplier.get();
        } catch (Exception e) {
            logError(operation, e, params);
            throw e;
        }
    }

    /**
     * Execute an operation with error logging (void return).
     */
    protected void executeWithLogging(String operation, Runnable runnable, Object... params) {
        try {
            logOperation(operation, params);
            runnable.run();
        } catch (Exception e) {
            logError(operation, e, params);
            throw e;
        }
    }

    /**
     * Execute a transactional operation with automatic logging and error handling.
     */
    @Transactional
    protected <R> R executeTransactional(String operation, java.util.function.Supplier<R> supplier) {
        long startTime = System.currentTimeMillis();

        logger.info("Starting transactional operation")
                .field("service", this.getClass().getSimpleName())
                .operation(operation)
                .field("startTime", startTime)
                .log();

        try {
            R result = supplier.get();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Transactional operation completed successfully")
                    .field("service", this.getClass().getSimpleName())
                    .operation(operation)
                    .duration(duration)
                    .log();

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Transactional operation failed", e)
                    .field("service", this.getClass().getSimpleName())
                    .operation(operation)
                    .duration(duration)
                    .log();
            throw e;
        }
    }

    /**
     * Execute a read-only operation with automatic logging.
     */
    @Transactional(readOnly = true)
    protected <R> R executeReadOnly(String operation, java.util.function.Supplier<R> supplier) {
        long startTime = System.currentTimeMillis();

        logger.debug("Starting read-only operation")
                .field("service", this.getClass().getSimpleName())
                .operation(operation)
                .field("startTime", startTime)
                .log();

        try {
            R result = supplier.get();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Read-only operation completed")
                    .field("service", this.getClass().getSimpleName())
                    .operation(operation)
                    .duration(duration)
                    .log();

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Read-only operation failed", e)
                    .field("service", this.getClass().getSimpleName())
                    .operation(operation)
                    .duration(duration)
                    .log();
            throw e;
        }
    }

    /**
     * Validate entity state and throw McpBusinessException if invalid.
     */
    protected void validateEntityState(T entity, String expectedState, String actualState) {
        if (!expectedState.equals(actualState)) {
            throw com.zamaz.mcp.common.exception.McpBusinessException.invalidState(
                    entity.getClass().getSimpleName(),
                    actualState,
                    expectedState);
        }
    }

    /**
     * Create a standardized not found exception.
     */
    protected com.zamaz.mcp.common.exception.McpBusinessException createStandardNotFoundException(String resourceType,
            ID id) {
        return com.zamaz.mcp.common.exception.McpBusinessException.notFound(resourceType, id.toString());
    }

    /**
     * Batch create entities with transaction management.
     */
    @Transactional
    @Auditable(action = AuditEvent.AuditAction.CREATE, resourceType = "Entity", description = "Batch create entities", riskLevel = AuditEvent.RiskLevel.MEDIUM)
    public List<T> createBatch(List<T> entities) {
        return executeTransactional("batch_create", () -> {
            entities.forEach(this::validateForCreate);
            return repository.saveAll(entities);
        });
    }

    /**
     * Batch update entities with transaction management.
     */
    @Transactional
    @Auditable(action = AuditEvent.AuditAction.UPDATE, resourceType = "Entity", description = "Batch update entities", riskLevel = AuditEvent.RiskLevel.MEDIUM)
    public List<T> updateBatch(List<T> entities) {
        return executeTransactional("batch_update", () -> {
            entities.forEach(entity -> {
                // Basic validation - subclasses can override for more specific validation
                if (getEntityId(entity) == null) {
                    throw new IllegalArgumentException("Entity ID cannot be null for update");
                }
            });
            return repository.saveAll(entities);
        });
    }
}