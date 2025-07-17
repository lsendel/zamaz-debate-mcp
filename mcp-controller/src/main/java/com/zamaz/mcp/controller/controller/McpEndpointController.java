package com.zamaz.mcp.controller.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.controller.dto.*;
import com.zamaz.mcp.controller.service.DebateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpEndpointController {

    private final ObjectMapper objectMapper;
    private final DebateService debateService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getServerInfo() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("name", "mcp-debate-controller");
        response.put("version", "1.0.0");
        response.put("description", "Debate orchestration and management service");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }

    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // Create debate tool
        ObjectNode createDebate = tools.addObject();
        createDebate.put("name", "create_debate");
        createDebate.put("description", "Create a new debate");
        ObjectNode createParams = createDebate.putObject("parameters");
        createParams.put("type", "object");
        ObjectNode createProps = createParams.putObject("properties");
        createProps.putObject("topic").put("type", "string").put("description", "Debate topic");
        createProps.putObject("format").put("type", "string").put("description", "Debate format (OXFORD, LINCOLN_DOUGLAS, etc.)");
        createProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        ArrayNode participantsArray = createProps.putObject("participants").put("type", "array").putArray("items");
        participantsArray.addObject().put("type", "string");
        createProps.putObject("maxRounds").put("type", "integer").put("description", "Maximum rounds (default: 3)");
        createParams.putArray("required").add("topic").add("format").add("organizationId");
        
        // Get debate tool
        ObjectNode getDebate = tools.addObject();
        getDebate.put("name", "get_debate");
        getDebate.put("description", "Get debate details by ID");
        ObjectNode getParams = getDebate.putObject("parameters");
        getParams.put("type", "object");
        ObjectNode getProps = getParams.putObject("properties");
        getProps.putObject("debateId").put("type", "string").put("description", "Debate ID");
        getParams.putArray("required").add("debateId");
        
        // List debates tool
        ObjectNode listDebates = tools.addObject();
        listDebates.put("name", "list_debates");
        listDebates.put("description", "List debates for an organization");
        ObjectNode listParams = listDebates.putObject("parameters");
        listParams.put("type", "object");
        ObjectNode listProps = listParams.putObject("properties");
        listProps.putObject("organizationId").put("type", "string").put("description", "Organization ID");
        listParams.putArray("required").add("organizationId");
        
        // Submit turn tool
        ObjectNode submitTurn = tools.addObject();
        submitTurn.put("name", "submit_turn");
        submitTurn.put("description", "Submit a turn in a debate");
        ObjectNode turnParams = submitTurn.putObject("parameters");
        turnParams.put("type", "object");
        ObjectNode turnProps = turnParams.putObject("properties");
        turnProps.putObject("debateId").put("type", "string").put("description", "Debate ID");
        turnProps.putObject("participantId").put("type", "string").put("description", "Participant ID");
        turnProps.putObject("content").put("type", "string").put("description", "Turn content");
        turnParams.putArray("required").add("debateId").add("participantId").add("content");
        
        return Mono.just(response);
    }

    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        switch (toolName) {
            case "create_debate":
                return createDebate(objectMapper.convertValue(params, CreateDebateToolRequest.class))
                        .map(objectMapper::valueToTree);
                
            case "get_debate":
                return getDebate(objectMapper.convertValue(params, GetDebateToolRequest.class))
                        .map(objectMapper::valueToTree);
                
            case "list_debates":
                return listDebates(objectMapper.convertValue(params, ListDebatesToolRequest.class))
                        .map(objectMapper::valueToTree);
                
            case "submit_turn":
                return submitTurn(objectMapper.convertValue(params, SubmitTurnToolRequest.class))
                        .map(objectMapper::valueToTree);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<CreateDebateToolResponse> createDebate(CreateDebateToolRequest request) {
        DebateDto.CreateDebateRequest debateRequest = DebateDto.CreateDebateRequest.builder()
            .topic(request.getTopic())
            .format(request.getFormat())
            .organizationId(request.getOrganizationId())
            .title(request.getTitle() != null ? request.getTitle() : request.getTopic())
            .description(request.getDescription())
            .maxRounds(request.getMaxRounds())
            .settings(request.getSettings())
            .build();
        
        return Mono.fromCallable(() -> debateService.createDebate(debateRequest))
            .map(debate -> CreateDebateToolResponse.builder()
                .debateId(debate.getId())
                .status(debate.getStatus())
                .topic(debate.getTopic())
                .build());
    }

    private Mono<GetDebateToolResponse> getDebate(GetDebateToolRequest request) {
        return Mono.fromCallable(() -> debateService.getDebate(request.getDebateId()))
            .map(debate -> GetDebateToolResponse.builder()
                .id(debate.getId())
                .topic(debate.getTopic())
                .format(debate.getFormat())
                .status(debate.getStatus())
                .currentRound(debate.getCurrentRound())
                .maxRounds(debate.getMaxRounds())
                .participants(debate.getParticipants())
                .build());
    }

    private Mono<ListDebatesToolResponse> listDebates(ListDebatesToolRequest request) {
        return Mono.fromCallable(() -> 
            debateService.listDebates(request.getOrganizationId(), request.getStatus(), org.springframework.data.domain.Pageable.unpaged())
        ).map(debatesPage -> ListDebatesToolResponse.builder()
                .debates(debatesPage.getContent().stream().map(debate -> ListDebatesToolResponse.DebateInfo.builder()
                    .id(debate.getId())
                    .topic(debate.getTopic())
                    .status(debate.getStatus())
                    .createdAt(debate.getCreatedAt())
                    .build())
                    .collect(java.util.stream.Collectors.toList()))
                .build());
    }

    private Mono<SubmitTurnToolResponse> submitTurn(SubmitTurnToolRequest request) {
        return Mono.fromCallable(() -> {
            // Get the debate to find current round
            DebateDto debate = debateService.getDebate(request.getDebateId());
            
            // Get the list of rounds
            List<RoundDto> rounds = debateService.listRounds(request.getDebateId());
            
            // Find the current active round
            UUID currentRoundId = rounds.stream()
                    .filter(round -> "IN_PROGRESS".equals(round.getStatus()))
                    .map(RoundDto::getId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No active round found for debate"));
            
            // Submit the response
            ResponseDto.CreateResponseRequest createResponseRequest = ResponseDto.CreateResponseRequest.builder()
                .participantId(request.getParticipantId())
                .content(request.getContent())
                .build();
            
            ResponseDto responseDto = debateService.submitResponse(request.getDebateId(), currentRoundId, createResponseRequest);
            
            return SubmitTurnToolResponse.builder()
                .responseId(responseDto.getId())
                .roundId(responseDto.getRoundId())
                .status("submitted")
                .build();
        }).onErrorResume(error -> {
            // Handle errors and return a ProblemDetail-like response
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
            problemDetail.setTitle("Tool Call Error");
            problemDetail.setType(URI.create("/errors/tool-call"));
            problemDetail.setProperty("timestamp", Instant.now());
            return Mono.error(new ToolCallException(problemDetail));
        });
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}
