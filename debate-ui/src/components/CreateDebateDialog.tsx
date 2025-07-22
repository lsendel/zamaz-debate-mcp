import React, { useState, useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../store';
import {
  closeCreateDebateDialog,
  addNotification,
} from '../store/slices/uiSlice';
import { createDebate } from '../store/slices/debateSlice';
import llmClient, { LLMProvider } from '../api/llmClient';
import { Modal, Button, Input, Select, Card, Form, Slider, InputNumber, Typography, Space, Row, Col } from 'antd';
import {  PlusOutlined, DeleteOutlined } from '@ant-design/icons';

const { TextArea } = Input;
const { Title, Text } = Typography;

// Custom FormField component
const FormField: React.FC<{ label: string; required?: boolean; children: React.ReactNode }> = ({ 
  label, 
  required, 
  children 
}) => (
  <Form.Item 
    label={label} 
    required={required}
    style={{ marginBottom: '16px' }}
  >
    {children}
  </Form.Item>
);

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
    <Modal
      open={createDebateDialogOpen}
      onCancel={handleClose}
      width={900}
      title={(
        <div>
          <Title level={4} style={{ margin: 0 }}>Create New Debate</Title>
          <Text type="secondary">Set up a new AI debate with multiple participants</Text>
        </div>
      )}
      footer={[
        <Button key="cancel" onClick={handleClose}>
          Cancel
        </Button>,
        <Button
          key="submit"
          type="primary"
          onClick={handleSubmit}
          loading={loading}
          disabled={loading || !topic || participants.length < 2}
        >
          Create Debate
        </Button>,
      ]}
      style={{ top: 20 }}
      bodyStyle={{ maxHeight: '70vh', overflowY: 'auto' }}
    >

      <Form layout="vertical">
        <FormField label="Topic" required>
          <Input
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            placeholder="Enter the debate topic"
          />
        </FormField>

        <FormField label="Description">
          <TextArea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Provide additional context for the debate"
            rows={3}
          />
        </FormField>

        <Row gutter={16}>
          <Col span={12}>
            <FormField label="Max Rounds">
              <InputNumber
                value={maxRounds}
                onChange={(value) => setMaxRounds(value || 5)}
                min={1}
                max={20}
                style={{ width: '100%' }}
              />
            </FormField>
          </Col>
          <Col span={12}>
            <FormField label="Turn Time Limit (seconds)">
              <InputNumber
                value={turnTimeLimit}
                onChange={(value) => setTurnTimeLimit(value || 60)}
                min={10}
                max={300}
                style={{ width: '100%' }}
              />
            </FormField>
          </Col>
        </Row>

        <div style={{ marginTop: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <Title level={5} style={{ margin: 0 }}>Participants</Title>
            <Button
              onClick={addParticipant}
              icon={<PlusOutlined />}
              size="small"
            >
              Add Participant
            </Button>
          </div>

          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {participants.map((participant, index) => (
              <Card 
                key={index}
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Input
                      value={participant.name}
                      onChange={(e) =>
                        updateParticipant(index, { name: e.target.value })
                      }
                      style={{ fontWeight: 600, border: 'none', paddingLeft: 0 }}
                    />
                    {participants.length > 2 && (
                      <Button
                        danger
                        type="text"
                        icon={<DeleteOutlined />}
                        onClick={() => removeParticipant(index)}
                        size="small"
                      />
                    )}
                  </div>
                }
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <FormField label="Provider">
                      <Select
                        value={participant.llmProvider}
                        onChange={(value) =>
                          updateParticipant(index, { llmProvider: value })
                        }
                        placeholder="Select provider"
                        style={{ width: '100%' }}
                      >
                        {providers.map((provider) => (
                          <Select.Option key={provider.id} value={provider.id}>
                            {provider.name}
                          </Select.Option>
                        ))}
                      </Select>
                    </FormField>
                  </Col>
                  <Col span={12}>
                    <FormField label="Model">
                      <Select
                        value={participant.model}
                        onChange={(value) =>
                          updateParticipant(index, { model: value })
                        }
                        placeholder="Select model"
                        style={{ width: '100%' }}
                      >
                        {getModelsForProvider(participant.llmProvider).map(
                          (model) => (
                            <Select.Option key={model.id} value={model.id}>
                              {model.name}
                            </Select.Option>
                          )
                        )}
                      </Select>
                    </FormField>
                  </Col>
                </Row>

                <FormField label="System Prompt">
                  <TextArea
                    value={participant.systemPrompt}
                    onChange={(e) =>
                      updateParticipant(index, {
                        systemPrompt: e.target.value,
                      })
                    }
                    rows={3}
                  />
                </FormField>

                <Row gutter={16}>
                  <Col span={12}>
                    <FormField label={`Temperature: ${participant.temperature}`}>
                      <Slider
                        min={0}
                        max={2}
                        step={0.1}
                        value={participant.temperature}
                        onChange={(value) =>
                          updateParticipant(index, {
                            temperature: value,
                          })
                        }
                      />
                    </FormField>
                  </Col>
                  <Col span={12}>
                    <FormField label="Max Tokens">
                      <InputNumber
                        value={participant.maxTokens}
                        onChange={(value) =>
                          updateParticipant(index, {
                            maxTokens: value || 1000,
                          })
                        }
                        min={100}
                        max={4000}
                        style={{ width: '100%' }}
                      />
                    </FormField>
                  </Col>
                </Row>
              </Card>
            ))}
          </Space>
        </div>
      </Form>

    </Modal>
  );
};

export default CreateDebateDialog;