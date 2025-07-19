#!/usr/bin/env node

const express = require('express');
const cors = require('cors');
const app = express();

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data store for LLM providers and models
const providers = [
  {
    id: "openai",
    name: "OpenAI",
    apiEndpoint: "https://api.openai.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "gpt-4",
        name: "GPT-4",
        provider: "openai",
        type: "CHAT",
        maxTokens: 8192,
        costPerToken: 0.00003,
        capabilities: ["text-generation", "code-generation", "analysis"]
      },
      {
        id: "gpt-3.5-turbo",
        name: "GPT-3.5 Turbo",
        provider: "openai", 
        type: "CHAT",
        maxTokens: 4096,
        costPerToken: 0.000002,
        capabilities: ["text-generation", "conversation"]
      }
    ]
  },
  {
    id: "anthropic",
    name: "Anthropic",
    apiEndpoint: "https://api.anthropic.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "claude-3-opus",
        name: "Claude 3 Opus",
        provider: "anthropic",
        type: "CHAT",
        maxTokens: 200000,
        costPerToken: 0.000015,
        capabilities: ["text-generation", "analysis", "reasoning"]
      },
      {
        id: "claude-3-sonnet",
        name: "Claude 3 Sonnet", 
        provider: "anthropic",
        type: "CHAT",
        maxTokens: 200000,
        costPerToken: 0.000003,
        capabilities: ["text-generation", "analysis"]
      }
    ]
  },
  {
    id: "google",
    name: "Google AI",
    apiEndpoint: "https://generativelanguage.googleapis.com/v1",
    authType: "API_KEY",
    status: "ACTIVE",
    models: [
      {
        id: "gemini-pro",
        name: "Gemini Pro",
        provider: "google",
        type: "CHAT",
        maxTokens: 32768,
        costPerToken: 0.00000025,
        capabilities: ["text-generation", "multimodal", "reasoning"]
      }
    ]
  }
];

// Get all providers
app.get('/api/v1/providers', (req, res) => {
  res.json(providers);
});

// Get provider by ID
app.get('/api/v1/providers/:id', (req, res) => {
  const provider = providers.find(p => p.id === req.params.id);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  res.json(provider);
});

// Get all models
app.get('/api/v1/models', (req, res) => {
  const allModels = providers.flatMap(p => p.models);
  res.json(allModels);
});

// Get models by provider
app.get('/api/v1/providers/:providerId/models', (req, res) => {
  const provider = providers.find(p => p.id === req.params.providerId);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  res.json(provider.models);
});

// Get model by ID
app.get('/api/v1/models/:id', (req, res) => {
  const allModels = providers.flatMap(p => p.models);
  const model = allModels.find(m => m.id === req.params.id);
  if (!model) {
    return res.status(404).json({ error: 'Model not found' });
  }
  res.json(model);
});

// Simple chat completion endpoint (mock)
app.post('/api/v1/chat/completions', (req, res) => {
  const { model, messages, max_tokens = 100 } = req.body;
  
  if (!model || !messages || !Array.isArray(messages)) {
    return res.status(400).json({ error: 'Model and messages are required' });
  }
  
  // Find the model
  const allModels = providers.flatMap(p => p.models);
  const selectedModel = allModels.find(m => m.id === model);
  
  if (!selectedModel) {
    return res.status(404).json({ error: 'Model not found' });
  }
  
  // Generate realistic debate response based on context
  const content = generateDebateResponse(model, messages);
  
  const response = {
    id: `chatcmpl-${Date.now()}`,
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model: model,
    choices: [
      {
        index: 0,
        message: {
          role: "assistant",
          content: content
        },
        finish_reason: "stop"
      }
    ],
    usage: {
      prompt_tokens: estimateTokens(messages.map(m => m.content).join(' ')),
      completion_tokens: estimateTokens(content),
      total_tokens: estimateTokens(messages.map(m => m.content).join(' ') + content)
    }
  };
  
  res.json(response);
});

// Provider health check
app.get('/api/v1/providers/:id/health', (req, res) => {
  const provider = providers.find(p => p.id === req.params.id);
  if (!provider) {
    return res.status(404).json({ error: 'Provider not found' });
  }
  
  res.json({
    provider: provider.id,
    status: provider.status,
    timestamp: new Date().toISOString(),
    latency: Math.floor(Math.random() * 100) + 20, // Mock latency
    available: true
  });
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

const PORT = process.env.PORT || 5002;
app.listen(PORT, () => {
  console.log(`🚀 Simple LLM Service running on port ${PORT}`);
  console.log(`📋 Available endpoints:`);
  console.log(`   GET    /api/v1/providers - List all LLM providers`);
  console.log(`   GET    /api/v1/providers/:id - Get provider by ID`);
  console.log(`   GET    /api/v1/models - List all available models`);
  console.log(`   GET    /api/v1/providers/:providerId/models - Get models by provider`);
  console.log(`   GET    /api/v1/models/:id - Get model by ID`);
  console.log(`   POST   /api/v1/chat/completions - Chat completion (mock)`);
  console.log(`   GET    /api/v1/providers/:id/health - Check provider health`);
  console.log(`   GET    /actuator/health - Health check`);
  console.log(`\n✅ Service is ready to accept requests!`);
  console.log(`\n📊 Available providers: ${providers.length}`);
  console.log(`📊 Available models: ${providers.flatMap(p => p.models).length}`);
});

// Generate realistic debate responses based on context
function generateDebateResponse(model, messages) {
  const systemMessage = messages.find(m => m.role === 'system');
  const userMessages = messages.filter(m => m.role === 'user').map(m => m.content).join(' ');
  
  // Extract debate topic and context
  const topicMatch = userMessages.match(/topic:\s*"([^"]+)"/i);
  const topic = topicMatch ? topicMatch[1] : 'this topic';
  
  const roundMatch = userMessages.match(/round\s+(\d+)/i);
  const round = roundMatch ? parseInt(roundMatch[1]) : 1;
  
  // Determine position (pro/con) from system prompt
  const systemPrompt = systemMessage?.content || '';
  const isProPosition = systemPrompt.toLowerCase().includes('for') || 
                       systemPrompt.toLowerCase().includes('support') ||
                       systemPrompt.toLowerCase().includes('favor');
  
  // Generate response based on topic and position
  return generateTopicBasedResponse(topic, isProPosition, round, model, userMessages);
}

function generateTopicBasedResponse(topic, isProPosition, round, model, context) {
  const topicLower = topic.toLowerCase();
  
  // AI/Automation topics
  if (topicLower.includes('automation') || topicLower.includes('ai') || topicLower.includes('artificial intelligence')) {
    if (isProPosition) {
      return generateAutomationProResponse(round, context);
    } else {
      return generateAutomationConResponse(round, context);
    }
  }
  
  // Healthcare topics
  if (topicLower.includes('healthcare') || topicLower.includes('medical')) {
    if (isProPosition) {
      return generateHealthcareProResponse(round, context);
    } else {
      return generateHealthcareConResponse(round, context);
    }
  }
  
  // Nuclear energy topics
  if (topicLower.includes('nuclear') || topicLower.includes('energy')) {
    if (isProPosition) {
      return generateNuclearProResponse(round, context);
    } else {
      return generateNuclearConResponse(round, context);
    }
  }
  
  // Generic debate response
  return generateGenericResponse(topic, isProPosition, round);
}

function generateAutomationProResponse(round, context) {
  const responses = [
    "Automation will indeed replace many human jobs, but this represents economic progress, not catastrophe. Historical precedent shows that technological advancement ultimately creates more prosperity. The Industrial Revolution displaced agricultural workers, yet living standards improved dramatically. Today's automation eliminates repetitive, dangerous tasks while creating new opportunities in robotics, AI development, and human-machine collaboration. Countries embracing automation like South Korea and Japan maintain low unemployment while achieving higher productivity. The key is ensuring proper transition support and education systems that prepare workers for emerging roles.",
    
    "The evidence for job displacement is overwhelming. Oxford Economics predicts 20 million manufacturing jobs will be automated by 2030. Amazon's warehouses already use 750,000 robots, reducing human worker needs by 50%. Self-driving vehicles threaten 3.5 million truck driving jobs in the US alone. Unlike previous technological shifts that took generations, AI automation is advancing exponentially. Machine learning now performs cognitive tasks once thought uniquely human - from medical diagnosis to legal research. While new jobs emerge, they require higher skills and education, leaving many current workers behind.",
    
    "We must acknowledge the reality: automation is economically inevitable because it provides massive competitive advantages. Companies adopting automation see 40-60% cost reductions and 24/7 operational capacity. China's factories are rapidly automating to maintain manufacturing dominance. Resisting this trend means falling behind economically. Rather than fighting automation, we should focus on universal basic income, retraining programs, and reduced working hours to distribute automation's benefits. The transition may be challenging, but automation ultimately frees humans from mundane labor to pursue more creative, interpersonal, and strategic work."
  ];
  return responses[round - 1] || responses[0];
}

function generateAutomationConResponse(round, context) {
  const responses = [
    "Automation will transform work, not eliminate it. History shows technology creates new job categories while eliminating others. The computer revolution automated calculations yet created millions of programming, support, and design jobs. Automation handles routine tasks, allowing humans to focus on creativity, emotional intelligence, and complex problem-solving. Future work will emphasize uniquely human skills: empathy, leadership, artistic expression, and innovation. Healthcare, education, entertainment, and personal services continue growing precisely because they require human connection. Smart automation augments human capability rather than replacing it entirely.",
    
    "The automation-displacement narrative ignores emerging job markets and human adaptability. While some manufacturing jobs disappear, new roles emerge in robot maintenance, programming, and oversight. The gig economy demonstrates humans' ability to create novel work arrangements. Automation often creates more jobs than it eliminates - ATMs increased bank teller employment by enabling branch expansion. Small businesses, creative industries, and service sectors remain fundamentally human-centered. Educational institutions are already adapting curricula for emerging fields like cybersecurity, renewable energy, and biotechnology.",
    
    "The future lies in human-AI collaboration, not replacement. Successful automation implementations typically augment human workers rather than replacing them. Radiologists use AI for initial screening but provide final diagnosis. Financial advisors use robo-advisors for portfolio management while focusing on client relationships. Manufacturing workers oversee automated systems while handling exceptions and quality control. This symbiotic relationship leverages both machine efficiency and human judgment. Companies investing in worker retraining and human-AI partnerships consistently outperform those pursuing full automation."
  ];
  return responses[round - 1] || responses[0];
}

function generateHealthcareProResponse(round, context) {
  const responses = [
    "AI should make medical decisions because it consistently outperforms human physicians in diagnostic accuracy and treatment selection. IBM Watson for Oncology analyzes thousands of medical studies in seconds, identifying treatment options human doctors might miss. AI systems don't suffer from fatigue, emotional bias, or ego that can cloud medical judgment. Google's AI achieved 94% accuracy in diabetic retinopathy detection versus 88% for human specialists. In radiology, AI identifies cancers 17% more accurately than radiologists. When lives are at stake, we must prioritize evidence-based accuracy over human intuition.",
    
    "Medical errors kill 250,000 Americans annually - largely due to human limitations AI could prevent. Diagnostic errors account for 40,000-80,000 hospital deaths yearly. AI systems process vast datasets continuously updated with latest research, ensuring treatment recommendations reflect cutting-edge knowledge. They can monitor patient vitals 24/7, predicting complications before symptoms appear. Emergency departments using AI triage reduce wait times and identify critical cases faster. Rural areas with physician shortages could access world-class medical expertise through AI systems. The question isn't whether AI should make medical decisions, but how quickly we can safely implement these life-saving technologies.",
    
    "Opposition to AI in medicine stems from emotional attachment to human authority rather than rational evaluation of outcomes. We already trust automated systems for airline navigation, nuclear plant controls, and financial transactions - all life-critical decisions. Medical AI transparency exceeds human decision-making; algorithms can explain every factor in their recommendations, while human doctors often rely on intuition they can't articulate. Patients deserve the most accurate diagnosis and treatment possible. If AI provides better outcomes than human physicians - and evidence increasingly shows it does - ethical obligation demands we embrace these tools to save lives and reduce suffering."
  ];
  return responses[round - 1] || responses[0];
}

function generateHealthcareConResponse(round, context) {
  const responses = [
    "AI should assist, not replace, human medical decision-making because healthcare involves complex human factors beyond clinical data. Every patient brings unique circumstances, values, and concerns that require empathy and understanding. A cancer diagnosis demands not just treatment selection but emotional support, family consultation, and consideration of quality-of-life preferences. AI cannot provide comfort, explain complex decisions in understandable terms, or adapt to a patient's cultural background. The doctor-patient relationship built on trust and communication remains irreplaceable for effective healthcare delivery.",
    
    "Medical practice requires nuanced judgment that AI cannot replicate. Unusual presentations, rare conditions, and complex cases often require creative thinking and pattern recognition beyond algorithmic capability. Dr. House's diagnostic brilliance came from connecting seemingly unrelated symptoms - human insight algorithms miss. Patient histories contain subjective elements AI misinterprets: pain descriptions, lifestyle factors, and psychosocial issues affecting health. Moreover, medical ethics involve value judgments about quality of life, treatment intensity, and resource allocation that require human moral reasoning, not computational optimization.",
    
    "The risks of AI-dependent medicine are too great to accept. Algorithmic bias could perpetuate healthcare disparities if training data underrepresents minority populations. System failures or cyberattacks could paralyze healthcare delivery. AI lacks accountability - when diagnosis is wrong, who bears responsibility? Patients need human advocates who can challenge AI recommendations when clinical intuition suggests alternatives. Medical training would atrophy if doctors become mere AI operators, reducing healthcare to algorithmic processes. The art of medicine - combining science with compassion, experience with innovation - defines quality healthcare that technology cannot replicate."
  ];
  return responses[round - 1] || responses[0];
}

function generateNuclearProResponse(round, context) {
  const responses = [
    "Nuclear energy is essential for addressing climate change because it provides reliable, carbon-free baseload power that renewable sources cannot match. Nuclear plants operate at 90%+ capacity factors regardless of weather, while solar and wind average 25-35%. France generates 70% of its electricity from nuclear power and has among the lowest carbon emissions per capita in developed nations. Modern reactor designs are inherently safe with passive safety systems. Nuclear waste, while requiring careful management, represents a manageable challenge compared to atmospheric carbon dioxide's global impact.",
    
    "Renewable energy alone cannot meet global decarbonization timelines without nuclear power. Storage technology for intermittent renewables remains expensive and limited in duration. Grid-scale batteries can store hours of electricity, but nuclear provides weeks of continuous power. Germany's nuclear phaseout led to increased coal burning and higher emissions. The IPCC consistently includes nuclear as necessary for limiting warming to 1.5°C. China and India are rapidly expanding nuclear capacity precisely because they recognize renewables' limitations. Climate urgency demands all clean energy technologies, including nuclear.",
    
    "Nuclear energy offers energy security and economic benefits beyond climate goals. Uranium fuel is energy-dense and domestically available in many countries, reducing dependence on fossil fuel imports. Nuclear plants provide high-paying jobs and stable tax revenue for decades. Small modular reactors promise cheaper, faster deployment while maintaining safety standards. Countries with strong nuclear programs like South Korea and UAE leverage this expertise for exports. Abandoning nuclear technology cedes leadership to countries that continue developing it, while perpetuating fossil fuel dependence."
  ];
  return responses[round - 1] || responses[0];
}

function generateNuclearConResponse(round, context) {
  const responses = [
    "Renewable energy alternatives have become so cost-effective and scalable that nuclear power is no longer necessary for decarbonization. Solar and wind costs have plummeted 85% in the past decade, making them the cheapest electricity sources globally. Battery storage costs are dropping rapidly, while grid modernization enables better renewable integration. Countries like Costa Rica and Uruguay generate nearly 100% renewable electricity. Distributed solar with home batteries provides energy independence without nuclear risks. Denmark generates 140% of its electricity needs from wind, exporting excess to neighbors.",
    
    "Nuclear projects consistently face massive cost overruns and delays that undermine climate goals. Vogtle nuclear plants in Georgia are 7 years behind schedule and $17 billion over budget. The V.C. Summer project was cancelled after $9 billion in costs. French EPR reactors in Finland and France face similar problems. Meanwhile, renewable installations can be deployed rapidly at scale. Tesla's South Australia battery installation was completed in 100 days. Utility-scale solar projects typically finish within 1-2 years. Climate change demands urgent action that nuclear's long development timelines cannot provide.",
    
    "Nuclear waste and safety concerns make renewable alternatives preferable for sustainable energy future. High-level radioactive waste remains dangerous for thousands of years with no permanent storage solution in most countries. Fukushima demonstrated that even advanced nations cannot guarantee nuclear safety. Small modular reactors are unproven technology that may introduce new risks. Meanwhile, renewable energy creates jobs without long-term environmental liabilities. Offshore wind and desert solar offer virtually unlimited clean energy potential. Smart grids, demand response, and sector coupling can manage renewable intermittency more effectively than nuclear baseload power."
  ];
  return responses[round - 1] || responses[0];
}

function generateGenericResponse(topic, isProPosition, round) {
  if (isProPosition) {
    return `I strongly support the position regarding ${topic}. The evidence clearly demonstrates the benefits and necessity of this approach. Consider the practical advantages, economic benefits, and long-term positive outcomes. Current trends and expert analysis support this position. Historical precedent shows similar approaches have been successful. The risks of not adopting this position outweigh the potential downsides. Implementation challenges can be addressed through proper planning and gradual adoption.`;
  } else {
    return `I have serious concerns about ${topic}. While there may be some benefits, the risks and negative consequences are significant. We must consider unintended consequences, ethical implications, and impacts on vulnerable populations. Alternative approaches may be more effective and safer. The evidence supporting this position is often overstated or based on flawed assumptions. We should proceed with caution and explore other options before committing to this path.`;
  }
}

// Simple token estimation
function estimateTokens(text) {
  return Math.ceil(text.length / 4);
}