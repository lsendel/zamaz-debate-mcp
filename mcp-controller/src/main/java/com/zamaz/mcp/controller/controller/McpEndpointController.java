package com.zamaz.mcp.controller.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.ResponseDto;
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
                return createDebate(params);
                
            case "get_debate":
                return getDebate(params);
                
            case "list_debates":
                return listDebates(params);
                
            case "submit_turn":
                return submitTurn(params);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<JsonNode> createDebate(JsonNode params) {
        DebateDto.CreateDebateRequest request = DebateDto.CreateDebateRequest.builder()
            .topic(params.get("topic").asText())
            .format(params.get("format").asText())
            .organizationId(UUID.fromString(params.get("organizationId").asText()))
            .title(params.has("title") ? params.get("title").asText() : params.get("topic").asText())
            .description(params.has("description") ? params.get("description").asText() : null)
            .maxRounds(params.has("maxRounds") ? params.get("maxRounds").asInt() : 3)
            .settings(params.has("settings") ? params.get("settings") : null)
            .build();
        
        return Mono.fromCallable(() -> debateService.createDebate(request))
            .map(debate -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("debateId", debate.getId().toString());
                response.put("status", debate.getStatus());
                response.put("topic", debate.getTopic());
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> getDebate(JsonNode params) {
        UUID debateId = UUID.fromString(params.get("debateId").asText());
        
        return Mono.fromCallable(() -> debateService.getDebate(debateId))
            .map(debate -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("id", debate.getId().toString());
                response.put("topic", debate.getTopic());
                response.put("format", debate.getFormat());
                response.put("status", debate.getStatus());
                response.put("currentRound", debate.getCurrentRound());
                response.put("maxRounds", debate.getMaxRounds());
                
                ArrayNode participants = response.putArray("participants");
                if (debate.getParticipants() != null) {
                    debate.getParticipants().forEach(p -> {
                        ObjectNode participant = participants.addObject();
                        participant.put("id", p.getId().toString());
                        participant.put("name", p.getName());
                        participant.put("type", p.getType());
                    });
                }
                
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> listDebates(JsonNode params) {
        UUID organizationId = UUID.fromString(params.get("organizationId").asText());
        String status = params.has("status") ? params.get("status").asText() : null;
        
        return Mono.fromCallable(() -> 
            debateService.listDebates(organizationId, status, org.springframework.data.domain.Pageable.unpaged())
        ).map(debatesPage -> {
                ObjectNode response = objectMapper.createObjectNode();
                ArrayNode debatesArray = response.putArray("debates");
                
                debatesPage.getContent().forEach(debate -> {
                    ObjectNode debateNode = debatesArray.addObject();
                    debateNode.put("id", debate.getId().toString());
                    debateNode.put("topic", debate.getTopic());
                    debateNode.put("status", debate.getStatus());
                    debateNode.put("createdAt", debate.getCreatedAt().toString());
                });
                
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> submitTurn(JsonNode params) {
        UUID debateId = UUID.fromString(params.get("debateId").asText());
        UUID participantId = UUID.fromString(params.get("participantId").asText());
        String content = params.get("content").asText();
        
        return Mono.fromCallable(() -> {
            // Get the debate to find current round
            DebateDto debate = debateService.getDebate(debateId);
            
            // Get the list of rounds
            List<Object> rounds = debateService.listRounds(debateId);
            
            // Find the current active round
            UUID currentRoundId = null;
            for (Object roundObj : rounds) {
                @SuppressWarnings("unchecked")
                Map<String, Object> round = (Map<String, Object>) roundObj;
                if ("IN_PROGRESS".equals(round.get("status"))) {
                    currentRoundId = (UUID) round.get("id");
                    break;
                }
            }
            
            if (currentRoundId == null) {
                throw new IllegalStateException("No active round found for debate");
            }
            
            // Submit the response
            ResponseDto.CreateResponseRequest request = ResponseDto.CreateResponseRequest.builder()
                .participantId(participantId)
                .content(content)
                .build();
            
            ResponseDto responseDto = debateService.submitResponse(debateId, currentRoundId, request);
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("responseId", responseDto.getId().toString());
            response.put("roundId", responseDto.getRoundId().toString());
            response.put("status", "submitted");
            return (JsonNode) response;
        }).onErrorResume(error -> {
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("error", error.getMessage());
            return Mono.just((JsonNode) errorResponse);
        });
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}