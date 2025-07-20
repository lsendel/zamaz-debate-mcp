import React, { useState, useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../store';
import {
  closeCreateDebateDialog,
  addNotification,
} from '../store/slices/uiSlice';
import { createDebate } from '../store/slices/debateSlice';
import llmClient, { LLMProvider, LLMModel } from '../api/llmClient';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Button,
  Input,
  Textarea,
  FormField,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Badge,
} from '@zamaz/ui';
import { X, Plus, Trash2, Sliders } from 'lucide-react';

interface Participant {
  name: string;
  llmProvider: string;
  model: string;
  systemPrompt: string;
  temperature: number;
  maxTokens: number;
}

const CreateDebateDialog: React.FC = () => {
  const dispatch = useAppDispatch();
  const { createDebateDialogOpen } = useAppSelector((state) => state.ui);
  const [providers, setProviders] = useState<LLMProvider[]>([]);
  const [loading, setLoading] = useState(false);

  const [topic, setTopic] = useState('');
  const [description, setDescription] = useState('');
  const [maxRounds, setMaxRounds] = useState(5);
  const [turnTimeLimit, setTurnTimeLimit] = useState(60);
  const [participants, setParticipants] = useState<Participant[]>([
    {
      name: 'Participant 1',
      llmProvider: '',
      model: '',
      systemPrompt:
        'You are a thoughtful debater who provides well-reasoned arguments.',
      temperature: 0.7,
      maxTokens: 1000,
    },
    {
      name: 'Participant 2',
      llmProvider: '',
      model: '',
      systemPrompt:
        'You are a critical thinker who challenges assumptions and provides counterarguments.',
      temperature: 0.7,
      maxTokens: 1000,
    },
  ]);

  useEffect(() => {
    const loadProviders = async () => {
      try {
        const providerList = await llmClient.listProviders();
        setProviders(providerList);

        // Set default providers and models after loading
        if (providerList.length > 0) {
          const updatedParticipants = participants.map((participant, index) => {
            if (!participant.llmProvider) {
              const defaultProvider = providerList[index % providerList.length];
              const defaultModel = defaultProvider.models[0];
              return {
                ...participant,
                llmProvider: defaultProvider.id,
                model: defaultModel.id,
              };
            }
            return participant;
          });
          setParticipants(updatedParticipants);
        }
      } catch (error) {
        console.error('Failed to load providers:', error);
      }
    };

    if (createDebateDialogOpen) {
      loadProviders();
    }
  }, [createDebateDialogOpen]);

  const handleClose = () => {
    dispatch(closeCreateDebateDialog());
    // Reset form
    setTopic('');
    setDescription('');
    setMaxRounds(5);
    setTurnTimeLimit(60);
    setParticipants([
      {
        name: 'Participant 1',
        llmProvider: '',
        model: '',
        systemPrompt:
          'You are a thoughtful debater who provides well-reasoned arguments.',
        temperature: 0.7,
        maxTokens: 1000,
      },
      {
        name: 'Participant 2',
        llmProvider: '',
        model: '',
        systemPrompt:
          'You are a critical thinker who challenges assumptions and provides counterarguments.',
        temperature: 0.7,
        maxTokens: 1000,
      },
    ]);
  };

  const handleSubmit = async () => {
    if (!topic || participants.length < 2) {
      dispatch(
        addNotification({
          type: 'error',
          message: 'Please provide a topic and at least 2 participants',
        })
      );
      return;
    }

    setLoading(true);
    try {
      const resultAction = await dispatch(
        createDebate({
          topic,
          description,
          maxRounds,
          turnTimeLimit,
          participants: participants.map((p) => ({
            name: p.name,
            llmConfig: {
              provider: p.llmProvider,
              model: p.model,
              temperature: p.temperature,
              maxTokens: p.maxTokens,
              systemPrompt: p.systemPrompt,
            },
          })),
        })
      );

      if (createDebate.fulfilled.match(resultAction)) {
        handleClose();
        dispatch(
          addNotification({
            type: 'success',
            message: 'Debate created successfully!',
          })
        );
      }
    } catch (error) {
      dispatch(
        addNotification({
          type: 'error',
          message: 'Failed to create debate',
        })
      );
    } finally {
      setLoading(false);
    }
  };

  const addParticipant = () => {
    const defaultProvider = providers[0];
    const defaultModel = defaultProvider?.models[0];
    
    setParticipants([
      ...participants,
      {
        name: `Participant ${participants.length + 1}`,
        llmProvider: defaultProvider?.id || '',
        model: defaultModel?.id || '',
        systemPrompt: 'You are a debate participant with unique perspectives.',
        temperature: 0.7,
        maxTokens: 1000,
      },
    ]);
  };

  const removeParticipant = (index: number) => {
    setParticipants(participants.filter((_, i) => i !== index));
  };

  const updateParticipant = (index: number, updates: Partial<Participant>) => {
    setParticipants(
      participants.map((p, i) => (i === index ? { ...p, ...updates } : p))
    );
  };

  const getModelsForProvider = (providerId: string) => {
    const provider = providers.find((p) => p.id === providerId);
    return provider?.models || [];
  };

  return (
    <Dialog open={createDebateDialogOpen} onOpenChange={handleClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create New Debate</DialogTitle>
          <DialogDescription>
            Set up a new AI debate with multiple participants
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6 py-4">
          <FormField label="Topic" required>
            <Input
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              placeholder="Enter the debate topic"
              fullWidth
            />
          </FormField>

          <FormField label="Description">
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Provide additional context for the debate"
              rows={3}
            />
          </FormField>

          <div className="grid grid-cols-2 gap-4">
            <FormField label="Max Rounds">
              <Input
                type="number"
                value={maxRounds}
                onChange={(e) => setMaxRounds(parseInt(e.target.value) || 5)}
                min={1}
                max={20}
              />
            </FormField>

            <FormField label="Turn Time Limit (seconds)">
              <Input
                type="number"
                value={turnTimeLimit}
                onChange={(e) => setTurnTimeLimit(parseInt(e.target.value) || 60)}
                min={10}
                max={300}
              />
            </FormField>
          </div>

          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-lg font-semibold">Participants</h3>
              <Button
                variant="secondary"
                size="sm"
                onClick={addParticipant}
                leftIcon={<Plus className="h-4 w-4" />}
              >
                Add Participant
              </Button>
            </div>

            {participants.map((participant, index) => (
              <Card key={index}>
                <CardHeader>
                  <div className="flex justify-between items-center">
                    <CardTitle className="text-base">
                      <Input
                        value={participant.name}
                        onChange={(e) =>
                          updateParticipant(index, { name: e.target.value })
                        }
                        className="font-semibold"
                      />
                    </CardTitle>
                    {participants.length > 2 && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => removeParticipant(index)}
                        className="text-red-600 hover:text-red-700"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <FormField label="Provider">
                      <Select
                        value={participant.llmProvider}
                        onValueChange={(value) =>
                          updateParticipant(index, { llmProvider: value })
                        }
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select provider" />
                        </SelectTrigger>
                        <SelectContent>
                          {providers.map((provider) => (
                            <SelectItem key={provider.id} value={provider.id}>
                              {provider.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </FormField>

                    <FormField label="Model">
                      <Select
                        value={participant.model}
                        onValueChange={(value) =>
                          updateParticipant(index, { model: value })
                        }
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Select model" />
                        </SelectTrigger>
                        <SelectContent>
                          {getModelsForProvider(participant.llmProvider).map(
                            (model) => (
                              <SelectItem key={model.id} value={model.id}>
                                {model.name}
                              </SelectItem>
                            )
                          )}
                        </SelectContent>
                      </Select>
                    </FormField>
                  </div>

                  <FormField label="System Prompt">
                    <Textarea
                      value={participant.systemPrompt}
                      onChange={(e) =>
                        updateParticipant(index, {
                          systemPrompt: e.target.value,
                        })
                      }
                      rows={3}
                    />
                  </FormField>

                  <div className="grid grid-cols-2 gap-4">
                    <FormField label={`Temperature: ${participant.temperature}`}>
                      <input
                        type="range"
                        min="0"
                        max="2"
                        step="0.1"
                        value={participant.temperature}
                        onChange={(e) =>
                          updateParticipant(index, {
                            temperature: parseFloat(e.target.value),
                          })
                        }
                        className="w-full"
                      />
                    </FormField>

                    <FormField label="Max Tokens">
                      <Input
                        type="number"
                        value={participant.maxTokens}
                        onChange={(e) =>
                          updateParticipant(index, {
                            maxTokens: parseInt(e.target.value) || 1000,
                          })
                        }
                        min={100}
                        max={4000}
                      />
                    </FormField>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            onClick={handleSubmit}
            loading={loading}
            disabled={loading || !topic || participants.length < 2}
          >
            Create Debate
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default CreateDebateDialog;