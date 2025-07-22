import React, { useState, useEffect } from 'react';
import {
  Card,
  Row,
  Col,
  Select,
  Button,
  Space,
  Typography,
  Statistic,
  Progress,
  Tag,
  
  Divider,
  Alert,
} from 'antd';
import {
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  FilePdfOutlined,
  GlobalOutlined,
  DownloadOutlined,
  FullscreenOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';

const { Title, Text } = Typography;
const { Option } = Select;

interface DebateVisualizationsProps {
  debateId: string;
  debate: any;
}

interface AnalyticsData {
  participantEngagement: Array<{
    participant: string;
    responseCount: number;
    avgResponseLength: number;
    sentiment: number;
  }>;
  roundProgression: Array<{
    round: number;
    totalTokens: number;
    avgSentiment: number;
    keyTopics: string[];
  }>;
  topicEvolution: Array<{
    topic: string;
    frequency: number;
    rounds: number[];
  }>;
  qualityMetrics: {
    argumentStrength: number;
    coherence: number;
    factualAccuracy: number;
    engagement: number;
  };
}

const DebateVisualizations: React.FC<DebateVisualizationsProps> = ({
  debateId,
  debate,
}) => {
//   const [loading, setLoading] = useState(false); // SonarCloud: removed useless assignment
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null);
  const [selectedVisualization, setSelectedVisualization] = useState('engagement');
// //   const [showPdfViewer, setShowPdfViewer] = useState(false); // SonarCloud: removed useless assignment // SonarCloud: removed useless assignment
// //   const [showMap, setShowMap] = useState(false); // SonarCloud: removed useless assignment // SonarCloud: removed useless assignment

  useEffect(() => {
    loadAnalyticsData();
  }, [debateId]);

  const loadAnalyticsData = async () => {
    setLoading(true);
    try {
      // Generate sample analytics data
      const sampleData: AnalyticsData = {
        participantEngagement: [
          {
            participant: 'AI Participant 1',
            responseCount: 8,
            avgResponseLength: 342,
            sentiment: 0.65,
          },
          {
            participant: 'AI Participant 2',
            responseCount: 7,
            avgResponseLength: 298,
            sentiment: -0.23,
          },
          {
            participant: 'AI Participant 3',
            responseCount: 6,
            avgResponseLength: 378,
            sentiment: 0.12,
          },
        ],
        roundProgression: [
          { round: 1, totalTokens: 1250, avgSentiment: 0.3, keyTopics: ['introduction', 'stance'] },
          { round: 2, totalTokens: 1680, avgSentiment: 0.1, keyTopics: ['evidence', 'counterargument'] },
          { round: 3, totalTokens: 1920, avgSentiment: -0.1, keyTopics: ['refutation', 'analysis'] },
          { round: 4, totalTokens: 2100, avgSentiment: 0.2, keyTopics: ['synthesis', 'conclusion'] },
        ],
        topicEvolution: [
          { topic: 'Economic Impact', frequency: 15, rounds: [1, 2, 3, 4] },
          { topic: 'Social Benefits', frequency: 12, rounds: [1, 3, 4] },
          { topic: 'Environmental Concerns', frequency: 8, rounds: [2, 3] },
          { topic: 'Technological Innovation', frequency: 6, rounds: [1, 4] },
        ],
        qualityMetrics: {
          argumentStrength: 78,
          coherence: 85,
          factualAccuracy: 92,
          engagement: 73,
        },
      };
      
      setAnalyticsData(sampleData);
    } catch (error) {
      console.error('Failed to load analytics data:', error);
    } finally {
      setLoading(false);
    }
  };

  const renderEngagementChart = () => {
    if (!analyticsData) return null;

    return (
      <div>
        <Title level={4}>Participant Engagement Analysis</Title>
        <Row gutter={[16, 16]}>
          {analyticsData.participantEngagement.map((participant, index) => (
            <Col span={8} key={participant.participant}>
              <Card size="small">
                <Statistic
                  title={participant.participant}
                  value={participant.responseCount}
                  suffix="responses"
                  valueStyle={{ color: index === 0 ? '#3f8600' : index === 1 ? '#cf1322' : '#1890ff' }}
                />
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary">Avg Length: {participant.avgResponseLength} tokens</Text>
                  <br />
                  <Text type="secondary">
                    Sentiment: 
                    <Tag color={participant.sentiment > 0 ? 'green' : participant.sentiment < 0 ? 'red' : 'blue'}>
                      {participant.sentiment > 0 ? 'Positive' : participant.sentiment < 0 ? 'Negative' : 'Neutral'}
                    </Tag>
                  </Text>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
        
        <Divider />
        
        <div style={{ background: '#f5f5f5', padding: '16px', borderRadius: '6px' }}>
          <Text strong>Engagement Insights:</Text>
          <ul style={{ marginTop: '8px', marginBottom: 0 }}>
            <li>AI Participant 1 shows highest engagement with 8 responses and positive sentiment</li>
            <li>Response lengths are consistent across participants (298-378 tokens)</li>
            <li>Sentiment distribution indicates healthy debate dynamics</li>
          </ul>
        </div>
      </div>
    );
  };

  const renderProgressionChart = () => {
    if (!analyticsData) return null;

    return (
      <div>
        <Title level={4}>Round Progression Analysis</Title>
        <Row gutter={[16, 16]}>
          {analyticsData.roundProgression.map((round) => (
            <Col span={6} key={round.round}>
              <Card size="small" title={`Round ${round.round}`}>
                <Statistic
                  title="Total Tokens"
                  value={round.totalTokens}
                  valueStyle={{ color: '#1890ff' }}
                />
                <div style={{ marginTop: '12px' }}>
                  <Text type="secondary">Avg Sentiment: </Text>
                  <Progress 
                    percent={Math.abs(round.avgSentiment * 100)} 
                    size="small"
                    strokeColor={round.avgSentiment >= 0 ? '#52c41a' : '#ff4d4f'}
                  />
                </div>
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary">Key Topics:</Text>
                  <div style={{ marginTop: '4px' }}>
                    {round.keyTopics.map(topic => (
                      <Tag key={topic} size="small">{topic}</Tag>
                    ))}
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
        
        <Divider />
        
        <div style={{ background: '#f5f5f5', padding: '16px', borderRadius: '6px' }}>
          <Text strong>Progression Insights:</Text>
          <ul style={{ marginTop: '8px', marginBottom: 0 }}>
            <li>Token usage increases progressively through rounds (1250 → 2100)</li>
            <li>Sentiment becomes more neutral in middle rounds, indicating critical analysis</li>
            <li>Topic evolution shows natural debate flow from introduction to conclusion</li>
          </ul>
        </div>
      </div>
    );
  };

  const renderTopicEvolution = () => {
    if (!analyticsData) return null;

    return (
      <div>
        <Title level={4}>Topic Evolution & Frequency</Title>
        <Row gutter={[16, 16]}>
          {analyticsData.topicEvolution.map((topic) => (
            <Col span={12} key={topic.topic}>
              <Card size="small">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text strong>{topic.topic}</Text>
                  <Tag color="blue">{topic.frequency} mentions</Tag>
                </div>
                <div style={{ marginTop: '8px' }}>
                  <Text type="secondary">Discussed in rounds: </Text>
                  {topic.rounds.map(round => (
                    <Tag key={round} size="small" color="green">{round}</Tag>
                  ))}
                </div>
                <Progress 
                  percent={(topic.frequency / 15) * 100} 
                  size="small" 
                  style={{ marginTop: '8px' }}
                />
              </Card>
            </Col>
          ))}
        </Row>
        
        <Divider />
        
        <div style={{ background: '#f5f5f5', padding: '16px', borderRadius: '6px' }}>
          <Text strong>Topic Analysis:</Text>
          <ul style={{ marginTop: '8px', marginBottom: 0 }}>
            <li>Economic Impact is the most discussed topic (15 mentions across all rounds)</li>
            <li>Environmental Concerns peaked in middle rounds (2-3)</li>
            <li>Social Benefits maintained consistent relevance throughout</li>
          </ul>
        </div>
      </div>
    );
  };

  const renderQualityMetrics = () => {
    if (!analyticsData) return null;

    return (
      <div>
        <Title level={4}>Debate Quality Metrics</Title>
        <Row gutter={[16, 16]}>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="Argument Strength"
                value={analyticsData.qualityMetrics.argumentStrength}
                suffix="%"
                valueStyle={{ color: analyticsData.qualityMetrics.argumentStrength > 75 ? '#3f8600' : '#faad14' }}
              />
              <Progress 
                percent={analyticsData.qualityMetrics.argumentStrength} 
                size="small"
                strokeColor="#3f8600"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="Coherence"
                value={analyticsData.qualityMetrics.coherence}
                suffix="%"
                valueStyle={{ color: analyticsData.qualityMetrics.coherence > 80 ? '#3f8600' : '#faad14' }}
              />
              <Progress 
                percent={analyticsData.qualityMetrics.coherence} 
                size="small"
                strokeColor="#1890ff"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="Factual Accuracy"
                value={analyticsData.qualityMetrics.factualAccuracy}
                suffix="%"
                valueStyle={{ color: analyticsData.qualityMetrics.factualAccuracy > 85 ? '#3f8600' : '#faad14' }}
              />
              <Progress 
                percent={analyticsData.qualityMetrics.factualAccuracy} 
                size="small"
                strokeColor="#52c41a"
              />
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <Statistic
                title="Engagement"
                value={analyticsData.qualityMetrics.engagement}
                suffix="%"
                valueStyle={{ color: analyticsData.qualityMetrics.engagement > 70 ? '#3f8600' : '#faad14' }}
              />
              <Progress 
                percent={analyticsData.qualityMetrics.engagement} 
                size="small"
                strokeColor="#722ed1"
              />
            </Card>
          </Col>
        </Row>
        
        <Divider />
        
        <Alert
          message="SAS Level Assessment"
          description={
            <div>
              <Text strong>Current SAS Level: Intermediate (Level 3)</Text>
              <ul style={{ marginTop: '8px', marginBottom: 0 }}>
                <li><Text type="success">High factual accuracy (92%)</Text></li>
                <li><Text type="success">Good coherence (85%)</Text></li>
                <li><Text type="warning">Moderate argument strength (78%)</Text></li>
                <li><Text type="warning">Improving engagement needed (73%)</Text></li>
              </ul>
            </div>
          }
          type="info"
          showIcon
          icon={<InfoCircleOutlined />}
        />
      </div>
    );
  };

  const renderPdfViewer = () => (
    <div>
      <Title level={4}>Debate Document Viewer</Title>
      <div style={{ 
        height: '400px', 
        border: '2px dashed #d9d9d9', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        flexDirection: 'column',
        background: '#fafafa',
      }}>
        <FilePdfOutlined style={{ fontSize: '48px', color: '#d9d9d9', marginBottom: '16px' }} />
        <Text type="secondary">PDF Viewer Component</Text>
        <Text type="secondary" style={{ fontSize: '12px' }}>
          Sample documents: Debate transcript, Supporting materials, Analysis report
        </Text>
        <Button 
          type="primary" 
          icon={<DownloadOutlined />}
          style={{ marginTop: '16px' }}
          onClick={() => console.log('Download PDF')}
        >
          Download Debate Transcript
        </Button>
      </div>
    </div>
  );

  const renderMapVisualization = () => (
    <div>
      <Title level={4}>Debate Geographic Context</Title>
      <div style={{ 
        height: '400px', 
        border: '2px dashed #d9d9d9', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        flexDirection: 'column',
        background: '#fafafa',
      }}>
        <GlobalOutlined style={{ fontSize: '48px', color: '#d9d9d9', marginBottom: '16px' }} />
        <Text type="secondary">Interactive Map Component</Text>
        <Text type="secondary" style={{ fontSize: '12px' }}>
          Geographic data points: Policy impact regions, Case study locations, Stakeholder distribution
        </Text>
        <Space style={{ marginTop: '16px' }}>
          <Tag color="blue">North America: 45% coverage</Tag>
          <Tag color="green">Europe: 32% coverage</Tag>
          <Tag color="orange">Asia: 23% coverage</Tag>
        </Space>
      </div>
    </div>
  );

  const renderSelectedVisualization = () => {
    switch (selectedVisualization) {
      case 'engagement':
        return renderEngagementChart();
      case 'progression':
        return renderProgressionChart();
      case 'topics':
        return renderTopicEvolution();
      case 'quality':
        return renderQualityMetrics();
      case 'pdf':
        return renderPdfViewer();
      case 'map':
        return renderMapVisualization();
      default:
        return renderEngagementChart();
    }
  };

  return (
    <Card
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChartOutlined />
          Debate Analytics & Visualizations
        </div>
      }
      extra={
        <Space>
          <Select
            value={selectedVisualization}
            onChange={setSelectedVisualization}
            style={{ width: 200 }}
          >
            <Option value="engagement">
              <BarChartOutlined /> Engagement
            </Option>
            <Option value="progression">
              <LineChartOutlined /> Progression
            </Option>
            <Option value="topics">
              <PieChartOutlined /> Topics
            </Option>
            <Option value="quality">
              <InfoCircleOutlined /> Quality
            </Option>
            <Option value="pdf">
              <FilePdfOutlined /> Documents
            </Option>
            <Option value="map">
              <GlobalOutlined /> Geography
            </Option>
          </Select>
          <Button icon={<FullscreenOutlined />} size="small">
            Fullscreen
          </Button>
        </Space>
      }
    >
      <div style={{ minHeight: '400px' }}>
        {renderSelectedVisualization()}
      </div>
    </Card>
  );
};

export default DebateVisualizations;