import React, { useState } from 'react';
import {
  Card,
  Typography,
  Tabs,
  Tag,
  Timeline,
  Collapse,
  List,
  Space,
  Progress,
  Divider,
  Alert,
  Row,
  Col,
  Statistic,
  Badge,
  
  
} from 'antd';
import {
  BulbOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  
  InfoCircleOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  SafetyOutlined,
  TeamOutlined,
  RobotOutlined,
  BranchesOutlined,
  AimOutlined,
  LinkOutlined,
import {  } from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;
const { Panel } = Collapse;

// Types
export interface AgenticFlowResult {
  flowId: string;
  flowType: string;
  executionId: string;
  timestamp: string;
  duration: number;
  status: 'success' | 'failed' | 'partial';
  finalAnswer: string;
  reasoning?: string;
  confidence?: number;
  iterations?: Array<{
    iteration: number;
    content: string;
    critique?: string;
    revision?: string;
  }>;
  perspectives?: {
    architect?: string;
    skeptic?: string;
    judge?: string;
  };
  toolCalls?: Array<{
    tool: string;
    query: string;
    result: string;
    timestamp: string;
  }>;
  documents?: Array<{
    id: string;
    title: string;
    relevanceScore: number;
    selected: boolean;
    excerpt: string;
  }>;
  violations?: Array<{
    rule: string;
    description: string;
    severity: 'error' | 'warning';
  }>;
  ensemble?: Array<{
    response: string;
    temperature: number;
    votes: number;
  }>;
  thoughtPaths?: Array<{
    path: string;
    score: number;
    selected: boolean;
  }>;
  principles?: string[];
  metadata?: Record<string, any>;
}

interface AgenticFlowResultProps {
  result: AgenticFlowResult;
  showMetadata?: boolean;
}

const getFlowIcon = (flowType: string): React.ReactNode => {
  const iconMap: Record<string, React.ReactNode> = {
    INTERNAL_MONOLOGUE: <BulbOutlined />,
    SELF_CRITIQUE_LOOP: <ExperimentOutlined />,
    MULTI_AGENT_RED_TEAM: <TeamOutlined />,
    TOOL_CALLING_VERIFICATION: <ThunderboltOutlined />,
    RAG_WITH_RERANKING: <FileSearchOutlined />,
    CONFIDENCE_SCORING: <AimOutlined />,
    CONSTITUTIONAL_PROMPTING: <SafetyOutlined />,
    ENSEMBLE_VOTING: <RobotOutlined />,
    POST_PROCESSING_RULES: <CheckCircleOutlined />,
    TREE_OF_THOUGHTS: <BranchesOutlined />,
    STEP_BACK_PROMPTING: <LinkOutlined />,
    PROMPT_CHAINING: <LinkOutlined />,
  };
  return iconMap[flowType] || <InfoCircleOutlined />;
};

const getFlowLabel = (flowType: string): string => {
  const labelMap: Record<string, string> = {
    INTERNAL_MONOLOGUE: 'Internal Monologue',
    SELF_CRITIQUE_LOOP: 'Self-Critique Loop',
    MULTI_AGENT_RED_TEAM: 'Multi-Agent Red Team',
    TOOL_CALLING_VERIFICATION: 'Tool-Calling Verification',
    RAG_WITH_RERANKING: 'RAG with Re-ranking',
    CONFIDENCE_SCORING: 'Confidence Scoring',
    CONSTITUTIONAL_PROMPTING: 'Constitutional Prompting',
    ENSEMBLE_VOTING: 'Ensemble Voting',
    POST_PROCESSING_RULES: 'Post-processing Rules',
    TREE_OF_THOUGHTS: 'Tree of Thoughts',
    STEP_BACK_PROMPTING: 'Step-Back Prompting',
    PROMPT_CHAINING: 'Prompt Chaining',
  };
  return labelMap[flowType] || flowType;
};

const getConfidenceColor = (confidence: number): string => {
  if (confidence >= 90) return '#52c41a';
  if (confidence >= 70) return '#1890ff';
  if (confidence >= 50) return '#faad14';
  return '#ff4d4f';
};

const AgenticFlowResultComponent: React.FC<AgenticFlowResultProps> = ({
  result,
  showMetadata = true,
}) => {
  const [activeTab, setActiveTab] = useState('result');

  const renderHeader = () => (
    <div style={{ marginBottom: '16px' }}>
      <Space align="center" size="middle">
        {getFlowIcon(result.flowType)}
        <Title level={4} style={{ margin: 0 }}>
          {getFlowLabel(result.flowType)}
        </Title>
        // TODO: Refactor nested ternary for better readability
        <Tag color={result.status === 'success' ? 'success' : result.status === 'failed' ? 'error' : 'warning'}>
          {result.status.toUpperCase()}
        </Tag>
        {result.confidence !== undefined && (
          <Tag color={getConfidenceColor(result.confidence)}>
            {result.confidence}% Confidence
          </Tag>
        )}
      </Space>
      {showMetadata && (
        <Space style={{ marginTop: '8px' }} size="large">
          <Text type="secondary">
            <ClockCircleOutlined /> {new Date(result.timestamp).toLocaleString()}
          </Text>
          <Text type="secondary">
            <ThunderboltOutlined /> {result.duration}ms
          </Text>
        </Space>
      )}
    </div>
  );

  const renderFinalAnswer = () => (
    <Card bordered={false}>
      <Title level={5}>Final Answer</Title>
      <Paragraph style={{ fontSize: '16px', lineHeight: '1.6' }}>
        {result.finalAnswer}
      </Paragraph>
    </Card>
  );

  const renderInternalMonologue = () => {
    if (!result.reasoning) return null;
    
    return (
      <Card bordered={false}>
        <Title level={5}>
          <BulbOutlined /> Reasoning Process
        </Title>
        <div style={{ 
          backgroundColor: '#f5f5f5', 
          padding: '16px', 
          borderRadius: '8px',
          fontFamily: 'monospace',
          whiteSpace: 'pre-wrap'
        }}>
          {result.reasoning}
        </div>
      </Card>
    );
  };

  const renderSelfCritiqueLoop = () => {
    if (!result.iterations || result.iterations.length === 0) return null;

    return (
      <Card bordered={false}>
        <Title level={5}>
          <ExperimentOutlined /> Critique Iterations
        </Title>
        <Timeline>
          {result.iterations.map((iteration, index) => (
            <Timeline.Item key={index} color={index === result.iterations!.length - 1 ? 'green' : 'blue'}>
              <Space direction="vertical" style={{ width: '100%' }}>
                <Tag color="blue">Iteration {iteration.iteration}</Tag>
                <Collapse ghost>
                  <Panel header="Initial Response" key="1">
                    <Paragraph>{iteration.content}</Paragraph>
                  </Panel>
                  {iteration.critique && (
                    <Panel header="Critique" key="2">
                      <Alert
                        message="Self-Critique"
                        description={iteration.critique}
                        type="warning"
                        showIcon
                      />
                    </Panel>
                  )}
                  {iteration.revision && (
                    <Panel header="Revised Response" key="3">
                      <Alert
                        message="Improved Version"
                        description={iteration.revision}
                        type="success"
                        showIcon
                      />
                    </Panel>
                  )}
                </Collapse>
              </Space>
            </Timeline.Item>
          ))}
        </Timeline>
      </Card>
    );
  };

  const renderMultiAgentRedTeam = () => {
    if (!result.perspectives) return null;

    return (
      <Card bordered={false}>
        <Title level={5}>
          <TeamOutlined /> Agent Perspectives
        </Title>
        <Row gutter={[16, 16]}>
          {result.perspectives.architect && (
            <Col span={8}>
              <Card title={<><UserOutlined /> Architect</>} type="inner">
                <Paragraph>{result.perspectives.architect}</Paragraph>
              </Card>
            </Col>
          )}
          {result.perspectives.skeptic && (
            <Col span={8}>
              <Card title={<><QuestionCircleOutlined /> Skeptic</>} type="inner">
                <Paragraph>{result.perspectives.skeptic}</Paragraph>
              </Card>
            </Col>
          )}
          {result.perspectives.judge && (
            <Col span={8}>
              <Card title={<><SafetyOutlined /> Judge</>} type="inner">
                <Paragraph>{result.perspectives.judge}</Paragraph>
              </Card>
            </Col>
          )}
        </Row>
      </Card>
    );
  };

  const renderToolCalls = () => {
    if (!result.toolCalls || result.toolCalls.length === 0) return null;

    return (
      <Card bordered={false}>
        <Title level={5}>
          <ThunderboltOutlined /> Tool Calls
        </Title>
        <List
          dataSource={result.toolCalls}
          renderItem={(call, index) => (
            <List.Item>
              <List.Item.Meta
                avatar={<Badge count={index + 1} style={{ backgroundColor: '#1890ff' }} />}
                title={<Space><Tag color="blue">{call.tool}</Tag><Text type="secondary">{call.timestamp}</Text></Space>}
                description={
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <Text strong>Query: </Text>
                      <Text code>{call.query}</Text>
                    </div>
                    <div>
                      <Text strong>Result: </Text>
                      <Paragraph style={{ marginBottom: 0 }}>{call.result}</Paragraph>
                    </div>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      </Card>
    );
  };

  const renderDocuments = () => {
    if (!result.documents || result.documents.length === 0) return null;

    const selectedDocs = result.documents.filter(doc => doc.selected);
    const rejectedDocs = result.documents.filter(doc => !doc.selected);

    return (
      <Card bordered={false}>
        <Title level={5}>
          <FileSearchOutlined /> Retrieved Documents
        </Title>
        <Tabs defaultActiveKey="selected">
          <TabPane tab={`Selected (${selectedDocs.length})`} key="selected">
            <List
              dataSource={selectedDocs}
              renderItem={(doc) => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text strong>{doc.title}</Text>
                        <Tag color="green">Score: {doc.relevanceScore}%</Tag>
                      </Space>
                    }
                    description={doc.excerpt}
                  />
                </List.Item>
              )}
            />
          </TabPane>
          <TabPane tab={`Rejected (${rejectedDocs.length})`} key="rejected">
            <List
              dataSource={rejectedDocs}
              renderItem={(doc) => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Text strong>{doc.title}</Text>
                        <Tag>Score: {doc.relevanceScore}%</Tag>
                      </Space>
                    }
                    description={doc.excerpt}
                  />
                </List.Item>
              )}
            />
          </TabPane>
        </Tabs>
      </Card>
    );
  };

  const renderConfidenceScore = () => {
    if (result.confidence === undefined) return null;

    return (
      <Card bordered={false}>
        <Title level={5}>
          <AimOutlined /> Confidence Analysis
        </Title>
        <Row gutter={16}>
          <Col span={8}>
            <Statistic
              title="Confidence Score"
              value={result.confidence}
              suffix="%"
              valueStyle={{ color: getConfidenceColor(result.confidence) }}
            />
          </Col>
          <Col span={16}>
            <Progress
              percent={result.confidence}
              strokeColor={getConfidenceColor(result.confidence)}
              format={(percent) => {
                if (percent! >= 90) return 'Very High';
                if (percent! >= 70) return 'High';
                if (percent! >= 50) return 'Medium';
                return 'Low';
              }}
            />
          </Col>
        </Row>
      </Card>
    );
  };

  const renderViolations = () => {
    if (!result.violations || result.violations.length === 0) return null;

    return (
      <Card bordered={false}>
        <Title level={5}>
          <SafetyOutlined /> Rule Violations
        </Title>
        <List
          dataSource={result.violations}
          renderItem={(violation) => (
            <Alert
              message={violation.rule}
              description={violation.description}
              type={violation.severity}
              showIcon
              style={{ marginBottom: '8px' }}
            />
          )}
        />
      </Card>
    );
  };

  const renderEnsembleVoting = () => {
    if (!result.ensemble || result.ensemble.length === 0) return null;

    const maxVotes = Math.max(...result.ensemble.map(e => e.votes));

    return (
      <Card bordered={false}>
        <Title level={5}>
          <RobotOutlined /> Ensemble Responses
        </Title>
        <List
          dataSource={result.ensemble}
          renderItem={(item, index) => (
            <List.Item>
              <List.Item.Meta
                avatar={
                  <Badge 
                    count={item.votes} 
                    style={{ 
                      backgroundColor: item.votes === maxVotes ? '#52c41a' : '#1890ff' 
                    }} 
                  />
                }
                title={
                  <Space>
                    <Text>Response {index + 1}</Text>
                    <Tag>Temp: {item.temperature}</Tag>
                    {item.votes === maxVotes && <Tag color="success">Selected</Tag>}
                  </Space>
                }
                description={item.response}
              />
            </List.Item>
          )}
        />
      </Card>
    );
  };

  const renderFlowSpecificContent = () => {
    switch (result.flowType) {
      case 'INTERNAL_MONOLOGUE':
        return renderInternalMonologue();
      case 'SELF_CRITIQUE_LOOP':
        return renderSelfCritiqueLoop();
      case 'MULTI_AGENT_RED_TEAM':
        return renderMultiAgentRedTeam();
      case 'TOOL_CALLING_VERIFICATION':
        return renderToolCalls();
      case 'RAG_WITH_RERANKING':
        return renderDocuments();
      case 'CONFIDENCE_SCORING':
        return renderConfidenceScore();
      case 'CONSTITUTIONAL_PROMPTING':
        return renderViolations();
      case 'ENSEMBLE_VOTING':
        return renderEnsembleVoting();
      default:
        return null;
    }
  };

  return (
    <Card>
      {renderHeader()}
      <Divider />
      
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="Result" key="result">
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {renderFinalAnswer()}
            {result.confidence !== undefined && renderConfidenceScore()}
          </Space>
        </TabPane>
        
        <TabPane tab="Process" key="process">
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {renderFlowSpecificContent()}
          </Space>
        </TabPane>
        
        {result.metadata && showMetadata && (
          <TabPane tab="Metadata" key="metadata">
            <Card bordered={false}>
              <pre style={{ overflow: 'auto' }}>
                {JSON.stringify(result.metadata, null, 2)}
              </pre>
            </Card>
          </TabPane>
        )}
      </Tabs>
    </Card>
  );
};

// Add missing imports that were used
import {
  ClockCircleOutlined,
  UserOutlined,
  QuestionCircleOutlined,

export default AgenticFlowResultComponent;