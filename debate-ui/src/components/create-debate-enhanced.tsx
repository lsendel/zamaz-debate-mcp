'use client';

import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Participant, DebateRules } from '@/types/debate';
import { ModelSelector } from '@/components/llm/model-selector';
import { useLLM } from '@/hooks/use-llm';
import { Plus, Trash2, Users, Brain, Sparkles, MessageSquare, AlertCircle, BookOpen, Target, Info, FileText } from 'lucide-react';

// Constants
const DEFAULT_MAX_ROUNDS = 5;
const DEFAULT_MAX_TURN_LENGTH = 500;
const DEFAULT_TEMPERATURE = 0.7;
const MIN_PARTICIPANTS = 2;

// Debate formats with descriptions
const DEBATE_FORMATS = [
  { 
    value: 'round_robin', 
    label: 'Round Robin', 
    description: 'Each participant takes turns in order'
  },
  { 
    value: 'oxford', 
    label: 'Oxford Style', 
    description: 'Formal debate with opening statements, rebuttals, and closing'
  },
  { 
    value: 'panel', 
    label: 'Panel Discussion', 
    description: 'Open discussion format with moderator'
  },
  { 
    value: 'socratic', 
    label: 'Socratic Method', 
    description: 'Question-driven exploration of ideas'
  },
  { 
    value: 'adversarial', 
    label: 'Adversarial', 
    description: 'Direct opposition with point-counterpoint'
  },
  { 
    value: 'collaborative', 
    label: 'Collaborative', 
    description: 'Working together to find solutions'
  }
];

interface DebateQuestion {
  mainQuestion: string;
  subQuestions?: string[];
  constraints?: string[];
  successCriteria?: string[];
}

interface DebateContext {
  background: string;
  relevantFacts?: string[];
  assumptions?: string[];
  scope?: string;
  outOfScope?: string[];
}

interface CreateDebateEnhancedProps {
  open: boolean;
  onOpenChange: (_open: boolean) => void;
  onSubmit: (_debate: {
    name: string;
    question: DebateQuestion;
    context: DebateContext;
    subject: string;
    expectedOutcome?: string;
    participants: Participant[];
    rules: DebateRules;
  }) => void;
  onStartDebate?: (_debateId: string) => void;
}

export function CreateDebateEnhanced({ 
  open, 
  onOpenChange, 
  onSubmit,
  onStartDebate 
}: CreateDebateEnhancedProps) {
  const { models, health } = useLLM();
  
  // Basic info
  const [name, setName] = useState('');
  const [subject, setSubject] = useState('');
  const [expectedOutcome, setExpectedOutcome] = useState('');
  
  // Question
  const [question, setQuestion] = useState<DebateQuestion>({
    mainQuestion: '',
    subQuestions: [],
    constraints: [],
    successCriteria: []
  });
  
  // Context
  const [context, setContext] = useState<DebateContext>({
    background: '',
    relevantFacts: [],
    assumptions: [],
    scope: '',
    outOfScope: []
  });
  
  // Participants and rules
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [rules, setRules] = useState<DebateRules>({
    format: 'round_robin',
    maxRounds: DEFAULT_MAX_ROUNDS,
    maxTurnLength: DEFAULT_MAX_TURN_LENGTH
  });
  
  // UI state
  const [activeTab, setActiveTab] = useState('question');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize default participants
  useEffect(() => {
    if (models.length > 0 && participants.length === 0) {
      const defaultModel = models[0];
      setParticipants([
        {
          name: 'Expert Advocate',
          role: 'debater',
          position: 'Primary perspective',
          llm_config: {
            provider: defaultModel.provider as 'claude' | 'openai' | 'gemini' | 'llama',
            model: defaultModel.id,
            temperature: DEFAULT_TEMPERATURE,
            systemPrompt: 'You are an expert providing thoughtful analysis and arguments.'
          }
        },
        {
          name: 'Critical Analyst',
          role: 'debater',
          position: 'Alternative perspective',
          llm_config: {
            provider: defaultModel.provider as 'claude' | 'openai' | 'gemini' | 'llama',
            model: defaultModel.id,
            temperature: DEFAULT_TEMPERATURE,
            systemPrompt: 'You provide critical analysis and alternative viewpoints.'
          }
        }
      ]);
    }
  }, [models, participants.length]);

  const handleSubmit = async () => {
    // Validation
    if (!name || !question.mainQuestion || !subject || participants.length < MIN_PARTICIPANTS) {
      alert('Please fill in all required fields');
      return;
    }

    setIsSubmitting(true);
    try {
      const debateData = {
        name,
        question,
        context,
        subject,
        expectedOutcome,
        participants,
        rules
      };
      
      await onSubmit(debateData);
      
      // Reset form
      resetForm();
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setName('');
    setSubject('');
    setExpectedOutcome('');
    setQuestion({
      mainQuestion: '',
      subQuestions: [],
      constraints: [],
      successCriteria: []
    });
    setContext({
      background: '',
      relevantFacts: [],
      assumptions: [],
      scope: '',
      outOfScope: []
    });
    setParticipants([]);
    setActiveTab('question');
  };

  const addParticipant = () => {
    const defaultModel = models[0] || { provider: 'unknown', id: 'unknown' };
    setParticipants([...participants, {
      name: `Participant ${participants.length + 1}`,
      role: 'debater',
      position: '',
      llm_config: {
        provider: defaultModel.provider as 'claude' | 'openai' | 'gemini' | 'llama',
        model: defaultModel.id,
        temperature: DEFAULT_TEMPERATURE
      }
    }]);
  };

  const updateParticipant = (index: number, updates: Partial<Participant>) => {
    const updated = [...participants];
    if (index >= 0 && index < updated.length) {
      updated[index] = { ...updated[index], ...updates };
      setParticipants(updated);
    }
  };

  const removeParticipant = (index: number) => {
    if (participants.length > MIN_PARTICIPANTS) {
      setParticipants(participants.filter((_, i) => i !== index));
    }
  };

  const addArrayItem = (
    setter: React.Dispatch<React.SetStateAction<any>>,
    field: string,
    value: string
  ) => {
    if (value.trim()) {
      setter((prev: any) => ({
        ...prev,
        [field]: [...(prev[field] || []), value.trim()]
      }));
    }
  };

  const removeArrayItem = (
    setter: React.Dispatch<React.SetStateAction<any>>,
    field: string,
    index: number
  ) => {
    setter((prev: any) => ({
      ...prev,
      [field]: prev[field].filter((_: any, i: number) => i !== index)
    }));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="text-2xl flex items-center gap-2">
            <Brain className="h-6 w-6 text-blue-600" />
            Create AI Debate
          </DialogTitle>
          <DialogDescription>
            Structure a comprehensive debate with clear questions, context, and multiple perspectives
          </DialogDescription>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 overflow-hidden">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="question">
              <MessageSquare className="h-4 w-4 mr-2" />
              Question
            </TabsTrigger>
            <TabsTrigger value="context">
              <BookOpen className="h-4 w-4 mr-2" />
              Context
            </TabsTrigger>
            <TabsTrigger value="participants">
              <Users className="h-4 w-4 mr-2" />
              Participants
            </TabsTrigger>
            <TabsTrigger value="settings">
              <Target className="h-4 w-4 mr-2" />
              Settings
            </TabsTrigger>
          </TabsList>

          <div className="mt-4 overflow-y-auto max-h-[calc(90vh-280px)]">
            {/* Question Tab */}
            <TabsContent value="question" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>Debate Question</CardTitle>
                  <CardDescription>
                    Define the main question and any sub-questions to explore
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="name">Debate Name *</Label>
                    <Input
                      id="name"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="e.g., AI Governance Framework Discussion"
                      className="mt-1"
                    />
                  </div>

                  <div>
                    <Label htmlFor="subject">Subject Area *</Label>
                    <Select value={subject} onValueChange={setSubject}>
                      <SelectTrigger className="mt-1">
                        <SelectValue placeholder="Select subject area" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="technology">Technology</SelectItem>
                        <SelectItem value="ethics">Ethics</SelectItem>
                        <SelectItem value="policy">Policy</SelectItem>
                        <SelectItem value="business">Business</SelectItem>
                        <SelectItem value="science">Science</SelectItem>
                        <SelectItem value="philosophy">Philosophy</SelectItem>
                        <SelectItem value="economics">Economics</SelectItem>
                        <SelectItem value="social">Social Issues</SelectItem>
                        <SelectItem value="environment">Environment</SelectItem>
                        <SelectItem value="other">Other</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div>
                    <Label htmlFor="mainQuestion">Main Question *</Label>
                    <textarea
                      id="mainQuestion"
                      value={question.mainQuestion}
                      onChange={(e) => setQuestion({ ...question, mainQuestion: e.target.value })}
                      placeholder="What is the primary question or problem to be debated?"
                      className="mt-1 w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>

                  <div>
                    <Label>Sub-Questions (Optional)</Label>
                    <div className="mt-1 space-y-2">
                      {question.subQuestions?.map((sq, index) => (
                        <div key={index} className="flex gap-2">
                          <Input value={sq} readOnly className="flex-1" />
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => removeArrayItem(setQuestion, 'subQuestions', index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                      <div className="flex gap-2">
                        <Input
                          placeholder="Add a sub-question..."
                          onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                              addArrayItem(setQuestion, 'subQuestions', e.currentTarget.value);
                              e.currentTarget.value = '';
                            }
                          }}
                        />
                        <Button size="sm" variant="outline">
                          <Plus className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="expectedOutcome">Expected Outcome</Label>
                    <textarea
                      id="expectedOutcome"
                      value={expectedOutcome}
                      onChange={(e) => setExpectedOutcome(e.target.value)}
                      placeholder="What resolution or conclusion are we seeking?"
                      className="mt-1 w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Context Tab */}
            <TabsContent value="context" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>Debate Context</CardTitle>
                  <CardDescription>
                    Provide background information and constraints
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label htmlFor="background">Background Information *</Label>
                    <textarea
                      id="background"
                      value={context.background}
                      onChange={(e) => setContext({ ...context, background: e.target.value })}
                      placeholder="Provide relevant background information..."
                      className="mt-1 w-full min-h-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>

                  <div>
                    <Label>Relevant Facts</Label>
                    <div className="mt-1 space-y-2">
                      {context.relevantFacts?.map((fact, index) => (
                        <div key={index} className="flex gap-2">
                          <Input value={fact} readOnly className="flex-1" />
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => removeArrayItem(setContext, 'relevantFacts', index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                      <Input
                        placeholder="Add a relevant fact..."
                        onKeyPress={(e) => {
                          if (e.key === 'Enter') {
                            addArrayItem(setContext, 'relevantFacts', e.currentTarget.value);
                            e.currentTarget.value = '';
                          }
                        }}
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="scope">Scope</Label>
                    <textarea
                      id="scope"
                      value={context.scope}
                      onChange={(e) => setContext({ ...context, scope: e.target.value })}
                      placeholder="What is within the scope of this debate?"
                      className="mt-1 w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                    />
                  </div>

                  <div>
                    <Label>Constraints</Label>
                    <div className="mt-1 space-y-2">
                      {question.constraints?.map((constraint, index) => (
                        <div key={index} className="flex gap-2">
                          <Input value={constraint} readOnly className="flex-1" />
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => removeArrayItem(setQuestion, 'constraints', index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                      <Input
                        placeholder="Add a constraint..."
                        onKeyPress={(e) => {
                          if (e.key === 'Enter') {
                            addArrayItem(setQuestion, 'constraints', e.currentTarget.value);
                            e.currentTarget.value = '';
                          }
                        }}
                      />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Participants Tab */}
            <TabsContent value="participants" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span>Debate Participants ({participants.length})</span>
                    <Button onClick={addParticipant} size="sm" variant="outline">
                      <Plus className="h-4 w-4 mr-1" />
                      Add Participant
                    </Button>
                  </CardTitle>
                  <CardDescription>
                    Add at least {MIN_PARTICIPANTS} participants with different perspectives
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {participants.map((participant, index) => (
                      <Card key={index} className="bg-muted/50">
                        <CardContent className="pt-4 space-y-3">
                          <div className="flex items-start justify-between">
                            <div className="flex-1 grid grid-cols-2 gap-3">
                              <div>
                                <Label>Name</Label>
                                <Input
                                  value={participant.name}
                                  onChange={(e) => updateParticipant(index, { name: e.target.value })}
                                  placeholder="Participant name"
                                  className="mt-1"
                                />
                              </div>
                              <div>
                                <Label>Position/Stance</Label>
                                <Input
                                  value={participant.position || ''}
                                  onChange={(e) => updateParticipant(index, { position: e.target.value })}
                                  placeholder="e.g., Pro-regulation"
                                  className="mt-1"
                                />
                              </div>
                            </div>
                            <Button
                              onClick={() => removeParticipant(index)}
                              size="sm"
                              variant="ghost"
                              className="ml-2 text-red-600 hover:text-red-700"
                              disabled={participants.length <= MIN_PARTICIPANTS}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>

                          <ModelSelector
                            value={participant.llm_config.model}
                            onChange={(modelId) => {
                              const model = models.find(m => m.id === modelId);
                              if (model) {
                                updateParticipant(index, {
                                  llm_config: {
                                    ...participant.llm_config,
                                    provider: model.provider as 'claude' | 'openai' | 'gemini' | 'llama',
                                    model: modelId
                                  }
                                });
                              }
                            }}
                            participantName={participant.name}
                            required
                          />

                          <div>
                            <Label>System Prompt</Label>
                            <textarea
                              value={participant.llm_config.systemPrompt || ''}
                              onChange={(e) => updateParticipant(index, {
                                llm_config: { ...participant.llm_config, systemPrompt: e.target.value }
                              })}
                              placeholder="Define the participant's expertise and approach..."
                              className="mt-1 w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                            />
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </TabsContent>

            {/* Settings Tab */}
            <TabsContent value="settings" className="space-y-4">
              <Card>
                <CardHeader>
                  <CardTitle>Debate Rules & Format</CardTitle>
                  <CardDescription>
                    Configure how the debate will be conducted
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div>
                    <Label>Debate Format</Label>
                    <Select
                      value={rules.format}
                      onValueChange={(value: any) => setRules({ ...rules, format: value })}
                    >
                      <SelectTrigger className="mt-1">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {DEBATE_FORMATS.map(format => (
                          <SelectItem key={format.value} value={format.value}>
                            <div>
                              <div className="font-medium">{format.label}</div>
                              <div className="text-xs text-muted-foreground">{format.description}</div>
                            </div>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label>Max Rounds</Label>
                      <Input
                        type="number"
                        value={rules.maxRounds}
                        onChange={(e) => setRules({ ...rules, maxRounds: parseInt(e.target.value) })}
                        min="1"
                        max="20"
                        className="mt-1"
                      />
                    </div>
                    <div>
                      <Label>Max Turn Length</Label>
                      <Input
                        type="number"
                        value={rules.maxTurnLength}
                        onChange={(e) => setRules({ ...rules, maxTurnLength: parseInt(e.target.value) })}
                        min="100"
                        max="2000"
                        className="mt-1"
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Success Criteria</Label>
                    <div className="mt-1 space-y-2">
                      {question.successCriteria?.map((criteria, index) => (
                        <div key={index} className="flex gap-2">
                          <Input value={criteria} readOnly className="flex-1" />
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => removeArrayItem(setQuestion, 'successCriteria', index)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                      <Input
                        placeholder="Add success criteria..."
                        onKeyPress={(e) => {
                          if (e.key === 'Enter') {
                            addArrayItem(setQuestion, 'successCriteria', e.currentTarget.value);
                            e.currentTarget.value = '';
                          }
                        }}
                      />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </TabsContent>
          </div>
        </Tabs>

        <DialogFooter className="border-t pt-4">
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              {health && health.status !== 'healthy' && (
                <Badge variant="outline" className="text-yellow-600">
                  <AlertCircle className="h-3 w-3 mr-1" />
                  LLM Service {health.status}
                </Badge>
              )}
            </div>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button 
                onClick={handleSubmit} 
                disabled={isSubmitting || !name || !question.mainQuestion || !subject || participants.length < MIN_PARTICIPANTS}
                className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white"
              >
                <Sparkles className="h-4 w-4 mr-2" />
                Create & Start Debate
              </Button>
            </div>
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}