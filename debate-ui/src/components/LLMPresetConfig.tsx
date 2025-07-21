import React, { useState, useEffect } from 'react';
import {
  Card,
  Form,
  Select,
  Input,
  InputNumber,
  Slider,
  Button,
  Switch,
  Space,
  Divider,
  Tag,
  Alert,
  Tooltip,
  Typography,
  Row,
  Col,
  notification,
  Collapse,
} from 'antd';
import {
  SettingOutlined,
  SaveOutlined,
  CopyOutlined,
  DeleteOutlined,
  PlusOutlined,
  InfoCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';

const { Option } = Select;
const { TextArea } = Input;
const { Title, Text } = Typography;
const { Panel } = Collapse;

interface LLMProvider {
  id: string;
  name: string;
  models: string[];
  defaultParams: Record<string, any>;
}

interface LLMPreset {
  id?: string;
  name: string;
  description?: string;
  provider: string;
  model: string;
  parameters: {
    temperature: number;
    maxTokens: number;
    topP?: number;
    topK?: number;
    frequencyPenalty?: number;
    presencePenalty?: number;
    stopSequences?: string[];
  };
  systemPrompt: string;
  isDefault?: boolean;
  isActive: boolean;
}

interface LLMPresetConfigProps {
  debateId?: string;
  participantId?: string;
  onSave?: (preset: LLMPreset) => void;
  onLoad?: (preset: LLMPreset) => void;
}

const LLMPresetConfig: React.FC<LLMPresetConfigProps> = ({
  debateId,
  participantId,
  onSave,
  onLoad,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [providers, setProviders] = useState<LLMProvider[]>([]);
  const [presets, setPresets] = useState<LLMPreset[]>([]);
  const [selectedProvider, setSelectedProvider] = useState<string>('');
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [currentPreset, setCurrentPreset] = useState<LLMPreset | null>(null);

  // Default LLM providers and models
  const defaultProviders: LLMProvider[] = [
    {
      id: 'CLAUDE',
      name: 'Anthropic Claude',
      models: ['claude-3-5-sonnet-20241022', 'claude-3-haiku-20240307', 'claude-3-opus-20240229'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 4096,
        topP: 1.0,
      },
    },
    {
      id: 'OPENAI',
      name: 'OpenAI GPT',
      models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 4096,
        topP: 1.0,
        frequencyPenalty: 0,
        presencePenalty: 0,
      },
    },
    {
      id: 'GEMINI',
      name: 'Google Gemini',
      models: ['gemini-1.5-pro', 'gemini-1.5-flash', 'gemini-pro'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 8192,
        topP: 0.95,
        topK: 40,
      },
    },
    {
      id: 'LLAMA',
      name: 'Meta Llama',
      models: ['llama-3.1-70b-instruct', 'llama-3.1-8b-instruct', 'llama-2-70b-chat'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 4096,
        topP: 0.9,
      },
    },
    {
      id: 'COHERE',
      name: 'Cohere Command',
      models: ['command-r-plus', 'command-r', 'command'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 4096,
        topP: 0.75,
        topK: 0,
      },
    },
    {
      id: 'MISTRAL',
      name: 'Mistral AI',
      models: ['mistral-large-2407', 'mistral-medium', 'mistral-small'],
      defaultParams: {
        temperature: 0.7,
        maxTokens: 4096,
        topP: 1.0,
      },
    },
  ];

  useEffect(() => {
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    setLoading(true);
    try {
      // Use default providers for now
      setProviders(defaultProviders);
      
      // Load existing presets if they exist
      // This would typically come from the backend
      const samplePresets: LLMPreset[] = [
        {
          id: 'preset-1',
          name: 'Balanced Debater',
          description: 'Balanced configuration for structured debates',
          provider: 'CLAUDE',
          model: 'claude-3-5-sonnet-20241022',
          parameters: {
            temperature: 0.7,
            maxTokens: 2048,
            topP: 0.9,
          },
          systemPrompt: 'You are a skilled debater participating in a structured debate. Present clear, logical arguments while being respectful to opposing viewpoints.',
          isActive: true,
        },
        {
          id: 'preset-2',
          name: 'Conservative Arguer',
          description: 'Low temperature for consistent, logical arguments',
          provider: 'OPENAI',
          model: 'gpt-4o',
          parameters: {
            temperature: 0.3,
            maxTokens: 1500,
            topP: 0.8,
            frequencyPenalty: 0.1,
          },
          systemPrompt: 'You are a conservative debater who values logical consistency and factual accuracy above all else.',
          isActive: true,
        },
        {
          id: 'preset-3',
          name: 'Creative Challenger',
          description: 'Higher temperature for creative and diverse arguments',
          provider: 'GEMINI',
          model: 'gemini-1.5-pro',
          parameters: {
            temperature: 1.1,
            maxTokens: 3000,
            topP: 0.95,
            topK: 40,
          },
          systemPrompt: 'You are a creative debater who brings innovative perspectives and challenges conventional thinking.',
          isActive: true,
        },
      ];
      
      setPresets(samplePresets);
    } catch (error) {
      console.error('Failed to load initial data:', error);
      notification.error({
        message: 'Failed to load LLM configuration data',
        description: 'Please check your connection and try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleProviderChange = (providerId: string) => {
    setSelectedProvider(providerId);
    setSelectedModel('');
    
    const provider = providers.find(p => p.id === providerId);
    if (provider) {
      // Set default parameters for the provider
      const defaultParams = provider.defaultParams;
      form.setFieldsValue({
        ...defaultParams,
        model: '',
      });
    }
  };

  const handleModelChange = (model: string) => {
    setSelectedModel(model);
    form.setFieldsValue({ model });
  };

  const handlePresetLoad = (preset: LLMPreset) => {
    setCurrentPreset(preset);
    setSelectedProvider(preset.provider);
    setSelectedModel(preset.model);
    
    form.setFieldsValue({
      name: preset.name,
      description: preset.description,
      provider: preset.provider,
      model: preset.model,
      systemPrompt: preset.systemPrompt,
      isActive: preset.isActive,
      ...preset.parameters,
    });
    
    onLoad?.(preset);
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      const values = await form.validateFields();
      
      const preset: LLMPreset = {
        id: currentPreset?.id,
        name: values.name,
        description: values.description,
        provider: values.provider,
        model: values.model,
        parameters: {
          temperature: values.temperature,
          maxTokens: values.maxTokens,
          topP: values.topP,
          topK: values.topK,
          frequencyPenalty: values.frequencyPenalty,
          presencePenalty: values.presencePenalty,
          stopSequences: values.stopSequences?.split('\n').filter(Boolean),
        },
        systemPrompt: values.systemPrompt,
        isActive: values.isActive ?? true,
      };

      // Save the preset (this would typically go to the backend)
      if (currentPreset?.id) {
        // Update existing preset
        const updatedPresets = presets.map(p => 
          p.id === currentPreset.id ? preset : p
        );
        setPresets(updatedPresets);
      } else {
        // Create new preset
        preset.id = `preset-${Date.now()}`;
        setPresets([...presets, preset]);
      }

      notification.success({
        message: 'LLM preset saved successfully',
        duration: 3,
      });

      onSave?.(preset);
      setCurrentPreset(preset);
    } catch (error) {
      console.error('Failed to save preset:', error);
      notification.error({
        message: 'Failed to save preset',
        description: 'Please check your inputs and try again.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleDeletePreset = (presetId: string) => {
    const updatedPresets = presets.filter(p => p.id !== presetId);
    setPresets(updatedPresets);
    
    if (currentPreset?.id === presetId) {
      setCurrentPreset(null);
      form.resetFields();
    }
    
    notification.success({
      message: 'Preset deleted successfully',
      duration: 2,
    });
  };

  const selectedProviderData = providers.find(p => p.id === selectedProvider);

  return (
    <div style={{ display: 'flex', gap: '16px', flexDirection: 'column' }}>
      {/* Preset Selection */}
      <Card 
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ThunderboltOutlined />
            LLM Presets
          </div>
        }
        size="small"
      >
        <Row gutter={[8, 8]}>
          {presets.map(preset => (
            <Col key={preset.id} span={8}>
              <Card
                size="small"
                hoverable
                onClick={() => handlePresetLoad(preset)}
                style={{
                  cursor: 'pointer',
                  border: currentPreset?.id === preset.id ? '2px solid #1677ff' : '1px solid #d9d9d9',
                }}
                bodyStyle={{ padding: '12px' }}
                actions={[
                  <CopyOutlined 
                    key="copy" 
                    onClick={(e) => {
                      e.stopPropagation();
                      const newPreset = { ...preset, id: undefined, name: `${preset.name} (Copy)` };
                      handlePresetLoad(newPreset);
                    }}
                  />,
                  <DeleteOutlined 
                    key="delete" 
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeletePreset(preset.id!);
                    }}
                    style={{ color: '#ff4d4f' }}
                  />,
                ]}
              >
                <div>
                  <Text strong style={{ fontSize: '12px' }}>{preset.name}</Text>
                  <br />
                  <Text type="secondary" style={{ fontSize: '11px' }}>
                    {preset.provider} - {preset.model}
                  </Text>
                  <br />
                  <Tag size="small" color={preset.isActive ? 'green' : 'red'}>
                    {preset.isActive ? 'Active' : 'Inactive'}
                  </Tag>
                </div>
              </Card>
            </Col>
          ))}
          <Col span={8}>
            <Card
              size="small"
              hoverable
              style={{
                cursor: 'pointer',
                border: '1px dashed #d9d9d9',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '100px',
              }}
              bodyStyle={{ 
                padding: '12px', 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center',
                justifyContent: 'center',
              }}
              onClick={() => {
                setCurrentPreset(null);
                form.resetFields();
              }}
            >
              <PlusOutlined style={{ fontSize: '20px', color: '#999' }} />
              <Text type="secondary" style={{ fontSize: '12px', marginTop: '4px' }}>
                New Preset
              </Text>
            </Card>
          </Col>
        </Row>
      </Card>

      {/* Configuration Form */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <SettingOutlined />
            LLM Configuration
          </div>
        }
        extra={
          <Space>
            <Button onClick={() => form.resetFields()}>Reset</Button>
            <Button 
              type="primary" 
              icon={<SaveOutlined />}
              onClick={handleSave}
              loading={saving}
            >
              Save Preset
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="name"
                label="Preset Name"
                rules={[{ required: true, message: 'Please enter a preset name' }]}
              >
                <Input placeholder="Enter preset name" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="description" label="Description">
                <Input placeholder="Enter description (optional)" />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="provider"
                label="Provider"
                rules={[{ required: true, message: 'Please select a provider' }]}
              >
                <Select
                  placeholder="Select LLM provider"
                  onChange={handleProviderChange}
                  value={selectedProvider}
                >
                  {providers.map(provider => (
                    <Option key={provider.id} value={provider.id}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <ThunderboltOutlined />
                        {provider.name}
                      </div>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="model"
                label="Model"
                rules={[{ required: true, message: 'Please select a model' }]}
              >
                <Select
                  placeholder="Select model"
                  onChange={handleModelChange}
                  value={selectedModel}
                  disabled={!selectedProvider}
                >
                  {selectedProviderData?.models.map(model => (
                    <Option key={model} value={model}>
                      {model}
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item
            name="systemPrompt"
            label={
              <span>
                System Prompt
                <Tooltip title="The system instruction that guides the AI's behavior in the debate">
                  <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                </Tooltip>
              </span>
            }
            rules={[{ required: true, message: 'Please enter a system prompt' }]}
          >
            <TextArea
              rows={4}
              placeholder="Enter the system prompt that will guide the AI's behavior..."
            />
          </Form.Item>

          <Collapse>
            <Panel header="Advanced Parameters" key="parameters">
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="temperature"
                    label={
                      <span>
                        Temperature
                        <Tooltip title="Controls randomness. Lower = more focused, Higher = more creative">
                          <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
                        </Tooltip>
                      </span>
                    }
                  >
                    <Slider
                      min={0}
                      max={2}
                      step={0.1}
                      marks={{ 0: '0', 0.7: '0.7', 1: '1', 2: '2' }}
                    />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item
                    name="maxTokens"
                    label="Max Tokens"
                  >
                    <InputNumber
                      min={1}
                      max={8192}
                      style={{ width: '100%' }}
                      placeholder="Maximum response length"
                    />
                  </Form.Item>
                </Col>
              </Row>

              {selectedProvider === 'OPENAI' && (
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item name="frequencyPenalty" label="Frequency Penalty">
                      <Slider min={-2} max={2} step={0.1} marks={{ 0: '0', 1: '1' }} />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="presencePenalty" label="Presence Penalty">
                      <Slider min={-2} max={2} step={0.1} marks={{ 0: '0', 1: '1' }} />
                    </Form.Item>
                  </Col>
                </Row>
              )}

              {(selectedProvider === 'GEMINI' || selectedProvider === 'COHERE') && (
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item name="topP" label="Top P">
                      <Slider min={0} max={1} step={0.05} marks={{ 0: '0', 0.5: '0.5', 1: '1' }} />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item name="topK" label="Top K">
                      <InputNumber min={0} max={100} style={{ width: '100%' }} />
                    </Form.Item>
                  </Col>
                </Row>
              )}

              <Form.Item name="stopSequences" label="Stop Sequences (one per line)">
                <TextArea rows={3} placeholder="Enter stop sequences, one per line" />
              </Form.Item>

              <Form.Item name="isActive" label="Active" valuePropName="checked">
                <Switch />
              </Form.Item>
            </Panel>
          </Collapse>
        </Form>
      </Card>

      {currentPreset && (
        <Alert
          message="Preset Loaded"
          description={`Using preset: ${currentPreset.name} (${currentPreset.provider} - ${currentPreset.model})`}
          type="info"
          showIcon
          closable
        />
      )}
    </div>
  );
};

export default LLMPresetConfig;