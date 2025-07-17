package com.zamaz.mcp.testing.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Provider contract verification test for Debate Controller
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("DebateController")
@PactFolder("pacts") // Load pacts from local folder
// @PactBroker(url = "${PACT_BROKER_URL}", authentication = @PactBrokerAuth(token = "${PACT_BROKER_TOKEN}"))
public class DebateControllerProviderPactTest {
    
    @LocalServerPort
    private int port;
    
    @MockBean
    private DebateService debateService;
    
    @MockBean
    private AuthorizationService authorizationService;
    
    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }
    
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
    
    @State("User is authenticated and authorized")
    void userIsAuthenticated() {
        // Mock authentication and authorization
        when(authorizationService.hasPermission(any(), any(), any())).thenReturn(true);
    }
    
    @State("Organization has debates")
    void organizationHasDebates() {
        // Mock debate data
        DebateDto debate1 = DebateDto.builder()
            .id(UUID.randomUUID())
            .title("AI Ethics Debate")
            .topic("Should AI be regulated?")
            .status("ACTIVE")
            .createdAt(LocalDateTime.now())
            .participantCount(2)
            .build();
        
        DebateDto debate2 = DebateDto.builder()
            .id(UUID.randomUUID())
            .title("Climate Change Solutions")
            .topic("Best approaches to combat climate change")
            .status("ACTIVE")
            .createdAt(LocalDateTime.now())
            .participantCount(3)
            .build();
        
        Page<DebateDto> debates = new PageImpl<>(
            Arrays.asList(debate1, debate2),
            PageRequest.of(0, 10),
            2
        );
        
        when(debateService.getDebates(any(), any(), any())).thenReturn(debates);
    }
    
    @State("Debate exists and is accepting participants")
    void debateExistsAndAcceptingParticipants() {
        UUID debateId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        DebateDto debate = DebateDto.builder()
            .id(debateId)
            .title("AI Ethics Debate")
            .topic("Should AI be regulated?")
            .status("CREATED")
            .createdAt(LocalDateTime.now())
            .participantCount(0)
            .build();
        
        when(debateService.getDebate(debateId)).thenReturn(debate);
        when(debateService.canAddParticipant(debateId)).thenReturn(true);
        
        ParticipantDto participant = ParticipantDto.builder()
            .id(UUID.randomUUID())
            .debateId(debateId)
            .type("AI")
            .name("Claude")
            .joinedAt(LocalDateTime.now())
            .build();
        
        when(debateService.addParticipant(eq(debateId), any())).thenReturn(participant);
    }
}