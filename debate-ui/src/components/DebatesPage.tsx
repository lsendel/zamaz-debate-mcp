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
import { Button, Card, Badge, Row, Col, Spin, Empty, Space } from 'antd';
import { 
  PlusOutlined, 
  PlayCircleOutlined, 
  PauseCircleOutlined, 
  StopOutlined, 
  ReloadOutlined 
} from '@ant-design/icons';
import { 
  PageTitle, 
  SectionTitle, 
  CardTitle, 
  BodyText, 
  BodyParagraph,
  Caption
} from './Typography';
import { colors, spacing, borderRadius, shadows } from '../styles';

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
      <div style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center', 
        height: `${spacing[64]}px` 
      }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <div style={{ marginBottom: `${spacing[6]}px` }}>
        <div style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'center', 
          marginBottom: `${spacing[4]}px` 
        }}>
          <PageTitle style={{ margin: 0 }}>Debates</PageTitle>
          <Space size={spacing[2]}>
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
          <Card 
            size="small" 
            style={{ 
              backgroundColor: colors.background.tertiary,
              border: `1px solid ${colors.border.light}`,
              borderRadius: `${borderRadius.card}px`
            }}
          >
            <div style={{ 
              display: 'flex', 
              alignItems: 'center', 
              gap: `${spacing[4]}px`,
              flexWrap: 'wrap'
            }}>
              <div>
                <BodyText style={{ fontWeight: 600, marginRight: `${spacing[2]}px` }}>
                  Organization Scope:
                </BodyText>
                <Badge color={colors.primary[500]} text={currentOrganization.name} />
              </div>
              <div>
                <BodyText style={{ fontWeight: 600, marginRight: `${spacing[2]}px` }}>
                  Active LLM Presets:
                </BodyText>
                <Badge color={colors.semantic.success} text="3 presets available" />
              </div>
              <div>
                <BodyText style={{ fontWeight: 600, marginRight: `${spacing[2]}px` }}>
                  Debate Access:
                </BodyText>
                <Badge color={colors.primary[700]} text="Organization members" />
              </div>
            </div>
          </Card>
        )}
      </div>

      {debates.length === 0 ? (
        <Card style={{ 
          textAlign: 'center', 
          padding: `${spacing[12]}px`,
          boxShadow: shadows.card,
          borderRadius: `${borderRadius.card}px`
        }}>
          <Empty
            description={
              <Space direction="vertical" size={spacing[6]}>
                <SectionTitle style={{ margin: 0, color: colors.text.primary }}>
                  No debates yet
                </SectionTitle>
                <BodyText secondary style={{ fontSize: '16px' }}>
                  Create your first debate to get started
                </BodyText>
                <Button
                  type="primary"
                  onClick={() => dispatch(openCreateDebateDialog())}
                  icon={<PlusOutlined />}
                  size="large"
                  style={{ marginTop: `${spacing[4]}px` }}
                >
                  Create Debate
                </Button>
              </Space>
            }
          />
        </Card>
      ) : (
        <Row gutter={[spacing[4], spacing[4]]}>
          {debates.map((debate) => (
            <Col key={debate.id} xs={24} md={12} lg={8}>
              <Card
                hoverable
                onClick={() => navigate(`/debates/${debate.id}`)}
                style={{ 
                  cursor: 'pointer', 
                  height: '100%',
                  transition: 'all 0.3s ease',
                  boxShadow: shadows.card,
                  borderRadius: `${borderRadius.card}px`,
                  border: `1px solid ${colors.border.light}`
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.boxShadow = shadows.cardHover;
                  e.currentTarget.style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.boxShadow = shadows.card;
                  e.currentTarget.style.transform = 'translateY(0)';
                }}
                bodyStyle={{ padding: `${spacing[6]}px` }}
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
                <div style={{ marginBottom: `${spacing[4]}px` }}>
                  <CardTitle 
                    style={{ 
                      marginBottom: `${spacing[2]}px`,
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}
                  >
                    {debate.topic}
                  </CardTitle>
                  <BodyParagraph 
                    secondary
                    style={{ 
                      marginBottom: `${spacing[4]}px`,
                      minHeight: '44px',
                      overflow: 'hidden',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      textOverflow: 'ellipsis'
                    }}
                  >
                    {debate.description || 'No description provided'}
                  </BodyParagraph>
                </div>
                <Space direction="vertical" style={{ width: '100%' }} size={spacing[3]}>
                  <Space wrap size={spacing[2]}>
                    <Badge 
                      status={getStatusColor(debate.status)}
                      text={
                        <BodyText style={{ textTransform: 'capitalize' }}>
                          {debate.status.replace('_', ' ')}
                        </BodyText>
                      }
                    />
                    <Badge 
                      count={
                        <span style={{ 
                          backgroundColor: colors.background.tertiary, 
                          color: colors.text.secondary,
                          padding: `${spacing[1]}px ${spacing[2]}px`,
                          borderRadius: `${borderRadius.badge}px`,
                          fontSize: '12px',
                          fontWeight: 500
                        }}>
                          Round {debate.currentRound}/{debate.maxRounds}
                        </span>
                      }
                    />
                  </Space>
                  <Caption style={{ color: colors.text.secondary }}>
                    {debate.participants.length} participants
                  </Caption>
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