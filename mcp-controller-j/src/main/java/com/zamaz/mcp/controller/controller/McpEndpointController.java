package com.zamaz.mcp.controller.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.controller.service.DebateService;
import com.zamaz.mcp.controller.dto.DebateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
        CreateDebateRequest request = new CreateDebateRequest();
        request.setTopic(params.get("topic").asText());
        request.setFormat(params.get("format").asText());
        request.setOrganizationId(params.get("organizationId").asText());
        
        if (params.has("participants")) {
            ArrayNode participantsNode = (ArrayNode) params.get("participants");
            String[] participants = new String[participantsNode.size()];
            for (int i = 0; i < participantsNode.size(); i++) {
                participants[i] = participantsNode.get(i).asText();
            }
            request.setParticipantNames(participants);
        }
        
        if (params.has("maxRounds")) {
            request.setMaxRounds(params.get("maxRounds").asInt());
        }
        
        return debateService.createDebate(request)
            .map(debate -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("debateId", debate.getId());
                response.put("status", debate.getStatus().toString());
                response.put("topic", debate.getTopic());
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> getDebate(JsonNode params) {
        String debateId = params.get("debateId").asText();
        
        return debateService.getDebate(debateId)
            .map(debate -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("id", debate.getId());
                response.put("topic", debate.getTopic());
                response.put("format", debate.getFormat().toString());
                response.put("status", debate.getStatus().toString());
                response.put("currentRound", debate.getCurrentRound());
                response.put("maxRounds", debate.getMaxRounds());
                
                ArrayNode participants = response.putArray("participants");
                debate.getParticipants().forEach(p -> {
                    ObjectNode participant = participants.addObject();
                    participant.put("id", p.getId());
                    participant.put("name", p.getName());
                    participant.put("type", p.getType().toString());
                });
                
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> listDebates(JsonNode params) {
        String organizationId = params.get("organizationId").asText();
        
        return debateService.listDebates(organizationId)
            .collectList()
            .map(debates -> {
                ObjectNode response = objectMapper.createObjectNode();
                ArrayNode debatesArray = response.putArray("debates");
                
                debates.forEach(debate -> {
                    ObjectNode debateNode = debatesArray.addObject();
                    debateNode.put("id", debate.getId());
                    debateNode.put("topic", debate.getTopic());
                    debateNode.put("status", debate.getStatus().toString());
                    debateNode.put("createdAt", debate.getCreatedAt().toString());
                });
                
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> submitTurn(JsonNode params) {
        String debateId = params.get("debateId").asText();
        String participantId = params.get("participantId").asText();
        String content = params.get("content").asText();
        
        return debateService.submitTurn(debateId, participantId, content)
            .map(turn -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("turnId", turn.getId());
                response.put("round", turn.getRound());
                response.put("status", "submitted");
                return (JsonNode) response;
            });
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}