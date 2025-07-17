# Coding Standards

This document outlines the coding standards and best practices for the Zamaz Debate MCP project.

## General Guidelines

### Code Style

- Write clean, readable, and maintainable code
- Follow language-specific conventions and idioms
- Use consistent formatting and naming conventions
- Keep methods and classes focused on a single responsibility
- Limit method length (aim for < 30 lines)
- Limit class length (aim for < 300 lines)
- Use meaningful names for variables, methods, and classes

### Documentation

- Document public APIs with clear descriptions
- Include parameter and return value descriptions
- Document exceptions that may be thrown
- Add examples for complex functionality
- Keep documentation up-to-date with code changes
- Use inline comments for complex or non-obvious code

### Error Handling

- Handle exceptions at the appropriate level
- Use specific exception types
- Provide meaningful error messages
- Log exceptions with context information
- Don't swallow exceptions without handling them
- Use try-with-resources for automatic resource cleanup

### Testing

- Write tests for all new code
- Maintain high test coverage (aim for > 80%)
- Test both happy paths and error cases
- Use descriptive test names
- Keep tests independent and idempotent
- Mock external dependencies in unit tests

## Java Standards

### Code Style

We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with the following modifications:

- Use 4 spaces for indentation (not 2)
- Line length limit is 120 characters (not 100)
- Use `@Override` annotation for all overridden methods

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── zamaz/
│   │           └── mcp/
│   │               └── servicename/
│   │                   ├── McpServiceNameApplication.java
│   │                   ├── config/
│   │                   ├── controller/
│   │                   ├── exception/
│   │                   ├── model/
│   │                   ├── repository/
│   │                   ├── service/
│   │                   └── util/
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── application-prod.yml
└── test/
    ├── java/
    │   └── com/
    │       └── zamaz/
    │           └── mcp/
    │               └── servicename/
    │                   ├── McpServiceNameApplicationTests.java
    │                   ├── controller/
    │                   ├── service/
    │                   └── util/
    └── resources/
        └── application-test.yml
```

### Naming Conventions

- **Classes**: PascalCase, noun phrases (e.g., `DebateController`, `LlmService`)
- **Interfaces**: PascalCase, adjective or noun phrases (e.g., `Cacheable`, `MessageRepository`)
- **Methods**: camelCase, verb phrases (e.g., `createDebate`, `findById`)
- **Variables**: camelCase, noun phrases (e.g., `debateId`, `userMessage`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_TOKEN_LIMIT`, `DEFAULT_PAGE_SIZE`)
- **Packages**: lowercase, singular nouns (e.g., `controller`, `service`, `util`)

### Imports

- Organize imports in the following order:
  1. Static imports
  2. Java standard library imports
  3. Third-party library imports
  4. Project imports
- No wildcard imports (e.g., `import java.util.*`)
- No unused imports

### Lombok Usage

- Use Lombok to reduce boilerplate code
- Prefer `@Getter` and `@Setter` over manual getters and setters
- Use `@RequiredArgsConstructor` for constructor injection
- Use `@Builder` for complex object creation
- Use `@Value` for immutable classes
- Use `@Slf4j` for logging

### Example

```java
package com.zamaz.mcp.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.zamaz.mcp.controller.exception.DebateNotFoundException;
import com.zamaz.mcp.controller.model.Debate;
import com.zamaz.mcp.controller.repository.DebateRepository;

/**
 * Service for managing debates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebateService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    
    private final DebateRepository debateRepository;
    
    /**
     * Find a debate by ID.
     *
     * @param id the debate ID
     * @return the debate
     * @throws DebateNotFoundException if the debate is not found
     */
    public Debate findById(String id) {
        log.debug("Finding debate with ID: {}", id);
        return debateRepository.findById(id)
            .orElseThrow(() -> new DebateNotFoundException("Debate not found with ID: " + id));
    }
}
```

## Spring Boot Standards

### Configuration

- Use YAML for configuration files
- Use profiles for environment-specific configuration
- Externalize sensitive configuration (e.g., API keys, passwords)
- Use `@ConfigurationProperties` for typed configuration
- Document configuration properties

### Dependency Injection

- Use constructor injection (with `@RequiredArgsConstructor`)
- Avoid field injection (`@Autowired` on fields)
- Make dependencies explicit and final
- Use appropriate scopes for beans

### REST APIs

- Follow REST principles
- Use appropriate HTTP methods (GET, POST, PUT, DELETE)
- Use appropriate HTTP status codes
- Use consistent URL patterns
- Version APIs in the URL path (e.g., `/api/v1/debates`)
- Use DTOs for request and response objects
- Validate request parameters and bodies
- Handle errors consistently

### Example Controller

```java
package com.zamaz.mcp.controller.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.zamaz.mcp.controller.dto.CreateDebateRequest;
import com.zamaz.mcp.controller.dto.DebateResponse;
import com.zamaz.mcp.controller.service.DebateService;

import javax.validation.Valid;
import java.util.List;

/**
 * REST controller for managing debates.
 */
@RestController
@RequestMapping("/api/v1/debates")
@RequiredArgsConstructor
@Validated
public class DebateController {

    private final DebateService debateService;
    
    /**
     * Create a new debate.
     *
     * @param request the debate creation request
     * @return the created debate
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebateResponse createDebate(@Valid @RequestBody CreateDebateRequest request) {
        return debateService.createDebate(request);
    }
    
    /**
     * Get a debate by ID.
     *
     * @param id the debate ID
     * @return the debate
     */
    @GetMapping("/{id}")
    public DebateResponse getDebate(@PathVariable String id) {
        return debateService.getDebate(id);
    }
}
```

## Database Standards

### JPA/Hibernate

- Use JPA entities for database models
- Use appropriate fetch types (lazy by default)
- Use appropriate cascade types
- Use indexes for frequently queried fields
- Use optimistic locking for concurrent updates
- Use database migrations for schema changes

### Example Entity

```java
package com.zamaz.mcp.controller.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Debate entity.
 */
@Entity
@Table(name = "debates")
@Getter
@Setter
public class Debate {

    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String organizationId;
    
    @Column(length = 1000)
    private String description;
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebateStatus status = DebateStatus.CREATED;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
```

## Testing Standards

### Unit Tests

- Use JUnit 5 for unit tests
- Use Mockito for mocking dependencies
- Use AssertJ for assertions
- Test one concept per test method
- Use descriptive test names
- Use `@DisplayName` for test descriptions
- Use `@Nested` for organizing related tests

### Example Unit Test

```java
package com.zamaz.mcp.controller.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zamaz.mcp.controller.exception.DebateNotFoundException;
import com.zamaz.mcp.controller.model.Debate;
import com.zamaz.mcp.controller.repository.DebateRepository;

@ExtendWith(MockitoExtension.class)
class DebateServiceTest {

    @Mock
    private DebateRepository debateRepository;
    
    @InjectMocks
    private DebateService debateService;
    
    @Nested
    @DisplayName("findById")
    class FindById {
        
        @Test
        @DisplayName("should return debate when found")
        void shouldReturnDebateWhenFound() {
            // Given
            String debateId = "debate-123";
            Debate debate = new Debate();
            debate.setId(debateId);
            when(debateRepository.findById(debateId)).thenReturn(Optional.of(debate));
            
            // When
            Debate result = debateService.findById(debateId);
            
            // Then
            assertThat(result).isEqualTo(debate);
        }
        
        @Test
        @DisplayName("should throw exception when debate not found")
        void shouldThrowExceptionWhenDebateNotFound() {
            // Given
            String debateId = "debate-123";
            when(debateRepository.findById(debateId)).thenReturn(Optional.empty());
            
            // When/Then
            assertThatThrownBy(() -> debateService.findById(debateId))
                .isInstanceOf(DebateNotFoundException.class)
                .hasMessageContaining(debateId);
        }
    }
}
```

## Logging Standards

### Log Levels

- **ERROR**: Errors that prevent the application from functioning correctly
- **WARN**: Potential issues that don't prevent the application from functioning
- **INFO**: Important application events and state changes
- **DEBUG**: Detailed information for debugging
- **TRACE**: Very detailed information for troubleshooting

### Logging Guidelines

- Use appropriate log levels
- Include context information in log messages
- Use structured logging when appropriate
- Don't log sensitive information
- Use parameterized logging to avoid string concatenation
- Log at service boundaries and important state changes

### Example

```java
// Good
log.debug("Processing debate with ID: {}", debateId);

// Bad (string concatenation)
log.debug("Processing debate with ID: " + debateId);

// Good (with multiple parameters)
log.info("Created debate with ID: {} for organization: {}", debateId, organizationId);

// Good (with exception)
log.error("Failed to process debate with ID: {}", debateId, exception);
```

## Security Standards

### Authentication and Authorization

- Use Spring Security for authentication and authorization
- Use JWT for stateless authentication
- Implement proper CORS configuration
- Use HTTPS in all environments
- Implement proper role-based access control
- Validate user input to prevent injection attacks

### Sensitive Data

- Don't log sensitive data
- Don't include sensitive data in error messages
- Use environment variables for sensitive configuration
- Use secure storage for API keys and credentials
- Encrypt sensitive data at rest

### Example Security Configuration

```java
package com.zamaz.mcp.controller.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/actuator/health").permitAll()
            .antMatchers("/api/v1/**").authenticated()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

## Code Review Guidelines

### What to Look For

- Code correctness and functionality
- Adherence to coding standards
- Test coverage and quality
- Security considerations
- Performance implications
- Documentation completeness
- Error handling
- Edge cases

### Review Process

1. Understand the purpose and context of the changes
2. Review the code for correctness and adherence to standards
3. Run the tests to verify functionality
4. Provide constructive feedback
5. Approve or request changes

### Feedback Guidelines

- Be specific and constructive
- Focus on the code, not the person
- Explain why changes are needed
- Suggest alternatives when appropriate
- Acknowledge good practices
- Use a respectful and collaborative tone

## Version Control Standards

### Git Workflow

We follow the [GitHub Flow](https://guides.github.com/introduction/flow/) workflow:

1. Create a branch from `main`
2. Make changes and commit to the branch
3. Open a pull request
4. Review and discuss the changes
5. Merge the pull request
6. Delete the branch

### Branch Naming

- Use lowercase with hyphens
- Include the issue number when applicable
- Use descriptive names
- Follow the pattern: `type/issue-number-description`
- Types: `feature`, `bugfix`, `hotfix`, `refactor`, `docs`, `test`

Examples:
- `feature/123-add-debate-summarization`
- `bugfix/456-fix-token-counting`
- `refactor/improve-error-handling`

### Commit Messages

- Use the imperative mood (e.g., "Add feature" not "Added feature")
- Start with a capital letter
- Keep the first line under 50 characters
- Add a blank line after the first line
- Provide details in the body if necessary
- Reference issues in the body or footer

Example:
```
Add debate summarization feature

- Implement pro/con summary generation
- Add key points extraction
- Add API endpoint for summarization

Closes #123
```

## Continuous Integration

### CI Pipeline

- Run on every pull request and push to `main`
- Build the project
- Run unit tests
- Run integration tests
- Run static code analysis
- Check code coverage
- Check for vulnerabilities
- Generate reports

### Quality Gates

- All tests must pass
- Code coverage must be above 80%
- No critical or high severity issues
- No code smells
- No security vulnerabilities

## Conclusion

These coding standards are designed to ensure code quality, maintainability, and consistency across the Zamaz Debate MCP project. All team members are expected to follow these standards and help enforce them through code reviews and pair programming.

Remember that these standards are guidelines, not rigid rules. Use your judgment and discuss with the team when exceptions are warranted.
