package com.zamaz.mcp.organization.config;

import com.zamaz.mcp.common.application.service.TransactionManager;
import com.zamaz.mcp.common.application.service.ValidationService;
import com.zamaz.mcp.common.domain.event.DomainEventPublisher;
import com.zamaz.mcp.organization.application.port.inbound.*;
import com.zamaz.mcp.organization.application.port.outbound.NotificationService;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.application.usecase.*;
import com.zamaz.mcp.organization.domain.service.OrganizationDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application layer components.
 * Wires use case implementations with their dependencies.
 */
@Configuration
public class ApplicationConfig {
    
    @Bean
    public CreateOrganizationUseCase createOrganizationUseCase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationDomainService domainService,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        
        return new CreateOrganizationUseCaseImpl(
            organizationRepository,
            userRepository,
            domainService,
            notificationService,
            eventPublisher,
            transactionManager,
            validationService
        );
    }
    
    @Bean
    public GetOrganizationUseCase getOrganizationUseCase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            TransactionManager transactionManager) {
        
        return new GetOrganizationUseCaseImpl(
            organizationRepository,
            userRepository,
            transactionManager
        );
    }
    
    @Bean
    public UpdateOrganizationUseCase updateOrganizationUseCase(
            OrganizationRepository organizationRepository,
            OrganizationDomainService domainService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        
        return new UpdateOrganizationUseCaseImpl(
            organizationRepository,
            domainService,
            eventPublisher,
            transactionManager,
            validationService
        );
    }
    
    @Bean
    public AddUserToOrganizationUseCase addUserToOrganizationUseCase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            OrganizationDomainService domainService,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        
        return new AddUserToOrganizationUseCaseImpl(
            organizationRepository,
            userRepository,
            domainService,
            notificationService,
            eventPublisher,
            transactionManager,
            validationService
        );
    }
    
    @Bean
    public RemoveUserFromOrganizationUseCase removeUserFromOrganizationUseCase(
            OrganizationRepository organizationRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            DomainEventPublisher eventPublisher,
            TransactionManager transactionManager,
            ValidationService validationService) {
        
        return new RemoveUserFromOrganizationUseCaseImpl(
            organizationRepository,
            userRepository,
            notificationService,
            eventPublisher,
            transactionManager,
            validationService
        );
    }
}