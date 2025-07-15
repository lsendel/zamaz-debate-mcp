# MCP Template Java Service - CLAUDE.md

This file provides guidance to Claude Code when working with the mcp-template service.

## Service Overview

The `mcp-template` service is a Spring Boot-based template management system, providing REST APIs for creating, managing, and rendering templates. This is the most complete Java service in the project with full CRUD operations and multi-tenant support.

## Purpose

- **Template Management**: Full CRUD operations for templates
- **Multi-tenant Support**: Organization-based isolation
- **Version Control**: Template versioning with parent tracking
- **REST API**: Complete HTTP interface for template operations
- **Enterprise Ready**: Spring Boot with JPA and PostgreSQL

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 2.7.5
- **Persistence**: Spring Data JPA with PostgreSQL
- **Build Tool**: Maven
- **JSON Processing**: Jackson with custom converters
- **Template Engine**: Thymeleaf (optional)

## Architecture Components

### Domain Model

#### Template Entity
```java
@Entity
@Table(name = "templates")
public class Template {
    @Id
    private String id;
    private String organizationId;
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private TemplateCategory category;
    
    @Enumerated(EnumType.STRING)
    private TemplateType type;
    
    @Enumerated(EnumType.STRING)
    private TemplateStatus status;
    
    @Lob
    private String content;
    
    @Convert(converter = JpaConverterJson.class)
    private List<TemplateVariable> variables;
    
    private Integer version;
    private String parentId;
    
    @Convert(converter = JpaConverterJson.class)
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Template Variable
```java
public class TemplateVariable {
    private String name;
    private String type;
    private boolean required;
    private Object defaultValue;
    private String description;
    private Map<String, Object> validation;
}
```

### Enumerations

- **TemplateCategory**: DEBATE, PROMPT, RESPONSE, EVALUATION, MODERATION, SYSTEM, CUSTOM
- **TemplateType**: JINJA2, MARKDOWN, PLAIN_TEXT, JSON
- **TemplateStatus**: DRAFT, ACTIVE, ARCHIVED, DEPRECATED

### Service Layer

The `TemplateManager` provides:
- Template CRUD operations
- Search and filtering
- Version management
- Rendering capabilities
- Usage tracking

### REST API

The `TemplateController` exposes:
```java
@RestController
@RequestMapping("/api/templates")
public class TemplateController {
    
    @PostMapping
    public Template createTemplate(@RequestBody Template template);
    
    @GetMapping("/{id}")
    public Template getTemplate(@PathVariable String id);
    
    @PutMapping("/{id}")
    public Template updateTemplate(@PathVariable String id, @RequestBody Template template);
    
    @DeleteMapping("/{id}")
    public void deleteTemplate(@PathVariable String id);
    
    @GetMapping
    public List<Template> listTemplates(
        @RequestParam(required = false) String organizationId,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String status
    );
    
    @PostMapping("/{id}/render")
    public String renderTemplate(
        @PathVariable String id,
        @RequestBody Map<String, Object> variables
    );
}
```

## Configuration

### Application Properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/templates
spring.datasource.username=template_user
spring.datasource.password=template_pass

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# JSON
spring.jackson.serialization.write-dates-as-timestamps=false
```

### Maven Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
</dependencies>
```

## Key Features

### Multi-Tenant Support
- All templates scoped by `organizationId`
- Automatic filtering in queries
- Cross-organization template sharing (future)

### Version Management
- Automatic version incrementing
- Parent-child relationships
- Version history tracking
- Rollback capabilities

### JSON Column Support
- Custom JPA converter for PostgreSQL JSONB
- Stores complex objects (variables, metadata)
- Enables flexible template configuration

### Template Rendering
- Variable substitution
- Type validation
- Default value handling
- Error reporting

## Development Guidelines

### Running the Service
```bash
# Build
mvn clean install

# Run with Maven
mvn spring-boot:run

# Run JAR
java -jar target/mcp-template-0.0.1-SNAPSHOT.jar

# Run with Docker
docker build -t mcp-template .
docker run -p 8080:8080 mcp-template
```

### Database Setup
```sql
-- Create database
CREATE DATABASE templates;

-- Create user
CREATE USER template_user WITH PASSWORD 'template_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE templates TO template_user;
```

### Testing
```java
@SpringBootTest
@AutoConfigureMockMvc
public class TemplateControllerTest {
    
    @Test
    public void testCreateTemplate() {
        // Test template creation
    }
    
    @Test
    public void testRenderTemplate() {
        // Test template rendering
    }
}
```

## Integration Points

### With Python Services
- Shared database schema
- Compatible JSON format
- REST API communication
- Common template format

### With UI
- RESTful endpoints
- JSON request/response
- Pagination support
- Error handling

## Best Practices

1. **Use DTOs** for API contracts
2. **Implement validation** with Bean Validation
3. **Add audit fields** (createdBy, updatedBy)
4. **Use transactions** for multi-step operations
5. **Implement caching** for frequently used templates
6. **Add API versioning** for backward compatibility
7. **Use Lombok** to reduce boilerplate

## Error Handling

```java
@ExceptionHandler(TemplateNotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(TemplateNotFoundException e) {
    return ResponseEntity.notFound().build();
}

@ExceptionHandler(InvalidTemplateException.class)
public ResponseEntity<ErrorResponse> handleInvalid(InvalidTemplateException e) {
    return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
}
```

## Security Considerations

- Add authentication/authorization
- Validate template content
- Sanitize rendered output
- Implement rate limiting
- Audit template changes
- Encrypt sensitive variables

## Performance Optimization

1. **Database indexes** on frequently queried fields
2. **Lazy loading** for template content
3. **Caching** with Spring Cache
4. **Pagination** for list operations
5. **Async processing** for rendering
6. **Connection pooling** with HikariCP

## Monitoring

- Spring Actuator endpoints
- Metrics with Micrometer
- Health checks
- Request logging
- Performance tracking

## Future Enhancements

1. **Template Inheritance**: Base templates with overrides
2. **Template Marketplace**: Share across organizations
3. **A/B Testing**: Multiple active versions
4. **Webhooks**: Notify on template changes
5. **Import/Export**: Backup and migration
6. **Template Preview**: Live rendering UI
7. **Access Control**: Fine-grained permissions
8. **Audit Trail**: Complete change history
9. **MCP Integration**: Expose via MCP protocol
10. **GraphQL API**: Alternative query interface