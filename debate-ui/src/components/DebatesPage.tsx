import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../store';
import {
  fetchDebates,
  startDebate,
  pauseDebate,
  cancelDebate,
} from '../store/slices/debateSlice';
import { openCreateDebateDialog } from '../store/slices/uiSlice';
import CreateDebateDialog from './CreateDebateDialog';
import { Button, Card, Badge, Typography, Row, Col, Spin, Empty, Space } from 'antd';
import { 
  PlusOutlined, 
  PlayCircleOutlined, 
  PauseCircleOutlined, 
  StopOutlined, 
  ReloadOutlined 
} from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;

const DebatesPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { debates, loading } = useAppSelector((state) => state.debate);
  const { currentOrganization } = useAppSelector((state) => state.organization);

  useEffect(() => {
    if (currentOrganization) {
      dispatch(fetchDebates());
    }
  }, [dispatch, currentOrganization]);

  const handleRefresh = () => {
    dispatch(fetchDebates());
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'created':
        return 'default';
      case 'in_progress':
        return 'processing';
      case 'completed':
        return 'success';
      case 'cancelled':
        return 'error';
      default:
        return 'default';
    }
  };

  const handleDebateAction = async (
    e: React.MouseEvent,
    debateId: string,
    action: string
  ) => {
    e.stopPropagation();
    switch (action) {
      case 'start':
        await dispatch(startDebate(debateId));
        break;
      case 'pause':
        await dispatch(pauseDebate(debateId));
        break;
      case 'cancel':
        await dispatch(cancelDebate(debateId));
        break;
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '256px' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <Title level={2} style={{ margin: 0 }}>Debates</Title>
          <Space>
            <Button
              onClick={handleRefresh}
              icon={<ReloadOutlined />}
            >
              Refresh
            </Button>
            <Button
              type="primary"
              onClick={() => dispatch(openCreateDebateDialog())}
              icon={<PlusOutlined />}
            >
              Create Debate
            </Button>
          </Space>
        </div>
        
        {currentOrganization && (
          <Card size="small" style={{ backgroundColor: '#f6f8fa' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div>
                <Text strong>Organization Scope: </Text>
                <Badge color="blue" text={currentOrganization.name} />
              </div>
              <div>
                <Text strong>Active LLM Presets: </Text>
                <Badge color="green" text="3 presets available" />
              </div>
              <div>
                <Text strong>Debate Access: </Text>
                <Badge color="purple" text="Organization members" />
              </div>
            </div>
          </Card>
        )}
      </div>

      {debates.length === 0 ? (
        <Card style={{ textAlign: 'center', padding: '48px' }}>
          <Empty
            description={
              <Space direction="vertical" size="large">
                <Title level={4} style={{ margin: 0 }}>No debates yet</Title>
                <Text type="secondary">Create your first debate to get started</Text>
                <Button
                  type="primary"
                  onClick={() => dispatch(openCreateDebateDialog())}
                  icon={<PlusOutlined />}
                >
                  Create Debate
                </Button>
              </Space>
            }
          />
        </Card>
      ) : (
        <Row gutter={[16, 16]}>
          {debates.map((debate) => (
            <Col key={debate.id} xs={24} md={12} lg={8}>
              <Card
                hoverable
                onClick={() => navigate(`/debates/${debate.id}`)}
                style={{ cursor: 'pointer', height: '100%' }}
                actions={[
                  debate.status === 'created' && (
                    <Button
                      size="small"
                      type="text"
                      onClick={(e) => handleDebateAction(e, debate.id, 'start')}
                      icon={<PlayCircleOutlined />}
                    >
                      Start
                    </Button>
                  ),
                  debate.status === 'in_progress' && (
                    <Button
                      size="small"
                      type="text"
                      onClick={(e) => handleDebateAction(e, debate.id, 'pause')}
                      icon={<PauseCircleOutlined />}
                    >
                      Pause
                    </Button>
                  ),
                  (debate.status === 'created' ||
                    debate.status === 'in_progress') && (
                    <Button
                      size="small"
                      type="text"
                      danger
                      onClick={(e) => handleDebateAction(e, debate.id, 'cancel')}
                      icon={<StopOutlined />}
                    >
                      Cancel
                    </Button>
                  ),
                ].filter(Boolean)}
              >
                <Card.Meta
                  title={
                    <Text 
                      ellipsis={{ tooltip: debate.topic }} 
                      style={{ marginBottom: '8px' }}
                    >
                      {debate.topic}
                    </Text>
                  }
                  description={
                    <Paragraph 
                      ellipsis={{ rows: 2, tooltip: debate.description || 'No description' }}
                      style={{ marginBottom: '16px' }}
                    >
                      {debate.description || 'No description'}
                    </Paragraph>
                  }
                />
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Space wrap>
                    <Badge 
                      status={getStatusColor(debate.status)}
                      text={debate.status.replace('_', ' ')}
                    />
                    <Badge 
                      count={`Round ${debate.currentRound}/${debate.maxRounds}`}
                      style={{ backgroundColor: '#f0f0f0', color: '#666' }}
                    />
                  </Space>
                  <Text type="secondary" style={{ fontSize: '14px' }}>
                    {debate.participants.length} participants
                  </Text>
                </Space>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <CreateDebateDialog />
    </div>
  );
};

export default DebatesPage;