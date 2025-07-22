import React from 'react';
import { Progress, Card, Badge, Alert, Spin, Typography } from 'antd';
import {
  CheckCircleOutlined,
  BorderOutlined,
  LoadingOutlined,
} from '@ant-design/icons';
import { Debate } from '../api/debateClient';

const { Title, Text } = Typography;

interface DebateProgressProps {
  debate: Debate;
  isPolling?: boolean;
}

const DebateProgress: React.FC<DebateProgressProps> = ({ debate, isPolling }) => {
  const getRoundStatus = (roundNumber: number) => {
    const round = debate.rounds?.find(r => r.roundNumber === roundNumber);
    if (!round) return 'pending';
    if (round.status === 'completed') return 'completed';
    if (round.status === 'in_progress') return 'in_progress';
    return 'pending';
  };

  const getStepIcon = (roundNumber: number) => {
    const status = getRoundStatus(roundNumber);
    switch (status) {
      case 'completed':
        return <CheckCircleOutlined style={{ fontSize: '20px', color: '#52c41a' }} />;
      case 'in_progress':
        return <Spin indicator={<LoadingOutlined style={{ fontSize: 20 }} spin />} />;
      default:
        return <BorderOutlined style={{ fontSize: '20px', color: '#bfbfbf' }} />;
    }
  };

  const getProgressPercentage = () => {
    if (!debate.maxRounds || debate.maxRounds === 0) return 0;
    const completedRounds = debate.rounds?.filter(r => r.status === 'completed').length || 0;
    return (completedRounds / debate.maxRounds) * 100;
  };

// //   const isDebateActive = debate.status === 'IN_PROGRESS' || debate.status === 'CREATED'; // SonarCloud: removed useless assignment // Removed: useless assignment
  const isGenerating = debate.status === 'IN_PROGRESS' && debate.rounds?.some(r => r.status === 'in_progress');

  return (
    <div style={{ marginBottom: '24px' }}>
      {/* Status Header */}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '16px', gap: '16px' }}>
        <Title level={4} style={{ margin: 0 }}>Debate Progress</Title>
        {isPolling && (
          <Badge 
            count="Live" 
            style={{ 
              backgroundColor: '#52c41a',
              animation: 'pulse 2s infinite'
            }}
          >
            <Spin indicator={<LoadingOutlined style={{ fontSize: 14, marginRight: '4px' }} spin />} />
          </Badge>
        )}
        {isGenerating && (
          <Text type="secondary" style={{ fontSize: '14px' }}>
            Generating responses...
          </Text>
        )}
      </div>

      {/* Progress Bar */}
      <Card style={{ marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
          <div style={{ flex: 1, marginRight: '8px' }}>
            <Progress 
              percent={getProgressPercentage()} 
              strokeColor="#1890ff"
              size="default"
            />
          </div>
          <div style={{ minWidth: '45px' }}>
            <Text strong style={{ fontSize: '14px', color: '#595959' }}>
              {Math.round(getProgressPercentage())}%
            </Text>
          </div>
        </div>
        <Text type="secondary" style={{ fontSize: '14px' }}>
          {debate.rounds?.filter(r => r.status === 'completed').length || 0} of {debate.maxRounds || 0} rounds completed
        </Text>
      </Card>

      {/* Round Steps */}
      <Card bordered>
        <div style={{ padding: '24px' }}>
          <div className="space-y-6">
            {Array.from({ length: debate.maxRounds || 3 }, (_, i) => i + 1).map((roundNumber) => {
              const round = debate.rounds?.find(r => r.roundNumber === roundNumber);
              const status = getRoundStatus(roundNumber);
              const isActive = debate.currentRound === roundNumber;
              
              return (
                <div key={roundNumber} className="relative">
                  {/* Connector Line */}
                  {roundNumber < (debate.maxRounds || 3) && (
                    <div 
                      style={{
                        position: 'absolute',
                        top: '40px',
                        left: '20px',
                        bottom: 0,
                        width: '2px',
                        backgroundColor: status === 'completed' ? '#1890ff' : '#f0f0f0'
                      }} 
                    />
                  )}
                  
                  {/* Step */}
                  <div className="relative z-10 flex">
                    {/* Step Icon */}
                    <div
                      style={{
                        width: '40px',
                        height: '40px',
                        borderRadius: '50%',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 500,
                        transition: 'all 0.3s',
                        // TODO: Refactor nested ternary for better readability
                        backgroundColor: status === 'completed' ? '#1890ff' : isActive ? '#e6f7ff' : '#fafafa',
                        // TODO: Refactor nested ternary for better readability
                        color: status === 'completed' ? '#fff' : isActive ? '#096dd9' : '#bfbfbf',
                        // TODO: Refactor nested ternary for better readability
                        border: `2px solid ${status === 'completed' ? '#1890ff' : isActive ? '#1890ff' : '#f0f0f0'}`
                      }}
                    >
                      {getStepIcon(roundNumber)}
                    </div>

                    {/* Content */}
                    <div style={{ marginLeft: '16px', flex: 1 }}>
                      <Text strong style={{ color: isActive ? '#262626' : '#8c8c8c' }}>
                        Round {roundNumber}
                      </Text>
                      {status === 'in_progress' && (
                        <Text style={{ fontSize: '14px', color: '#1890ff', display: 'block', marginTop: '4px' }}>
                          Generating responses...
                        </Text>
                      )}
                      {round && round.responses && round.responses.length > 0 && (
                        <div className="ml-2 mt-2">
                          <Text type="secondary" style={{ fontSize: '14px' }}>
                            {round.responses.length} response{round.responses.length !== 1 ? 's' : ''} generated
                          </Text>
                        </div>
                      )}
                      {status === 'in_progress' && (
                        <Alert 
                          message="AI participants are formulating their responses..."
                          type="info"
                          showIcon
                          style={{ marginTop: '8px' }}
                        />
                      )}
                      {status === 'pending' && debate.status === 'IN_PROGRESS' && (
                        <Text type="secondary" style={{ fontSize: '14px', marginTop: '8px', display: 'block' }}>
                          Waiting to start...
                        </Text>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </Card>

      {/* Status Messages */}
      {debate.status === 'CREATED' && (
        <Alert 
          message='Click "Start" to begin the debate. AI participants will generate responses for each round.'
          type="info"
          showIcon
          style={{ marginTop: '16px' }}
        />
      )}
      
      {debate.status === 'COMPLETED' && (
        <Alert 
          message="Debate completed! All rounds have been generated."
          type="success"
          showIcon
          style={{ marginTop: '16px' }}
        />
      )}
      
      {debate.status === 'CANCELLED' && (
        <Alert 
          message="This debate was cancelled."
          type="warning"
          showIcon
          style={{ marginTop: '16px' }}
        />
      )}
    </div>
  );
};

export default DebateProgress;