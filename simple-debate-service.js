#!/usr/bin/env node

const express = require('express');
const cors = require('cors');
const app = express();

// Add fetch for Node.js (for making HTTP requests to LLM service)
const fetch = require('node-fetch');

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data store for debates and agentic flows
let debates = [
  {
    id: "debate-001",
    title: "AI Ethics in Healthcare",
    topic: "Should AI make medical decisions?",
    description: "A comprehensive debate on the ethical implications of AI in healthcare decision-making",
    status: "COMPLETED",
    format: "OXFORD",
    participants: [
      {
        id: "participant-001",
        name: "Claude 3 Opus",
        llmProvider: "anthropic",
        model: "claude-3-opus",
        systemPrompt: "You are arguing FOR AI making medical decisions. Present evidence-based arguments supporting AI's role in healthcare."
      },
      {
        id: "participant-002", 
        name: "GPT-4",
        llmProvider: "openai",
        model: "gpt-4",
        systemPrompt: "You are arguing AGAINST AI making medical decisions. Present thoughtful counterarguments emphasizing human oversight."
      }
    ],
    rounds: [
      {
        roundNumber: 1,
        status: "completed",
        responses: [
          {
            id: "response-001",
            participantId: "participant-001",
            roundNumber: 1,
            content: "AI should make medical decisions because it can process vast amounts of medical data instantaneously, reducing human error and bias. Studies show AI diagnostic systems achieve 95% accuracy rates in radiology, compared to 88% for human radiologists. AI systems don't suffer from fatigue, emotional stress, or cognitive biases that can cloud human judgment. Furthermore, AI can provide consistent, evidence-based recommendations 24/7, ensuring patients receive optimal care regardless of time or location.",
            timestamp: "2024-01-01T10:00:00Z",
            tokenCount: 89
          },
          {
            id: "response-002",
            participantId: "participant-002",
            roundNumber: 1,
            content: "While AI shows promise in healthcare, it should not make final medical decisions. Medicine requires empathy, ethical reasoning, and understanding of individual patient circumstances that AI cannot replicate. Medical decisions often involve complex social, psychological, and ethical factors beyond clinical data. AI systems can exhibit bias from training data and lack transparency in decision-making. Human doctors provide emotional support, explain treatment options, and make nuanced judgments that consider the whole person, not just clinical metrics.",
            timestamp: "2024-01-01T10:05:00Z",
            tokenCount: 95
          }
        ]
      },
      {
        roundNumber: 2,
        status: "completed",
        responses: [
          {
            id: "response-003",
            participantId: "participant-001",
            roundNumber: 2,
            content: "Human doctors already use AI-assisted tools extensively - from diagnostic imaging to treatment recommendations. The question isn't whether AI should be involved, but how to optimize its use. AI can augment human capabilities by flagging critical cases, suggesting treatment protocols, and monitoring patient responses in real-time. In emergency situations, AI can provide life-saving recommendations faster than human consultation. The key is proper implementation with appropriate safeguards, not avoiding AI altogether.",
            timestamp: "2024-01-01T10:10:00Z",
            tokenCount: 87
          },
          {
            id: "response-004",
            participantId: "participant-002",
            roundNumber: 2,
            content: "AI assistance and AI decision-making are fundamentally different. While AI tools can provide valuable insights, the final decision must remain with human physicians who can integrate AI recommendations with their clinical experience and patient relationships. Medical errors often stem from systemic issues, not just individual mistakes - issues that AI cannot address. Moreover, patients have the right to human judgment in their care decisions. The doctor-patient relationship is built on trust, communication, and shared decision-making that AI cannot replicate.",
            timestamp: "2024-01-01T10:15:00Z",
            tokenCount: 94
          }
        ]
      },
      {
        roundNumber: 3,
        status: "completed",
        responses: [
          {
            id: "response-005",
            participantId: "participant-001",
            roundNumber: 3,
            content: "The current healthcare system faces critical challenges: physician shortages, rising costs, and inconsistent care quality. AI can democratize access to high-quality medical decision-making, especially in underserved areas where specialist expertise is limited. AI systems can be continuously updated with the latest medical research and best practices, ensuring all patients benefit from cutting-edge knowledge. While human oversight remains important, we cannot let perfect be the enemy of good when AI can save lives and improve outcomes today.",
            timestamp: "2024-01-01T10:20:00Z",
            tokenCount: 92
          },
          {
            id: "response-006",
            participantId: "participant-002",
            roundNumber: 3,
            content: "The healthcare challenges you mention require systemic solutions, not just technological ones. AI can be a powerful tool in addressing these issues, but it should complement, not replace, human medical decision-making. We need to invest in healthcare infrastructure, train more physicians, and improve access to care. Rushing to replace human judgment with AI risks creating new problems: loss of medical expertise, over-reliance on technology, and potential for catastrophic failures. The goal should be human-AI collaboration, not AI replacement.",
            timestamp: "2024-01-01T10:25:00Z",
            tokenCount: 98
          }
        ]
      }
    ],
    maxRounds: 3,
    currentRound: 3,
    createdAt: "2024-01-01T00:00:00Z",
    updatedAt: "2024-01-01T10:30:00Z",
    completedAt: "2024-01-01T10:30:00Z",
    organizationId: "org-001",
    createdBy: "user-001"
  },
  {
    id: "debate-002", 
    title: "Climate Change Solutions",
    topic: "Is nuclear energy the answer to climate change?",
    description: "Examining nuclear energy as a solution to global climate challenges",
    status: "IN_PROGRESS",
    format: "LINCOLN_DOUGLAS",
    participants: [
      {
        id: "participant-003",
        name: "Claude 3 Sonnet",
        llmProvider: "anthropic", 
        model: "claude-3-sonnet",
        systemPrompt: "You are arguing FOR nuclear energy as a climate solution. Present scientific evidence and practical considerations."
      },
      {
        id: "participant-004",
        name: "Gemini Pro",
        llmProvider: "google",
        model: "gemini-pro",
        systemPrompt: "You are arguing for renewable alternatives to nuclear energy. Focus on sustainability and safety concerns."
      }
    ],
    rounds: [
      {
        roundNumber: 1,
        status: "completed",
        responses: [
          {
            id: "response-007",
            participantId: "participant-003",
            roundNumber: 1,
            content: "Nuclear energy is essential for addressing climate change because it provides reliable, carbon-free baseload power that renewable sources cannot match. Nuclear plants generate electricity 24/7 regardless of weather conditions, with capacity factors exceeding 90%. France generates 70% of its electricity from nuclear power and has among the lowest carbon emissions per capita in the developed world. Modern reactor designs are inherently safe, and the waste management challenges, while real, are manageable with proper technology and policies.",
            timestamp: "2024-01-02T14:00:00Z",
            tokenCount: 96
          },
          {
            id: "response-008",
            participantId: "participant-004",
            roundNumber: 1,
            content: "While nuclear energy is low-carbon, renewable alternatives like solar and wind are becoming increasingly cost-effective and scalable. The combination of renewables with battery storage and grid modernization can provide reliable clean energy without nuclear's risks. Solar and wind costs have plummeted 90% in the past decade, making them the cheapest electricity sources in many regions. Nuclear projects consistently face cost overruns and delays, while renewable installations can be deployed rapidly and at scale.",
            timestamp: "2024-01-02T14:05:00Z",
            tokenCount: 89
          }
        ]
      },
      {
        roundNumber: 2,
        status: "in_progress",
        responses: [
          {
            id: "response-009",
            participantId: "participant-003",
            roundNumber: 2,
            content: "Renewable energy intermittency remains a fundamental challenge that battery technology cannot yet solve at scale. Grid-scale energy storage is still expensive and limited in duration. Nuclear provides steady, predictable power that complements renewables perfectly. Countries like Germany that have moved away from nuclear have seen increased reliance on fossil fuels and higher carbon emissions. The IPCC consistently includes nuclear as a necessary component of deep decarbonization scenarios.",
            timestamp: "2024-01-02T14:10:00Z",
            tokenCount: 91
          }
        ]
      }
    ],
    maxRounds: 4,
    currentRound: 2,
    createdAt: "2024-01-02T00:00:00Z",
    updatedAt: "2024-01-02T14:10:00Z",
    organizationId: "org-001",
    createdBy: "user-002"
  },
  {
    id: "debate-003",
    title: "Future of Work",
    topic: "Will automation replace human jobs?",
    description: "Exploring the impact of automation on employment and the future workforce",
    status: "CREATED",
    format: "OXFORD",
    participants: [
      {
        id: "participant-005",
        name: "GPT-3.5 Turbo",
        llmProvider: "openai",
        model: "gpt-3.5-turbo",
        systemPrompt: "You are arguing that automation will largely replace human jobs. Present evidence of technological advancement and economic trends."
      },
      {
        id: "participant-006",
        name: "Claude 3 Opus",
        llmProvider: "anthropic",
        model: "claude-3-opus", 
        systemPrompt: "You are arguing that automation will create new opportunities and transform rather than eliminate human work."
      }
    ],
    rounds: [],
    maxRounds: 3,
    currentRound: 0,
    createdAt: "2024-01-03T00:00:00Z",
    updatedAt: "2024-01-03T00:00:00Z",
    organizationId: "org-002",
    createdBy: "user-003"
  }
];

// In-memory store for agentic flow configurations
let agenticFlowConfigurations = {
  // debate-level configurations
  debates: {},
  // participant-level configurations  
  participants: {}
};

// Sample agentic flow analytics data
let agenticFlowAnalytics = [
  {
    flowType: 'INTERNAL_MONOLOGUE',
    executionCount: 45,
    averageConfidence: 82.5,
    successRate: 0.95,
    averageExecutionTime: 1200
  },
  {
    flowType: 'SELF_CRITIQUE_LOOP', 
    executionCount: 32,
    averageConfidence: 88.2,
    successRate: 0.91,
    averageExecutionTime: 2800
  },
  {
    flowType: 'MULTI_AGENT_RED_TEAM',
    executionCount: 28,
    averageConfidence: 85.7,
    successRate: 0.89,
    averageExecutionTime: 3500
  }
];

// Get all debates
app.get('/api/v1/debates', (req, res) => {
  const { organizationId, status, format } = req.query;
  let filteredDebates = debates;
  
  if (organizationId) {
    filteredDebates = filteredDebates.filter(d => d.organizationId === organizationId);
  }
  
  if (status) {
    filteredDebates = filteredDebates.filter(d => d.status === status);
  }
  
  if (format) {
    filteredDebates = filteredDebates.filter(d => d.format === format);
  }
  
  res.json(filteredDebates);
});

// Get debate by ID
app.get('/api/v1/debates/:id', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  res.json(debate);
});

// Create new debate
app.post('/api/v1/debates', (req, res) => {
  const { title, topic, format = 'OXFORD', participants = [], organizationId, createdBy } = req.body;
  
  if (!title || !topic) {
    return res.status(400).json({ error: 'Title and topic are required' });
  }
  
  const newDebate = {
    id: `debate-${Date.now()}`,
    title,
    topic,
    status: 'CREATED',
    format,
    participants: Array.isArray(participants) ? participants : [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    organizationId: organizationId || 'org-001',
    createdBy: createdBy || 'user-001'
  };
  
  debates.push(newDebate);
  res.status(201).json(newDebate);
});

// Update debate
app.put('/api/v1/debates/:id', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const { title, topic, status, format, participants } = req.body;
  
  if (title) debate.title = title;
  if (topic) debate.topic = topic;
  if (status) debate.status = status;
  if (format) debate.format = format;
  if (participants && Array.isArray(participants)) debate.participants = participants;
  
  debate.updatedAt = new Date().toISOString();
  
  res.json(debate);
});

// Delete debate
app.delete('/api/v1/debates/:id', (req, res) => {
  const index = debates.findIndex(d => d.id === req.params.id);
  if (index === -1) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  debates.splice(index, 1);
  res.status(204).send();
});

// Start debate
app.post('/api/v1/debates/:id/start', async (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  if (debate.status !== 'CREATED') {
    return res.status(400).json({ error: 'Debate can only be started from CREATED status' });
  }
  
  debate.status = 'IN_PROGRESS';
  debate.currentRound = 1;
  debate.updatedAt = new Date().toISOString();
  
  // Initialize rounds array if not exists
  if (!debate.rounds) {
    debate.rounds = [];
  }
  
  console.log(`🚀 Starting debate: ${debate.title}`);
  console.log(`👥 Participants: ${debate.participants.length}`);
  
  // Start the first round asynchronously
  setTimeout(() => {
    generateNextRound(debate);
  }, 1000);
  
  res.json(debate);
});

// Complete debate
app.post('/api/v1/debates/:id/complete', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  if (debate.status !== 'IN_PROGRESS') {
    return res.status(400).json({ error: 'Debate can only be completed from IN_PROGRESS status' });
  }
  
  debate.status = 'COMPLETED';
  debate.updatedAt = new Date().toISOString();
  
  res.json(debate);
});

// Get debate formats
app.get('/api/v1/debate-formats', (req, res) => {
  res.json([
    {
      id: 'OXFORD',
      name: 'Oxford Style',
      description: 'Traditional Oxford-style debate with opening statements, rebuttals, and closing arguments'
    },
    {
      id: 'LINCOLN_DOUGLAS',
      name: 'Lincoln-Douglas',
      description: 'One-on-one debate format focusing on philosophical and ethical issues'
    },
    {
      id: 'PARLIAMENTARY',
      name: 'Parliamentary',
      description: 'Team-based debate format with government and opposition sides'
    },
    {
      id: 'STRUCTURED',
      name: 'Structured',
      description: 'Highly structured format with specific time limits and rounds'
    }
  ]);
});

// Get debate statistics
app.get('/api/v1/debates/stats', (req, res) => {
  const { organizationId } = req.query;
  let filteredDebates = debates;
  
  if (organizationId) {
    filteredDebates = filteredDebates.filter(d => d.organizationId === organizationId);
  }
  
  const stats = {
    total: filteredDebates.length,
    byStatus: {
      CREATED: filteredDebates.filter(d => d.status === 'CREATED').length,
      IN_PROGRESS: filteredDebates.filter(d => d.status === 'IN_PROGRESS').length,
      COMPLETED: filteredDebates.filter(d => d.status === 'COMPLETED').length
    },
    byFormat: {
      OXFORD: filteredDebates.filter(d => d.format === 'OXFORD').length,
      LINCOLN_DOUGLAS: filteredDebates.filter(d => d.format === 'LINCOLN_DOUGLAS').length,
      PARLIAMENTARY: filteredDebates.filter(d => d.format === 'PARLIAMENTARY').length,
      STRUCTURED: filteredDebates.filter(d => d.format === 'STRUCTURED').length
    }
  };
  
  res.json(stats);
});

// Function to generate next round of debate
async function generateNextRound(debate) {
  try {
    console.log(`🎯 Generating round ${debate.currentRound} for debate: ${debate.title}`);
    
    // Create new round
    const newRound = {
      roundNumber: debate.currentRound,
      status: 'in_progress',
      responses: []
    };
    
    // Add round to debate
    debate.rounds.push(newRound);
    
    // Generate responses for each participant
    for (let i = 0; i < debate.participants.length; i++) {
      const participant = debate.participants[i];
      console.log(`💬 Generating response for ${participant.name}...`);
      
      try {
        // Build conversation context
        const messages = buildConversationContext(debate, participant, debate.currentRound);
        
        // Call LLM service
        const response = await callLLMService(participant, messages);
        
        // Add response to round
        const responseObj = {
          id: `response-${Date.now()}-${i}`,
          participantId: participant.id,
          roundNumber: debate.currentRound,
          content: response.content,
          timestamp: new Date().toISOString(),
          tokenCount: response.tokenCount || estimateTokens(response.content)
        };
        
        newRound.responses.push(responseObj);
        console.log(`✅ Response generated for ${participant.name}: ${response.content.substring(0, 100)}...`);
        
        // Wait between participants to avoid rate limits
        if (i < debate.participants.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 2000));
        }
        
      } catch (error) {
        console.error(`❌ Error generating response for ${participant.name}:`, error.message);
        
        // Add fallback response
        const fallbackResponse = {
          id: `response-${Date.now()}-${i}`,
          participantId: participant.id,
          roundNumber: debate.currentRound,
          content: `I apologize, but I'm having trouble generating a response right now. Let me try again in the next round.`,
          timestamp: new Date().toISOString(),
          tokenCount: 20
        };
        
        newRound.responses.push(fallbackResponse);
      }
    }
    
    // Mark round as completed
    newRound.status = 'completed';
    debate.updatedAt = new Date().toISOString();
    
    console.log(`✅ Round ${debate.currentRound} completed for debate: ${debate.title}`);
    
    // Check if debate should continue
    if (debate.currentRound < debate.maxRounds) {
      debate.currentRound++;
      console.log(`🔄 Scheduling round ${debate.currentRound}...`);
      
      // Schedule next round
      setTimeout(() => {
        generateNextRound(debate);
      }, 5000); // Wait 5 seconds between rounds
    } else {
      // Complete the debate
      debate.status = 'COMPLETED';
      debate.completedAt = new Date().toISOString();
      console.log(`🏁 Debate completed: ${debate.title}`);
    }
    
  } catch (error) {
    console.error(`❌ Error in generateNextRound:`, error);
    debate.status = 'COMPLETED'; // Mark as completed even if error
  }
}

// Build conversation context for LLM
function buildConversationContext(debate, participant, currentRound) {
  const messages = [
    {
      role: 'system',
      content: participant.systemPrompt
    },
    {
      role: 'user',
      content: `You are participating in a debate on the topic: "${debate.topic}". This is round ${currentRound} of ${debate.maxRounds}.`
    }
  ];
  
  // Add previous rounds as context
  if (debate.rounds && debate.rounds.length > 0) {
    let context = 'Previous debate rounds:\n\n';
    
    debate.rounds.forEach(round => {
      if (round.roundNumber < currentRound && round.responses) {
        context += `Round ${round.roundNumber}:\n`;
        round.responses.forEach(response => {
          const respParticipant = debate.participants.find(p => p.id === response.participantId);
          const name = respParticipant ? respParticipant.name : 'Unknown';
          context += `${name}: ${response.content}\n\n`;
        });
      }
    });
    
    if (context.length > 50) {
      messages.push({
        role: 'user',
        content: context + `Now provide your response for round ${currentRound}. Keep it focused, well-reasoned, and around 100-150 words.`
      });
    }
  } else {
    messages.push({
      role: 'user',
      content: `Please provide your opening argument for this debate topic. Keep it focused, well-reasoned, and around 100-150 words.`
    });
  }
  
  return messages;
}

// Call LLM service
async function callLLMService(participant, messages) {
  try {
    const response = await fetch('http://localhost:5002/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: participant.model,
        messages: messages,
        max_tokens: 200,
        temperature: 0.7
      })
    });
    
    if (!response.ok) {
      throw new Error(`LLM service responded with status: ${response.status}`);
    }
    
    const data = await response.json();
    
    return {
      content: data.choices[0].message.content,
      tokenCount: data.usage?.total_tokens || estimateTokens(data.choices[0].message.content)
    };
    
  } catch (error) {
    console.error('❌ LLM service call failed:', error.message);
    throw error;
  }
}

// Estimate token count (simple approximation)
function estimateTokens(text) {
  return Math.ceil(text.length / 4);
}

// Export debate
app.get('/api/v1/debates/:id/export', (req, res) => {
  const debate = debates.find(d => d.id === req.params.id);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const format = req.query.format || 'json';
  
  if (format === 'json') {
    res.setHeader('Content-Type', 'application/json');
    res.setHeader('Content-Disposition', `attachment; filename="debate-${debate.id}.json"`);
    res.json(debate);
  } else if (format === 'markdown') {
    res.setHeader('Content-Type', 'text/markdown');
    res.setHeader('Content-Disposition', `attachment; filename="debate-${debate.id}.md"`);
    
    let markdown = `# ${debate.title}\n\n`;
    markdown += `**Topic:** ${debate.topic}\n\n`;
    if (debate.description) {
      markdown += `**Description:** ${debate.description}\n\n`;
    }
    markdown += `**Status:** ${debate.status}\n`;
    markdown += `**Format:** ${debate.format}\n\n`;
    
    markdown += `## Participants\n\n`;
    if (debate.participants && Array.isArray(debate.participants)) {
      debate.participants.forEach((participant, index) => {
        if (typeof participant === 'string') {
          markdown += `${index + 1}. ${participant}\n`;
        } else {
          markdown += `${index + 1}. **${participant.name}** (${participant.llmProvider} - ${participant.model})\n`;
          if (participant.systemPrompt) {
            markdown += `   - System Prompt: ${participant.systemPrompt}\n`;
          }
        }
      });
    }
    
    if (debate.rounds && debate.rounds.length > 0) {
      markdown += `\n## Debate Rounds\n\n`;
      debate.rounds.forEach((round) => {
        markdown += `### Round ${round.roundNumber}\n\n`;
        if (round.responses && round.responses.length > 0) {
          round.responses.forEach((response) => {
            const participant = debate.participants.find(p => p.id === response.participantId);
            const participantName = participant ? participant.name : 'Unknown';
            markdown += `**${participantName}** (${new Date(response.timestamp).toLocaleString()}):\n\n`;
            markdown += `${response.content}\n\n`;
            if (response.tokenCount) {
              markdown += `*Token count: ${response.tokenCount}*\n\n`;
            }
          });
        }
      });
    }
    
    res.send(markdown);
  } else if (format === 'pdf') {
    // For PDF, we'll return a simple text response since generating real PDF requires additional libraries
    res.setHeader('Content-Type', 'text/plain');
    res.setHeader('Content-Disposition', `attachment; filename="debate-${debate.id}.txt"`);
    res.send('PDF export not yet implemented. Please use JSON or Markdown format.');
  } else {
    res.status(400).json({ error: 'Unsupported format. Use json, markdown, or pdf' });
  }
});

// ============================================================================
// AGENTIC FLOW ENDPOINTS
// ============================================================================

// Configure debate-level agentic flow
app.post('/api/v1/debates/:debateId/agentic-flow', (req, res) => {
  const { debateId } = req.params;
  const { flowType, enabled = true, parameters = {} } = req.body;
  
  const debate = debates.find(d => d.id === debateId);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const configuration = {
    flowId: `flow-${Date.now()}`,
    flowType,
    enabled,
    parameters,
    debateId,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  
  agenticFlowConfigurations.debates[debateId] = configuration;
  
  console.log(`🧠 Configured agentic flow ${flowType} for debate ${debateId}`);
  res.status(201).json(configuration);
});

// Get debate-level agentic flow
app.get('/api/v1/debates/:debateId/agentic-flow', (req, res) => {
  const { debateId } = req.params;
  
  const debate = debates.find(d => d.id === debateId);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const configuration = agenticFlowConfigurations.debates[debateId];
  if (!configuration) {
    return res.status(404).json({ error: 'Agentic flow configuration not found' });
  }
  
  res.json(configuration);
});

// Configure participant-level agentic flow
app.post('/api/v1/debates/:debateId/participants/:participantId/agentic-flow', (req, res) => {
  const { debateId, participantId } = req.params;
  const { flowType, enabled = true, parameters = {} } = req.body;
  
  const debate = debates.find(d => d.id === debateId);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const participant = debate.participants.find(p => p.id === participantId);
  if (!participant) {
    return res.status(404).json({ error: 'Participant not found' });
  }
  
  const configuration = {
    flowId: `flow-${Date.now()}`,
    flowType,
    enabled,
    parameters,
    debateId,
    participantId,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  
  const key = `${debateId}-${participantId}`;
  agenticFlowConfigurations.participants[key] = configuration;
  
  console.log(`🧠 Configured agentic flow ${flowType} for participant ${participantId} in debate ${debateId}`);
  res.status(201).json(configuration);
});

// Get participant-level agentic flow
app.get('/api/v1/debates/:debateId/participants/:participantId/agentic-flow', (req, res) => {
  const { debateId, participantId } = req.params;
  
  const debate = debates.find(d => d.id === debateId);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  const participant = debate.participants.find(p => p.id === participantId);
  if (!participant) {
    return res.status(404).json({ error: 'Participant not found' });
  }
  
  const key = `${debateId}-${participantId}`;
  const configuration = agenticFlowConfigurations.participants[key];
  if (!configuration) {
    return res.status(404).json({ error: 'Agentic flow configuration not found' });
  }
  
  res.json(configuration);
});

// Get agentic flow analytics for debate
app.get('/api/v1/analytics/debates/:debateId/agentic-flows', (req, res) => {
  const { debateId } = req.params;
  
  const debate = debates.find(d => d.id === debateId);
  if (!debate) {
    return res.status(404).json({ error: 'Debate not found' });
  }
  
  // Return mock analytics data for the debate
  const analytics = {
    debateId,
    flowTypeSummaries: {
      INTERNAL_MONOLOGUE: {
        executionCount: 12,
        averageConfidence: 85.3,
        successRate: 0.92,
        averageExecutionTime: 1100
      },
      SELF_CRITIQUE_LOOP: {
        executionCount: 8,
        averageConfidence: 89.1,
        successRate: 0.88,
        averageExecutionTime: 2600
      }
    },
    totalExecutions: 20,
    averageConfidence: 87.2,
    successRate: 0.90
  };
  
  res.json(analytics);
});

// Get flow type statistics
app.get('/api/v1/analytics/agentic-flows/statistics', (req, res) => {
  const { organizationId, startDate, endDate } = req.query;
  
  // Return mock statistics data
  res.json(agenticFlowAnalytics);
});

// Get flow execution time series
app.get('/api/v1/analytics/agentic-flows/time-series', (req, res) => {
  const { organizationId, startDate, endDate } = req.query;
  
  // Generate mock time series data
  const timeSeries = [];
  const now = new Date();
  for (let i = 7; i >= 0; i--) {
    const date = new Date(now);
    date.setDate(date.getDate() - i);
    timeSeries.push({
      date: date.toISOString().split('T')[0],
      executions: Math.floor(Math.random() * 20) + 5,
      averageConfidence: Math.floor(Math.random() * 20) + 75,
      successRate: 0.85 + Math.random() * 0.1
    });
  }
  
  res.json(timeSeries);
});

// Get trending flow types
app.get('/api/v1/analytics/agentic-flows/trending', (req, res) => {
  const { organizationId, limit = 10 } = req.query;
  
  // Return mock trending data
  const trending = [
    {
      flowType: 'INTERNAL_MONOLOGUE',
      usageCount: 45,
      averageConfidence: 82.5,
      successRate: 0.95,
      averageExecutionTime: 1200,
      trendScore: 0.85,
      trendCategory: 'Hot'
    },
    {
      flowType: 'SELF_CRITIQUE_LOOP',
      usageCount: 32,
      averageConfidence: 88.2,
      successRate: 0.91,
      averageExecutionTime: 2800,
      trendScore: 0.72,
      trendCategory: 'Rising'
    },
    {
      flowType: 'TOOL_CALLING_VERIFICATION',
      usageCount: 28,
      averageConfidence: 85.7,
      successRate: 0.89,
      averageExecutionTime: 3500,
      trendScore: 0.68,
      trendCategory: 'Stable'
    }
  ];
  
  res.json(trending.slice(0, parseInt(limit)));
});

// Compare flow types
app.post('/api/v1/analytics/agentic-flows/compare', (req, res) => {
  const { organizationId, flowTypes, startDate, endDate } = req.body;
  
  // Return mock comparison data
  const comparison = {
    organizationId,
    flowTypes,
    comparison: flowTypes.map(flowType => ({
      flowType,
      metrics: {
        executionCount: Math.floor(Math.random() * 50) + 10,
        averageConfidence: Math.floor(Math.random() * 20) + 75,
        successRate: 0.8 + Math.random() * 0.15,
        averageExecutionTime: Math.floor(Math.random() * 3000) + 1000
      }
    }))
  };
  
  res.json(comparison);
});

// Health check
app.get('/actuator/health', (req, res) => {
  res.json({ status: 'UP' });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Something went wrong!' });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

const PORT = process.env.PORT || 5013;
app.listen(PORT, () => {
  console.log(`🚀 Simple Debate Service running on port ${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   GET    /api/v1/debates - List all debates`);
  console.log(`   POST   /api/v1/debates - Create new debate`);
  console.log(`   GET    /api/v1/debates/:id - Get debate by ID`);
  console.log(`   PUT    /api/v1/debates/:id - Update debate`);
  console.log(`   DELETE /api/v1/debates/:id - Delete debate`);
  console.log(`   POST   /api/v1/debates/:id/start - Start debate`);
  console.log(`   POST   /api/v1/debates/:id/complete - Complete debate`);
  console.log(`   GET    /api/v1/debates/:id/export - Export debate (json, markdown, pdf)`);
  console.log(`   GET    /api/v1/debate-formats - Get available debate formats`);
  console.log(`   GET    /api/v1/debates/stats - Get debate statistics`);
  console.log(`   🧠 AGENTIC FLOW ENDPOINTS:`);
  console.log(`   POST   /api/v1/debates/:id/agentic-flow - Configure debate agentic flow`);
  console.log(`   GET    /api/v1/debates/:id/agentic-flow - Get debate agentic flow`);
  console.log(`   POST   /api/v1/debates/:id/participants/:pid/agentic-flow - Configure participant flow`);
  console.log(`   GET    /api/v1/debates/:id/participants/:pid/agentic-flow - Get participant flow`);
  console.log(`   GET    /api/v1/analytics/debates/:id/agentic-flows - Get debate analytics`);
  console.log(`   GET    /api/v1/analytics/agentic-flows/statistics - Get flow statistics`);
  console.log(`   GET    /api/v1/analytics/agentic-flows/time-series - Get time series data`);
  console.log(`   GET    /api/v1/analytics/agentic-flows/trending - Get trending flows`);
  console.log(`   POST   /api/v1/analytics/agentic-flows/compare - Compare flow types`);
  console.log(`   GET    /actuator/health - Health check`);
  console.log(`\n✅ Service is ready to accept requests!`);
  console.log(`\n📊 Available debates: ${debates.length}`);
});