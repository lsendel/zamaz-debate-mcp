'use client';

import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Participant, DebateRules } from '@/types/debate';
import { ModelSelector } from '@/components/llm/model-selector';
import { useLLM } from '@/hooks/use-llm';
import { Plus, Trash2, Users, Brain, Sparkles, MessageSquare, Cloud, Server, Zap, AlertCircle } from 'lucide-react';

interface CreateDebateDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (debate: any) => void;
}

export function CreateDebateDialog({ open, onOpenChange, onSubmit }: CreateDebateDialogProps) {
  const { models, health, loading: llmLoading } = useLLM();
  const [name, setName] = useState('');
  const [topic, setTopic] = useState('');
  const [description, setDescription] = useState('');
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [rules, setRules] = useState<DebateRules>({
    format: 'round_robin',
    maxRounds: 5,
    maxTurnLength: 500
  });

  // Initialize default participants when models are loaded
  useEffect(() => {
    if (models.length > 0 && participants.length === 0) {
      const defaultModel = models[0];
      setParticipants([
        {
          name: 'AI Optimist',
          role: 'debater',
          position: 'Pro-AI advancement',
          llm_config: {
            provider: defaultModel.provider,
            model: defaultModel.id,
            temperature: 0.7,
            systemPrompt: 'You are an optimistic AI advocate who believes in the positive potential of artificial intelligence.'
          }
        },
        {
          name: 'AI Skeptic',
          role: 'debater',
          position: 'Cautious about AI',
          llm_config: {
            provider: defaultModel.provider,
            model: defaultModel.id,
            temperature: 0.7,
            systemPrompt: 'You are a thoughtful AI skeptic who raises important concerns about artificial intelligence development.'
          }
        }
      ]);
    }
  }, [models, participants.length]);

  const handleSubmit = () => {
    if (!name || !topic || participants.length < 2) {
      alert('Please fill in all required fields and add at least 2 participants');
      return;
    }

    onSubmit({
      name,
      topic,
      description,
      participants,
      rules
    });

    // Reset form
    setName('');
    setTopic('');
    setDescription('');
    setParticipants([]);
  };

  const addParticipant = () => {
    const defaultModel = models[0] || { provider: 'unknown', id: 'unknown' };
    setParticipants([...participants, {
      name: `Participant ${participants.length + 1}`,
      role: 'debater',
      llm_config: {
        provider: defaultModel.provider,
        model: defaultModel.id,
        temperature: 0.7
      }
    }]);
  };

  const updateParticipant = (index: number, updates: Partial<Participant>) => {
    const updated = [...participants];
    updated[index] = { ...updated[index], ...updates };
    setParticipants(updated);
  };

  const removeParticipant = (index: number) => {
    setParticipants(participants.filter((_, i) => i !== index));
  };


  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="text-2xl flex items-center gap-2">
            <Brain className="h-6 w-6 text-blue-600" />
            Create New Debate
          </DialogTitle>
          <DialogDescription>
            Set up an AI-powered debate with multiple participants using various AI models
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Basic Info */}
          <div className="space-y-4">
            <div>
              <Label htmlFor="name">Debate Name</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g., AI Ethics Discussion"
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="topic">Topic</Label>
              <Input
                id="topic"
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="e.g., Should AI development be regulated?"
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="description">Description (Optional)</Label>
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide additional context for the debate..."
                className="mt-1 w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm"
              />
            </div>
          </div>

          {/* Debate Rules */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold flex items-center gap-2">
              <MessageSquare className="h-5 w-5" />
              Debate Rules
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label>Format</Label>
                <Select
                  value={rules.format}
                  onValueChange={(value: any) => setRules({ ...rules, format: value })}
                >
                  <SelectTrigger className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="round_robin">Round Robin</SelectItem>
                    <SelectItem value="free_form">Free Form</SelectItem>
                    <SelectItem value="oxford">Oxford Style</SelectItem>
                    <SelectItem value="panel">Panel Discussion</SelectItem>
                  </SelectContent>
                </Select>
              </div>

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
            </div>
          </div>

          {/* Participants */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold flex items-center gap-2">
                <Users className="h-5 w-5" />
                Participants ({participants.length})
              </h3>
              <Button onClick={addParticipant} size="sm" variant="outline">
                <Plus className="h-4 w-4 mr-1" />
                Add Participant
              </Button>
            </div>

            <div className="space-y-3">
              {participants.map((participant, index) => (
                <Card key={index} className="p-4 bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800">
                  <div className="space-y-3">
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
                          <Label>Position</Label>
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
                              provider: model.provider,
                              model: modelId
                            }
                          });
                        }
                      }}
                      participantName={participant.name}
                      required
                    />

                    <div className="grid grid-cols-2 gap-3">
                      <div>
                        <Label>Temperature</Label>
                        <Input
                          type="number"
                          value={participant.llm_config.temperature}
                          onChange={(e) => updateParticipant(index, {
                            llm_config: { ...participant.llm_config, temperature: parseFloat(e.target.value) }
                          })}
                          min="0"
                          max="2"
                          step="0.1"
                          className="mt-1"
                        />
                      </div>
                      <div>
                        <Label>Role</Label>
                        <Select
                          value={participant.role}
                          onValueChange={(value: any) => updateParticipant(index, { role: value })}
                        >
                          <SelectTrigger className="mt-1">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="debater">Debater</SelectItem>
                            <SelectItem value="moderator">Moderator</SelectItem>
                            <SelectItem value="judge">Judge</SelectItem>
                            <SelectItem value="observer">Observer</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </div>

                    <div>
                      <Label>System Prompt (Optional)</Label>
                      <textarea
                        value={participant.llm_config.systemPrompt || ''}
                        onChange={(e) => updateParticipant(index, {
                          llm_config: { ...participant.llm_config, systemPrompt: e.target.value }
                        })}
                        placeholder="Define the participant's personality and debate style..."
                        className="mt-1 w-full min-h-[60px] rounded-md border border-input bg-background px-3 py-2 text-sm"
                      />
                    </div>

                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      <Badge variant="outline">
                        {participant.llm_config.provider}
                      </Badge>
                      <span>•</span>
                      <span>{models.find(m => m.id === participant.llm_config.model)?.name || participant.llm_config.model}</span>
                      <span>•</span>
                      <span>Temperature: {participant.llm_config.temperature}</span>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          </div>

          {/* LLM Service Status */}
          {health && health.status !== 'healthy' && (
            <div className="flex items-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-400 rounded-md">
              <AlertCircle className="h-4 w-4" />
              <span className="text-sm">
                LLM service is {health.status}. Some models may not be available.
              </span>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white">
            <Sparkles className="h-4 w-4 mr-2" />
            Create Debate
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}