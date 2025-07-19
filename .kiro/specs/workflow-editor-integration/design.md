# Design Document

## Overview

This document outlines the technical design for integrating a React-Flow-based workflow editor into the existing Zamaz Debate MCP Services web application. The system will support telemetry orchestration for IoT sensor workflows and software planning for CI/CD task pipelines, handling 10,000 nodes with real-time telemetry at 10Hz frequency. The design follows hexagonal architecture principles with specialized databases and includes four sample applications demonstrating different workflow capabilities.

## Architecture

### Hexagonal Architecture Implementation

The system follows hexagonal architecture (ports and adapters) with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   React-Flow    │  │   GraphQL API   │  │  REST API   │ │
│  │   Frontend      │  │   Endpoint      │  │  Endpoints  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Workflow      │  │   Telemetry     │  │   Sample    │ │
│  │   Application   │  │   Application   │  │ Application │ │
│  │   Services      │  │   Services      │  │  Services   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Workflow      │  │   Telemetry     │  │   Sample    │ │
│  │   Domain        │  │   Domain        │  │   Domain    │ │
│  │   Services      │  │   Services      │  │  Services   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                 Infrastructure Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────┐ │
│  │   Neo4j     │  │  InfluxDB   │  │   PostGIS   │  │ MCP │ │
│  │  Adapter    │  │   Adapter   │  │   Adapter   │  │ LLM │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 5-Subproject Architecture

The system is organized into 5 modular subprojects, each with clear scope and integration strategy:

#### Subproject 1: Workflow Editor Frontend
**Directory**: `client/workflow-editor/`
**Purpose**: React module for drag-and-drop workflow editor and map visualization

**Technology Stack**:
- **React-Flow**: Workflow canvas with custom nodes
- **React-QueryBuilder**: Visual condition builder
- **Framer-Motion**: Node animations
- **Zustand**: State management
- **React-Query**: Data fetching and caching
- **Socket.IO-Client**: Real-time telemetry
- **MapLibre GL JS**: OpenStreetMap rendering
- **react-virtuoso**: Virtualization for 10K+ nodes

**Components**:
```
src/
├── components/
│   ├── WorkflowEditor.jsx      # Main React-Flow canvas
│   ├── MapViewer.jsx           # OpenStreetMap visualization
│   ├── nodes/
│   │   ├── StartNode.jsx       # Workflow start node
│   │   ├── DecisionNode.jsx    # Decision/condition node
│   │   ├── TaskNode.jsx        # Action/task node
│   │   └── EndNode.jsx         # Workflow end node
│   └── ConditionBuilder.jsx    # Drag-and-drop condition builder
├── store/                      # Zustand state management
├── hooks/                      # React Query hooks
├── workers/                    # Web Workers for condition evaluation
└── utils/                      # Utility functions
```

#### Subproject 2: GraphQL Backend
**Directory**: `server/src/main/java/com/example/workflow/graphql/`
**Purpose**: Spring Boot module with Spring GraphQL following hexagonal architecture

**Features**:
- GraphQL schema: `nodes(viewport)`, `edges`, `triggerAction`, `mapTiles(region)`
- Hexagonal architecture with ports and adapters
- Integration with Neo4j, InfluxDB, PostGIS, OpenMapTiles

**Structure**:
```
graphql/
├── ports/
│   ├── WorkflowService.java        # Inbound port
│   ├── TelemetryService.java       # Inbound port
│   └── MapService.java             # Inbound port
├── adapters/
│   ├── inbound/
│   │   ├── GraphQLController.java  # GraphQL resolvers
│   │   └── WebSocketController.java # Real-time subscriptions
│   └── outbound/
│       ├── Neo4jAdapter.java       # Workflow repository
│       ├── InfluxDbAdapter.java    # Telemetry repository
│       ├── PostGisAdapter.java     # Spatial repository
│       └── OpenMapTilesAdapter.java # Map tile repository
└── domain/
    ├── Workflow.java               # Domain entity
    ├── TelemetryData.java          # Domain entity
    └── services/                   # Domain services
```

#### Subproject 3: Telemetry Data Emulator
**Directory**: `server/src/main/java/com/example/workflow/telemetry/`
**Purpose**: Generate and stream emulated telemetry data at 10Hz

**Features**:
- Synthetic time-series data generation
- Spatial data for North America/Europe
- WebSocket streaming at 100ms intervals
- LTTB downsampling for performance

#### Subproject 4: Data Storage Layer
**Directory**: `server/src/main/java/com/example/workflow/data/`
**Purpose**: Multi-database storage management

**Databases**:
- **Neo4j**: Workflow graphs with Cypher queries
- **InfluxDB**: Time-series telemetry at 10Hz
- **PostGIS**: Spatial telemetry data
- **OpenMapTiles**: Vector tiles for North America/Europe

#### Subproject 5: Kubernetes Deployment
**Directory**: `k8s/`
**Purpose**: Container orchestration and deployment

**Features**:
- Kubernetes manifests for all services
- Istio service mesh integration
- Horizontal Pod Autoscaling
- Production-ready configurations

### Module Structure (Hexagonal Layers)

Following hexagonal architecture principles, each subproject is organized into layers:

#### Domain Modules
- **workflow-domain**: Core workflow business logic and entities
- **telemetry-domain**: Telemetry processing and analysis logic
- **geospatial-domain**: Geospatial sample application domain
- **debate-tree-domain**: Debate tree visualization domain
- **decision-tree-domain**: Decision tree workflow domain
- **document-analysis-domain**: AI document analysis domain

#### Application Modules
- **workflow-application**: Workflow orchestration use cases
- **telemetry-application**: Telemetry processing use cases
- **sample-applications**: Sample application orchestration

#### Infrastructure Modules
- **workflow-infrastructure**: Database adapters and external integrations
- **spatial-infrastructure**: PostGIS and mapping infrastructure
- **telemetry-infrastructure**: InfluxDB and real-time data infrastructure
- **ai-infrastructure**: Integration with existing mcp-llm service

#### Presentation Modules
- **workflow-web**: React-Flow frontend application
- **workflow-api**: GraphQL API gateway

## Components and Interfaces

### Core Domain Entities

#### Workflow Domain
```java
// Domain Entity
public class Workflow {
    private WorkflowId id;
    private String name;
    private List<WorkflowNode> nodes;
    private List<WorkflowConnection> connections;
    private WorkflowStatus status;
    private OrganizationId organizationId;
}

// Domain Service
public interface WorkflowDomainService {
    Workflow createWorkflow(CreateWorkflowCommand command);
    void executeWorkflow(WorkflowId id, TelemetryData data);
    WorkflowExecutionResult processNode(WorkflowNode node, TelemetryData data);
}

// Repository Port
public interface WorkflowRepository {
    void save(Workflow workflow);
    Optional<Workflow> findById(WorkflowId id);
    List<Workflow> findByOrganization(OrganizationId orgId);
}
```

#### Telemetry Domain
```java
// Domain Entity
public class TelemetryData {
    private TelemetryId id;
    private DeviceId deviceId;
    private Instant timestamp;
    private Map<String, Object> metrics;
    private GeoLocation location;
}

// Domain Service
public interface TelemetryDomainService {
    void processTelemetryStream(Stream<TelemetryData> dataStream);
    TelemetryAnalysis analyzeTelemetry(TelemetryQuery query);
    void triggerWorkflowConditions(TelemetryData data);
}

// Repository Port
public interface TelemetryRepository {
    void saveTimeSeries(TelemetryData data);
    void saveSpatialData(TelemetryData data);
    Stream<TelemetryData> queryTimeSeries(TimeRange range);
    List<TelemetryData> querySpatial(GeoQuery query);
}
```

### Application Services

#### Workflow Application Service
```java
@Service
public class WorkflowApplicationService {
    private final WorkflowDomainService workflowDomainService;
    private final WorkflowRepository workflowRepository;
    private final TelemetryApplicationService telemetryService;
    
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        // Orchestrate workflow creation
    }
    
    public void executeWorkflowWithTelemetry(WorkflowId id, TelemetryData data) {
        // Coordinate workflow execution with telemetry processing
    }
}
```

#### Telemetry Application Service
```java
@Service
public class TelemetryApplicationService {
    private final TelemetryDomainService telemetryDomainService;
    private final TelemetryRepository telemetryRepository;
    private final WorkflowNotificationPort workflowNotificationPort;
    
    @EventListener
    public void handleTelemetryData(TelemetryDataEvent event) {
        // Process incoming telemetry at 10Hz
        telemetryDomainService.processTelemetryStream(event.getDataStream());
        // Trigger workflow conditions
        workflowNotificationPort.notifyWorkflows(event.getData());
    }
}
```

### Infrastructure Adapters

#### Neo4j Workflow Adapter
```java
@Repository
public class Neo4jWorkflowRepository implements WorkflowRepository {
    private final Neo4jTemplate neo4jTemplate;
    
    @Override
    public void save(Workflow workflow) {
        WorkflowEntity entity = WorkflowMapper.toEntity(workflow);
        neo4jTemplate.save(entity);
    }
    
    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return neo4jTemplate.findById(id.getValue(), WorkflowEntity.class)
            .map(WorkflowMapper::toDomain);
    }
}
```

#### InfluxDB Telemetry Adapter
```java
@Repository
public class InfluxDbTelemetryRepository implements TelemetryRepository {
    private final InfluxDBClient influxDBClient;
    
    @Override
    public void saveTimeSeries(TelemetryData data) {
        Point point = Point.measurement("telemetry")
            .addTag("device_id", data.getDeviceId().getValue())
            .addFields(data.getMetrics())
            .time(data.getTimestamp(), WritePrecision.MS);
        
        influxDBClient.getWriteApiBlocking().writePoint(point);
    }
}
```

#### PostGIS Spatial Adapter
```java
@Repository
public class PostGisSpatialRepository implements SpatialRepository {
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public void saveSpatialData(TelemetryData data) {
        String sql = "INSERT INTO spatial_telemetry (device_id, location, timestamp, data) " +
                    "VALUES (?, ST_GeomFromText(?, 4326), ?, ?::jsonb)";
        
        jdbcTemplate.update(sql, 
            data.getDeviceId().getValue(),
            data.getLocation().toWKT(),
            data.getTimestamp(),
            data.getMetricsAsJson());
    }
}
```

### GraphQL API Layer

#### Workflow GraphQL Controller
```java
@Controller
public class WorkflowGraphQLController {
    private final WorkflowApplicationService workflowService;
    
    @QueryMapping
    public List<WorkflowResponse> workflows(@Argument String organizationId) {
        return workflowService.getWorkflowsByOrganization(organizationId);
    }
    
    @MutationMapping
    public WorkflowResponse createWorkflow(@Argument CreateWorkflowInput input) {
        return workflowService.createWorkflow(input.toRequest());
    }
    
    @SubscriptionMapping
    public Flux<WorkflowExecutionEvent> workflowExecution(@Argument String workflowId) {
        return workflowService.subscribeToWorkflowExecution(workflowId);
    }
}
```

## Data Models

### Neo4j Graph Schema

#### Workflow Nodes
```cypher
// Workflow Node
CREATE CONSTRAINT workflow_id_unique FOR (w:Workflow) REQUIRE w.id IS UNIQUE;
CREATE CONSTRAINT node_id_unique FOR (n:WorkflowNode) REQUIRE n.id IS UNIQUE;

// Workflow structure
(:Workflow {
    id: String,
    name: String,
    organizationId: String,
    status: String,
    createdAt: DateTime,
    updatedAt: DateTime
})

(:WorkflowNode {
    id: String,
    type: String, // 'input', 'condition', 'action', 'output'
    name: String,
    configuration: Map,
    position: Map // {x: Number, y: Number}
})

// Relationships
(:Workflow)-[:CONTAINS]->(:WorkflowNode)
(:WorkflowNode)-[:CONNECTS_TO {order: Integer}]->(:WorkflowNode)
```

### InfluxDB Schema

#### Time-Series Measurements
```sql
-- Telemetry measurement
telemetry,device_id=sensor001,location=stamford temperature=23.5,humidity=65.2,motion=false 1642680000000000000

-- Workflow execution measurement  
workflow_execution,workflow_id=wf001,node_id=node001 execution_time=150,status="completed" 1642680000000000000

-- Performance metrics
system_performance,service=workflow-engine cpu_usage=45.2,memory_usage=512,active_workflows=25 1642680000000000000
```

### PostGIS Spatial Schema

#### Spatial Tables
```sql
-- Spatial telemetry data
CREATE TABLE spatial_telemetry (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(255) NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    data JSONB NOT NULL
);

CREATE INDEX idx_spatial_telemetry_location ON spatial_telemetry USING GIST(location);
CREATE INDEX idx_spatial_telemetry_timestamp ON spatial_telemetry(timestamp);
CREATE INDEX idx_spatial_telemetry_device ON spatial_telemetry(device_id);

-- Stamford Connecticut addresses for geospatial sample
CREATE TABLE stamford_addresses (
    id SERIAL PRIMARY KEY,
    address TEXT NOT NULL,
    location GEOMETRY(POINT, 4326) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

## Sample Applications Design

### 1. Geospatial Sample - Stamford Connecticut

#### Architecture
```java
// Domain Service
@Service
public class GeospatialSampleDomainService {
    public List<StamfordAddress> generateRandomAddresses() {
        // Generate 10 random addresses within Stamford boundaries
    }
    
    public TelemetryStream simulateSensorData(List<StamfordAddress> addresses) {
        // Generate 10Hz telemetry data for each address
    }
}

// Application Service
@Service
public class GeospatialSampleApplicationService {
    public GeospatialSampleResponse initializeSample() {
        List<StamfordAddress> addresses = domainService.generateRandomAddresses();
        TelemetryStream stream = domainService.simulateSensorData(addresses);
        return new GeospatialSampleResponse(addresses, stream);
    }
}
```

#### Data Generation
- 10 random addresses within Stamford, CT boundaries (41.0534°N, 73.5387°W)
- Simulated sensors: temperature, humidity, motion, air quality
- 10Hz data generation with realistic variance patterns
- PostGIS spatial queries for proximity analysis

### 2. Debate Tree Map Sample

#### Architecture
```java
// Domain Service
@Service
public class DebateTreeDomainService {
    public DebateTreeStructure buildTreeFromDebates(List<Debate> debates) {
        // Transform flat debate list into hierarchical tree
    }
    
    public TreeMapVisualization generateTreeMap(DebateTreeStructure tree) {
        // Create tree map visualization data
    }
}

// Integration with existing MCP services
@Service
public class DebateIntegrationService {
    private final McpControllerClient mcpControllerClient;
    
    public List<Debate> fetchDebateData() {
        return mcpControllerClient.getAllDebates();
    }
}
```

#### Visualization Structure
- Hierarchical tree based on debate parent-child relationships
- Node size based on participant count
- Color coding based on debate status
- Interactive expansion/collapse functionality
- Real-time updates via GraphQL subscriptions

### 3. Decision Tree Sample

#### Architecture
```java
// Domain Service
@Service
public class DecisionTreeDomainService {
    public DecisionWorkflow createSampleDecisionTree() {
        return DecisionWorkflow.builder()
            .addConditionNode("temperature_check", "temperature > 25")
            .addConditionNode("humidity_check", "humidity > 70")
            .addActionNode("cooling_action", "activate_cooling")
            .addActionNode("dehumidify_action", "activate_dehumidifier")
            .build();
    }
    
    public DecisionResult evaluateConditions(DecisionNode node, TelemetryData data) {
        // Evaluate complex conditional logic
    }
}
```

#### Condition Builder Components
- Drag-and-drop condition elements
- Logical operators (AND, OR, NOT)
- Comparison operators (>, <, ==, !=, CONTAINS)
- Mathematical expressions and functions
- Real-time condition evaluation visualization

### 4. AI Document Analysis Sample

#### Architecture
```java
// Domain Service
@Service
public class DocumentAnalysisDomainService {
    public DocumentAnalysisSession createAnalysisSession(PDFDocument document) {
        // Initialize AI-powered document analysis
    }
    
    public List<InformationExtraction> analyzeDocumentPage(DocumentPage page) {
        // Extract structured information from document page
    }
}

// Integration with MCP LLM Service
@Service
public class AIDocumentService {
    private final McpLlmClient mcpLlmClient;
    
    public AIAnalysisResult analyzeText(String text, String context) {
        return mcpLlmClient.analyzeDocument(text, context);
    }
    
    public List<InformationSuggestion> getSuggestions(String selectedText) {
        return mcpLlmClient.getInformationSuggestions(selectedText);
    }
}
```

#### PDF Viewer Integration
- Multi-page PDF rendering
- Text selection and highlighting
- AI-powered information extraction
- Contextual suggestions and recommendations
- Export to structured formats (JSON, CSV, XML)

## Error Handling

### Domain-Level Error Handling
```java
// Domain Exceptions
public class WorkflowExecutionException extends DomainException {
    public WorkflowExecutionException(String message, WorkflowId workflowId) {
        super(message);
        this.workflowId = workflowId;
    }
}

public class TelemetryProcessingException extends DomainException {
    public TelemetryProcessingException(String message, TelemetryId telemetryId) {
        super(message);
        this.telemetryId = telemetryId;
    }
}
```

### Application-Level Error Handling
```java
@ControllerAdvice
public class GraphQLExceptionHandler {
    @ExceptionHandler(WorkflowExecutionException.class)
    public GraphQLError handleWorkflowException(WorkflowExecutionException ex) {
        return GraphQLError.newError()
            .message(ex.getMessage())
            .errorType(ErrorType.ExecutionAborted)
            .build();
    }
}
```

### Infrastructure-Level Error Handling
- Database connection retry logic with exponential backoff
- Circuit breaker pattern for external service calls
- Dead letter queues for failed telemetry processing
- Graceful degradation for non-critical features

## Testing Strategy

### Domain Testing
```java
@ExtendWith(MockitoExtension.class)
class WorkflowDomainServiceTest {
    @Mock
    private WorkflowRepository workflowRepository;
    
    @InjectMocks
    private WorkflowDomainService workflowDomainService;
    
    @Test
    void shouldCreateWorkflowWithValidInput() {
        // Test domain logic in isolation
    }
}
```

### Application Testing
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.neo4j.uri=bolt://localhost:7687"
})
class WorkflowApplicationServiceIntegrationTest {
    @Test
    void shouldProcessWorkflowEndToEnd() {
        // Test application service integration
    }
}
```

### Infrastructure Testing
```java
@DataNeo4jTest
class Neo4jWorkflowRepositoryTest {
    @Test
    void shouldSaveAndRetrieveWorkflow() {
        // Test Neo4j adapter
    }
}

@TestContainers
class InfluxDbTelemetryRepositoryTest {
    @Container
    static InfluxDBContainer influxDB = new InfluxDBContainer("influxdb:2.0");
    
    @Test
    void shouldSaveTimeSeriesData() {
        // Test InfluxDB adapter with test containers
    }
}
```

### Frontend Testing
```typescript
// React-Flow component testing
describe('WorkflowEditor', () => {
  test('should render workflow nodes', () => {
    render(<WorkflowEditor workflow={mockWorkflow} />);
    expect(screen.getByTestId('workflow-canvas')).toBeInTheDocument();
  });
  
  test('should handle node drag and drop', () => {
    // Test drag and drop functionality
  });
});

// GraphQL integration testing
describe('WorkflowQueries', () => {
  test('should fetch workflows', async () => {
    const { data } = await client.query({
      query: GET_WORKFLOWS,
      variables: { organizationId: 'org1' }
    });
    expect(data.workflows).toBeDefined();
  });
});
```

## Performance Considerations

### Real-Time Telemetry Processing
- Reactive streams for 10Hz data processing
- Backpressure handling for high-volume data
- Batch processing for database writes
- Connection pooling for database connections

### Large-Scale Workflow Support
- Lazy loading for workflow nodes (virtualization)
- Pagination for workflow lists
- Caching for frequently accessed workflows
- Optimistic UI updates for better user experience

### Database Optimization
- Proper indexing strategies for each database
- Connection pooling and prepared statements
- Read replicas for query-heavy operations
- Data retention policies for time-series data

## Security Integration

### Authentication and Authorization
```java
@Configuration
@EnableWebSecurity
public class WorkflowSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/graphql").authenticated()
                .requestMatchers("/workflow/**").hasRole("WORKFLOW_USER")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### Data Isolation
- Organization-level data segregation
- Row-level security for multi-tenant data
- Encrypted sensitive data in databases
- Audit logging for all workflow operations

## Deployment Architecture

### Kubernetes Deployment
```yaml
# Workflow service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: workflow-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: workflow-service
  template:
    metadata:
      labels:
        app: workflow-service
    spec:
      containers:
      - name: workflow-service
        image: workflow-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: NEO4J_URI
          valueFrom:
            configMapKeyRef:
              name: workflow-config
              key: neo4j.uri
        - name: INFLUXDB_URL
          valueFrom:
            configMapKeyRef:
              name: workflow-config
              key: influxdb.url
```

### Service Mesh Integration
- Istio service mesh for inter-service communication
- Circuit breakers and retry policies
- Distributed tracing with Jaeger
- Metrics collection with Prometheus

## OpenStreetMap Integration

### Map Tile Service
```java
@Service
public class OpenStreetMapService {
    public MapTileResponse getTiles(BoundingBox bounds, int zoomLevel) {
        // Fetch OpenMapTiles for specified region
        return openMapTilesClient.getTiles(bounds, zoomLevel);
    }
    
    public List<MapFeature> getFeatures(GeoQuery query) {
        // Query geographic features from PostGIS
        return spatialRepository.queryFeatures(query);
    }
}
```

### Frontend Map Integration
```typescript
// React map component with OpenStreetMap
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';

export const TelemetryMap: React.FC<TelemetryMapProps> = ({ telemetryData }) => {
  return (
    <MapContainer center={[41.0534, -73.5387]} zoom={13}>
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; OpenStreetMap contributors'
      />
      {telemetryData.map(data => (
        <Marker key={data.id} position={[data.lat, data.lng]}>
          <Popup>
            <TelemetryPopup data={data} />
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
};
```

This design provides a comprehensive foundation for the workflow editor integration, following hexagonal architecture principles while supporting high-performance real-time telemetry processing and providing four distinct sample applications to demonstrate the system's capabilities.