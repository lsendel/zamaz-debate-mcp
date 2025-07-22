import React, { useState, useEffect } from 'react';
import {
  Card,
  Typography,
  Switch,
  Select,
  Input,
  InputNumber,
  Slider,
  Button,
  
  Divider,
  Collapse,
  
  Tooltip,
  
  
  Form,
  Alert,
  Spin,
} from 'antd';
import {
  InfoCircleOutlined,
  SaveOutlined,
  
  
  SettingOutlined,
} from '@ant-design/icons';
import debateClient from '../api/debateClient';
import { useAppDispatch } from '../store';
import { addNotification } from '../store/slices/uiSlice';

const { Title, Text, Paragraph } = Typography;
const { Panel } = Collapse;
const { TextArea } = Input;

// Types
export interface AgenticFlowType {
  value: string;
  label: string;
  description: string;
  category: string;
  icon?: React.ReactNode;
}

export interface AgenticFlowConfiguration {
  flowId?: string;
  flowType: string;
  enabled: boolean;
  parameters: Record<string, any>;
}

interface AgenticFlowConfigProps {
  debateId: string;
  participantId?: string;
  onSave?: (configuration: AgenticFlowConfiguration) => void;
}

// Flow type definitions with descriptions
const FLOW_TYPES: AgenticFlowType[] = [
  {
    value: 'INTERNAL_MONOLOGUE',
    label: 'Internal Monologue',
    description: 'Instructs the AI to "think out loud" by writing out reasoning step-by-step before providing the final answer.',
    category: 'reasoning',
  },
  {
    value: 'SELF_CRITIQUE_LOOP',
    label: 'Self-Critique Loop',
    description: 'AI generates an answer, critiques its own output for flaws, and then revises it based on that critique.',
    category: 'reasoning',
  },
  {
    value: 'MULTI_AGENT_RED_TEAM',
    label: 'Multi-Agent Red Team',
    description: 'Simulates a debate between different personas (Architect, Skeptic, Judge) within a single AI to challenge and defend an answer.',
    category: 'reasoning',
  },
  {
    value: 'TOOL_CALLING_VERIFICATION',
    label: 'Tool-Calling Verification',
    description: 'Empowers the AI to use external tools to verify facts and retrieve up-to-date information.',
    category: 'verification',
  },
  {
    value: 'RAG_WITH_RERANKING',
    label: 'RAG with Re-ranking',
    description: 'Enhanced RAG that first retrieves many documents, then re-ranks them for relevance before generating an answer.',
    category: 'retrieval',
  },
  {
    value: 'CONFIDENCE_SCORING',
    label: 'Confidence Scoring',
    description: 'AI provides confidence scores with responses and automatically improves low-confidence answers.',
    category: 'validation',
  },
  {
    value: 'CONSTITUTIONAL_PROMPTING',
    label: 'Constitutional Prompting',
    description: 'Applies a set of inviolable rules or principles to guide and constrain AI behavior.',
    category: 'constraints',
  },
  {
    value: 'ENSEMBLE_VOTING',
    label: 'Ensemble Voting',
    description: 'Generates multiple responses to the same prompt and selects the most consistent answer.',
    category: 'validation',
  },
  {
    value: 'POST_PROCESSING_RULES',
    label: 'Post-processing Rules',
    description: 'Applies deterministic checks and formatting rules to validate and correct AI outputs.',
    category: 'validation',
  },
  {
    value: 'TREE_OF_THOUGHTS',
    label: 'Tree of Thoughts',
    description: 'Explores multiple reasoning paths simultaneously like a decision tree.',
    category: 'advanced',
  },
  {
    value: 'STEP_BACK_PROMPTING',
    label: 'Step-Back Prompting',
    description: 'Prompts the model to generalize from a specific question to underlying principles before answering.',
    category: 'advanced',
  },
  {
    value: 'PROMPT_CHAINING',
    label: 'Prompt Chaining',
    description: 'Decomposes complex tasks into a sequence of smaller, interconnected prompts.',
    category: 'advanced',
  },
];

// Helper to get flow type by value
const getFlowType = (value: string): AgenticFlowType | undefined => {
  return FLOW_TYPES.find(type => type.value === value);
};

// Custom form field component
const FormField: React.FC<{ label: string; tooltip?: string; children: React.ReactNode }> = ({ 
  label, 
  tooltip,
  children 
}) => (
  <Form.Item 
    label={
      <span>
        {label}
        {tooltip && (
          <Tooltip title={tooltip}>
            <InfoCircleOutlined style={{ marginLeft: 4, color: '#8c8c8c' }} />
          </Tooltip>
        )}
      </span>
    }
    style={{ marginBottom: '16px' }}
  >
    {children}
  </Form.Item>
);

const AgenticFlowConfig: React.FC<AgenticFlowConfigProps> = ({
  debateId,
  participantId,
  onSave,
}) => {
  const dispatch = useAppDispatch();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [currentConfig, setCurrentConfig] = useState<AgenticFlowConfiguration | null>(null);
  const [selectedFlowType, setSelectedFlowType] = useState<string>('');
  const [flowEnabled, setFlowEnabled] = useState(false);
  const [flowParameters, setFlowParameters] = useState<Record<string, any>>({});

  // Load existing configuration
  useEffect(() => {
    const loadConfiguration = async () => {
      setLoading(true);
      try {
        let config;
        if (participantId) {
          config = await debateClient.getParticipantAgenticFlow(debateId, participantId);
        } else {
          config = await debateClient.getDebateAgenticFlow(debateId);
        }
        
        if (config) {
          setCurrentConfig(config);
          setSelectedFlowType(config.flowType);
          setFlowEnabled(config.enabled);
          setFlowParameters(config.parameters || {});
        }
      } catch (error) {
        console.error('Failed to load agentic flow configuration:', error);
      } finally {
        setLoading(false);
      }
    };

    loadConfiguration();
  }, [debateId, participantId]);

  const handleFlowTypeChange = (flowType: string) => {
    setSelectedFlowType(flowType);
    // Reset parameters to defaults for the new flow type
    setFlowParameters(getDefaultParameters(flowType));
  };

  const getDefaultParameters = (flowType: string): Record<string, any> => {
    switch (flowType) {
      case 'INTERNAL_MONOLOGUE':
        return {
          prefix: 'Take a deep breath, think step by step, and show your work.',
          temperature: 0.7,
        };
      case 'SELF_CRITIQUE_LOOP':
        return {
          iterations: 1,
          temperature: 0.7,
        };
      case 'MULTI_AGENT_RED_TEAM':
        return {
          personas: {
            architect: 'You are the Architect. Your role is to propose a solution to the problem.',
            skeptic: 'You are the Skeptic. Your role is to find flaws in the Architect\'s solution.',
            judge: 'You are the Judge. Your role is to evaluate both perspectives and make a final decision.',
          },
        };
      case 'TOOL_CALLING_VERIFICATION':
        return {
          enabledTools: ['web_search', 'calculator'],
          maxToolCalls: 3,
        };
      case 'RAG_WITH_RERANKING':
        return {
          initialRetrievalCount: 20,
          finalDocumentCount: 5,
        };
      case 'CONFIDENCE_SCORING':
        return {
          confidenceThreshold: 70,
          improvementStrategy: 'self_critique',
        };
      case 'CONSTITUTIONAL_PROMPTING':
        return {
          principles: [
            'If a date or number is not explicitly in the context, say "I don\'t know"',
            'Always cite sources when making factual claims',
            'Acknowledge uncertainty when appropriate',
          ],
        };
      case 'ENSEMBLE_VOTING':
        return {
          ensembleSize: 3,
          temperatures: [0.3, 0.7, 1.0],
        };
      case 'POST_PROCESSING_RULES':
        return {
          rules: [
            { type: 'regex', pattern: '\\d{4}-\\d{2}-\\d{2}', description: 'Date format validation' },
            { type: 'length', max: 2000, description: 'Maximum response length' },
          ],
        };
      default:
        return {};
    }
  };

  const handleSave = async () => {
    if (!selectedFlowType) {
      dispatch(addNotification({
        type: 'error',
        message: 'Please select a flow type',
      }));
      return;
    }

    setSaving(true);
    try {
      const configuration: AgenticFlowConfiguration = {
        flowId: currentConfig?.flowId,
        flowType: selectedFlowType,
        enabled: flowEnabled,
        parameters: flowParameters,
      };

      if (participantId) {
        await debateClient.configureParticipantAgenticFlow(
          debateId,
          participantId,
          configuration
        );
      } else {
        await debateClient.configureDebateAgenticFlow(debateId, configuration);
      }

      dispatch(addNotification({
        type: 'success',
        message: 'Agentic flow configuration saved successfully',
      }));

      if (onSave) {
        onSave(configuration);
      }
    } catch (error) {
      dispatch(addNotification({
        type: 'error',
        message: 'Failed to save agentic flow configuration',
      }));
    } finally {
      setSaving(false);
    }
  };

  const renderParameterFields = () => {
    if (!selectedFlowType) return null;

    switch (selectedFlowType) {
      case 'INTERNAL_MONOLOGUE':
        return (
          <>
            <FormField 
              label="Thinking Prompt" 
              tooltip="The prefix added to prompts to encourage step-by-step reasoning"
            >
              <TextArea
                value={flowParameters.prefix || ''}
                onChange={(e) => setFlowParameters({ ...flowParameters, prefix: e.target.value })}
                rows={2}
                placeholder="Enter the thinking prompt..."
              />
            </FormField>
            <FormField label="Temperature">
              <Slider
                min={0}
                max={2}
                step={0.1}
                value={flowParameters.temperature || 0.7}
                onChange={(value) => setFlowParameters({ ...flowParameters, temperature: value })}
                marks={{ 0: '0', 0.7: '0.7', 1: '1', 2: '2' }}
              />
            </FormField>
          </>
        );

      case 'SELF_CRITIQUE_LOOP':
        return (
          <>
            <FormField 
              label="Critique Iterations" 
              tooltip="Number of times to critique and revise the response"
            >
              <Select
                value={flowParameters.iterations || 1}
                onChange={(value) => setFlowParameters({ ...flowParameters, iterations: value })}
                style={{ width: '100%' }}
              >
                <Select.Option value={1}>1 iteration</Select.Option>
                <Select.Option value={2}>2 iterations</Select.Option>
                <Select.Option value={3}>3 iterations</Select.Option>
              </Select>
            </FormField>
          </>
        );

      case 'TOOL_CALLING_VERIFICATION':
        return (
          <>
            <FormField 
              label="Enabled Tools" 
              tooltip="Select which external tools the AI can use"
            >
              <Select
                mode="multiple"
                value={flowParameters.enabledTools || []}
                onChange={(value) => setFlowParameters({ ...flowParameters, enabledTools: value })}
                style={{ width: '100%' }}
              >
                <Select.Option value="web_search">Web Search</Select.Option>
                <Select.Option value="calculator">Calculator</Select.Option>
                <Select.Option value="code_interpreter">Code Interpreter</Select.Option>
              </Select>
            </FormField>
            <FormField label="Max Tool Calls">
              <InputNumber
                value={flowParameters.maxToolCalls || 3}
                onChange={(value) => setFlowParameters({ ...flowParameters, maxToolCalls: value })}
                min={1}
                max={10}
                style={{ width: '100%' }}
              />
            </FormField>
          </>
        );

      case 'RAG_WITH_RERANKING':
        return (
          <>
            <FormField 
              label="Initial Retrieval Count" 
              tooltip="Number of documents to retrieve before re-ranking"
            >
              <InputNumber
                value={flowParameters.initialRetrievalCount || 20}
                onChange={(value) => setFlowParameters({ ...flowParameters, initialRetrievalCount: value })}
                min={10}
                max={100}
                style={{ width: '100%' }}
              />
            </FormField>
            <FormField 
              label="Final Document Count" 
              tooltip="Number of documents to keep after re-ranking"
            >
              <InputNumber
                value={flowParameters.finalDocumentCount || 5}
                onChange={(value) => setFlowParameters({ ...flowParameters, finalDocumentCount: value })}
                min={1}
                max={20}
                style={{ width: '100%' }}
              />
            </FormField>
          </>
        );

      case 'CONFIDENCE_SCORING':
        return (
          <>
            <FormField 
              label="Confidence Threshold (%)" 
              tooltip="Responses below this confidence will trigger improvement"
            >
              <InputNumber
                value={flowParameters.confidenceThreshold || 70}
                onChange={(value) => setFlowParameters({ ...flowParameters, confidenceThreshold: value })}
                min={0}
                max={100}
                style={{ width: '100%' }}
                formatter={value => `${value}%`}
                parser={value => value?.replace('%', '') as any}
              />
            </FormField>
            <FormField label="Improvement Strategy">
              <Select
                value={flowParameters.improvementStrategy || 'self_critique'}
                onChange={(value) => setFlowParameters({ ...flowParameters, improvementStrategy: value })}
                style={{ width: '100%' }}
              >
                <Select.Option value="self_critique">Self-Critique</Select.Option>
                <Select.Option value="tool_calling">Tool Calling</Select.Option>
                <Select.Option value="ensemble">Ensemble Voting</Select.Option>
              </Select>
            </FormField>
          </>
        );

      case 'ENSEMBLE_VOTING':
        return (
          <>
            <FormField 
              label="Ensemble Size" 
              tooltip="Number of responses to generate for voting"
            >
              <Select
                value={flowParameters.ensembleSize || 3}
                onChange={(value) => setFlowParameters({ ...flowParameters, ensembleSize: value })}
                style={{ width: '100%' }}
              >
                <Select.Option value={3}>3 responses</Select.Option>
                <Select.Option value={5}>5 responses</Select.Option>
                <Select.Option value={7}>7 responses</Select.Option>
              </Select>
            </FormField>
          </>
        );

      default:
        return (
          <Alert
            message="Configuration Options"
            description="Advanced configuration options for this flow type are coming soon."
            type="info"
            showIcon
          />
        );
    }
  };

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px' }}>
          <Spin size="large" />
          <div style={{ marginTop: '16px' }}>Loading configuration...</div>
        </div>
      </Card>
    );
  }

  return (
    <Card>
      <Title level={4}>
        <SettingOutlined style={{ marginRight: 8 }} />
        Agentic Flow Configuration
      </Title>
      <Paragraph type="secondary">
        Configure advanced reasoning and decision-making patterns for AI participants
      </Paragraph>

      <Divider />

      <Form layout="vertical">
        <FormField label="Enable Agentic Flow">
          <Switch
            checked={flowEnabled}
            onChange={setFlowEnabled}
            checkedChildren="Enabled"
            unCheckedChildren="Disabled"
          />
        </FormField>

        <FormField 
          label="Flow Type" 
          tooltip="Select the reasoning pattern to apply"
        >
          <Select
            value={selectedFlowType}
            onChange={handleFlowTypeChange}
            placeholder="Select a flow type"
            style={{ width: '100%' }}
            disabled={!flowEnabled}
          >
            {Object.entries(
              FLOW_TYPES.reduce((acc, type) => {
                if (!acc[type.category]) acc[type.category] = [];
                acc[type.category].push(type);
                return acc;
              }, {} as Record<string, AgenticFlowType[]>)
            ).map(([category, types]) => (
              <Select.OptGroup key={category} label={category.charAt(0).toUpperCase() + category.slice(1)}>
                {types.map(type => (
                  <Select.Option key={type.value} value={type.value}>
                    <div>
                      <div>{type.label}</div>
                      <div style={{ fontSize: '12px', color: '#8c8c8c' }}>{type.description}</div>
                    </div>
                  </Select.Option>
                ))}
              </Select.OptGroup>
            ))}
          </Select>
        </FormField>

        {flowEnabled && selectedFlowType && (
          <>
            <Divider orientation="left">Configuration Parameters</Divider>
            {renderParameterFields()}
          </>
        )}

        <div style={{ marginTop: '24px' }}>
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
            disabled={!flowEnabled || !selectedFlowType}
          >
            Save Configuration
          </Button>
        </div>
      </Form>
    </Card>
  );
};

export default AgenticFlowConfig;