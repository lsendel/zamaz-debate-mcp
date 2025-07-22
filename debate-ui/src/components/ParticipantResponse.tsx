import React, { useState } from 'react';
import {
  Card,
  Avatar,
  Space,
  Typography,
  Button,
  Collapse,
  Tag,
  Tooltip,
} from 'antd';
import {
  ExpandOutlined,
  CompressOutlined,
  BranchesOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import AgenticFlowResult from './AgenticFlowResult';
import type { AgenticFlowResult as AgenticFlowResultType } from './AgenticFlowResult';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;

interface Participant {
  id: string;
  name: string;
  llmProvider?: string;
  model?: string;
}

interface DebateResponse {
  id: string;
  participantId: string;
  content: string;
  timestamp: string;
  tokenCount?: number;
  agenticFlowResult?: AgenticFlowResultType;
}

interface ParticipantResponseProps {
  response: DebateResponse;
  participant: Participant | string;
  participantIndex: number;
  showAgenticFlow?: boolean;
}

const getParticipantColor = (index: number): string => {
  const colors = ['#1890ff', '#52c41a', '#fa8c16', '#eb2f96', '#722ed1'];
  return colors[index % colors.length];
};

const ParticipantResponse: React.FC<ParticipantResponseProps> = ({
  response,
  participant,
  participantIndex,
  showAgenticFlow = true,
}) => {
  const [expanded, setExpanded] = useState(false);
  
  // Handle both string and object participants
  const isString = typeof participant === 'string';
  const participantName = isString ? participant : participant.name;

  const hasAgenticFlow = showAgenticFlow && response.agenticFlowResult;

  return (
    <Card 
      style={{ marginBottom: '12px' }}
      actions={hasAgenticFlow ? [
        <Button
          type="text"
          icon={expanded ? <CompressOutlined /> : <ExpandOutlined />}
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? 'Hide' : 'Show'} Agentic Flow Details
        </Button>
      ] : undefined}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: '12px' }}>
        <Avatar
          size="default"
          style={{
            backgroundColor: getParticipantColor(participantIndex),
            marginRight: '12px',
            flexShrink: 0,
          }}
        >
          {participantName.charAt(0).toUpperCase()}
        </Avatar>
        
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '4px' }}>
            <Text strong style={{ marginRight: '8px' }}>
              {participantName}
            </Text>
            {hasAgenticFlow && (
              <Tooltip title="Response generated with Agentic Flow">
                <Tag icon={<BranchesOutlined />} color="blue">
                  {response.agenticFlowResult?.flowType}
                </Tag>
              </Tooltip>
            )}
          </div>
          
          <Space size="small" style={{ fontSize: '12px', color: '#8c8c8c' }}>
            <span>
              <ClockCircleOutlined /> {new Date(response.timestamp).toLocaleTimeString()}
            </span>
            {response.tokenCount && (
              <span>{response.tokenCount} tokens</span>
            )}
            {hasAgenticFlow && response.agenticFlowResult?.confidence !== undefined && (
              <Tag color={getConfidenceColor(response.agenticFlowResult.confidence)}>
                {response.agenticFlowResult.confidence}% confidence
              </Tag>
            )}
          </Space>
        </div>
      </div>
      
      <Paragraph style={{ marginBottom: hasAgenticFlow ? '16px' : 0 }}>
        {response.content}
      </Paragraph>
      
      {hasAgenticFlow && expanded && response.agenticFlowResult && (
        <Collapse ghost activeKey={['flow-details']}>
          <Panel header="Agentic Flow Details" key="flow-details">
            <AgenticFlowResult 
              result={response.agenticFlowResult}
              showMetadata={false}
            />
          </Panel>
        </Collapse>
      )}
    </Card>
  );
};

const getConfidenceColor = (confidence: number): string => {
  if (confidence >= 90) return 'success';
  if (confidence >= 70) return 'blue';
  if (confidence >= 50) return 'warning';
  return 'error';
};

export default ParticipantResponse;