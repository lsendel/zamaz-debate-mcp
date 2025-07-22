import React, { useState, useEffect } from 'react';
import {
  Card,
  Typography,
  Row,
  Col,
  Statistic,
  Select,
  DatePicker,
  Space,
  Spin,
  
  Tag,
  Progress,
  Tabs,
  Table,
  Empty,
  
} from 'antd';
import {
  TrendingUpOutlined,
  TrendingDownOutlined,
  ThunderboltOutlined,
  
  CheckCircleOutlined,
  AimOutlined,
  FireOutlined,
  BulbOutlined,
  
  
  
} from '@ant-design/icons';
import debateClient from '../api/debateClient';
import type { Dayjs } from 'antd/es/date-picker/generatePicker/interface';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

// Types
interface FlowTypeStats {
  flowType: string;
  executionCount: number;
  averageConfidence: number;
  successRate: number;
  averageExecutionTime: number;
  trendScore?: number;
}

interface TimeSeriesData {
  date: string;
  executions: number;
  averageConfidence: number;
  successRate: number;
}

interface TrendingFlow {
  flowType: string;
  usageCount: number;
  averageConfidence: number;
  successRate: number;
  averageExecutionTime: number;
  trendScore: number;
  trendCategory: string;
}

interface AgenticFlowAnalyticsProps {
  organizationId?: string;
  debateId?: string;
  height?: number;
}

const FLOW_TYPE_COLORS: Record<string, string> = {
  INTERNAL_MONOLOGUE: '#1890ff',
  SELF_CRITIQUE_LOOP: '#52c41a',
  MULTI_AGENT_RED_TEAM: '#fa8c16',
  TOOL_CALLING_VERIFICATION: '#eb2f96',
  RAG_WITH_RERANKING: '#722ed1',
  CONFIDENCE_SCORING: '#13c2c2',
  CONSTITUTIONAL_PROMPTING: '#faad14',
  ENSEMBLE_VOTING: '#f5222d',
  POST_PROCESSING_RULES: '#a0d911',
  TREE_OF_THOUGHTS: '#2f54eb',
  STEP_BACK_PROMPTING: '#fa541c',
  PROMPT_CHAINING: '#520339',
};

const getFlowTypeLabel = (flowType: string): string => {
  const labels: Record<string, string> = {
    INTERNAL_MONOLOGUE: 'Internal Monologue',
    SELF_CRITIQUE_LOOP: 'Self-Critique Loop',
    MULTI_AGENT_RED_TEAM: 'Multi-Agent Red Team',
    TOOL_CALLING_VERIFICATION: 'Tool-Calling',
    RAG_WITH_RERANKING: 'RAG with Re-ranking',
    CONFIDENCE_SCORING: 'Confidence Scoring',
    CONSTITUTIONAL_PROMPTING: 'Constitutional',
    ENSEMBLE_VOTING: 'Ensemble Voting',
    POST_PROCESSING_RULES: 'Post-processing',
    TREE_OF_THOUGHTS: 'Tree of Thoughts',
    STEP_BACK_PROMPTING: 'Step-Back',
    PROMPT_CHAINING: 'Prompt Chaining',
  };
  return labels[flowType] || flowType;
};

const AgenticFlowAnalytics: React.FC<AgenticFlowAnalyticsProps> = ({
  organizationId,
  debateId,
  height = 400,
}) => {
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs]>([
    dayjs().subtract(30, 'days'),
    dayjs(),
  ]);
  const [selectedFlowTypes, setSelectedFlowTypes] = useState<string[]>([]);
  const [flowTypeStats, setFlowTypeStats] = useState<FlowTypeStats[]>([]);
  const [timeSeriesData, setTimeSeriesData] = useState<TimeSeriesData[]>([]);
  const [trendingFlows, setTrendingFlows] = useState<TrendingFlow[]>([]);
  const [activeTab, setActiveTab] = useState('overview');

  useEffect(() => {
    loadAnalytics();
  }, [organizationId, debateId, dateRange, selectedFlowTypes]);

  const loadAnalytics = async () => {
    setLoading(true);
    try {
      // Load different analytics data based on context
      if (debateId) {
        const debateAnalytics = await debateClient.getDebateAnalytics(debateId);
        processDebateAnalytics(debateAnalytics);
      } else if (organizationId) {
        const [stats, timeSeries, trending] = await Promise.all([
          debateClient.getFlowTypeStatistics(organizationId, dateRange[0].toISOString(), dateRange[1].toISOString()),
          debateClient.getFlowExecutionTimeSeries(organizationId, dateRange[0].toISOString(), dateRange[1].toISOString()),
          debateClient.getTrendingFlowTypes(organizationId, 10),
        ]);
        setFlowTypeStats(stats);
        setTimeSeriesData(timeSeries);
        setTrendingFlows(trending);
      }
    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  const processDebateAnalytics = (analytics: any) => {
    // Transform debate-specific analytics to component state
    const stats = Object.entries(analytics.flowTypeSummaries).map(([flowType, summary]: [string, any]) => ({
      flowType,
      executionCount: summary.executionCount,
      averageConfidence: summary.averageConfidence,
      successRate: summary.successRate,
      averageExecutionTime: summary.averageExecutionTime,
    }));
    setFlowTypeStats(stats);
  };

  const renderOverviewMetrics = () => {
    const totalExecutions = flowTypeStats.reduce((sum, stat) => sum + stat.executionCount, 0);
    const avgConfidence = flowTypeStats.length > 0
      ? flowTypeStats.reduce((sum, stat) => sum + stat.averageConfidence * stat.executionCount, 0) / totalExecutions
      : 0;
    const avgSuccessRate = flowTypeStats.length > 0
      ? flowTypeStats.reduce((sum, stat) => sum + stat.successRate * stat.executionCount, 0) / totalExecutions
      : 0;

    return (
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Total Executions"
              value={totalExecutions}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Average Confidence"
              value={avgConfidence}
              precision={1}
              suffix="%"
              prefix={<AimOutlined />}
              valueStyle={{ color: avgConfidence >= 80 ? '#52c41a' : '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Success Rate"
              value={avgSuccessRate * 100}
              precision={1}
              suffix="%"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: avgSuccessRate >= 0.8 ? '#52c41a' : '#faad14' }}
            />
          </Card>
        </Col>
      </Row>
    );
  };

  const renderFlowTypeDistribution = () => {
    const data = flowTypeStats.map(stat => ({
      name: getFlowTypeLabel(stat.flowType),
      value: stat.executionCount,
      flowType: stat.flowType,
    }));

    return (
      <Card title="Flow Type Distribution">
        <ResponsiveContainer width="100%" height={height}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={FLOW_TYPE_COLORS[entry.flowType] || '#8884d8'} />
              ))}
            </Pie>
            <Tooltip />
          </PieChart>
        </ResponsiveContainer>
      </Card>
    );
  };

  const renderPerformanceComparison = () => {
    const data = flowTypeStats.map(stat => ({
      flowType: getFlowTypeLabel(stat.flowType),
      confidence: stat.averageConfidence,
      successRate: stat.successRate * 100,
      speed: Math.max(0, 100 - (stat.averageExecutionTime / 1000)), // Convert to speed score
    }));

    return (
      <Card title="Performance Comparison">
        <ResponsiveContainer width="100%" height={height}>
          <RadarChart data={data}>
            <PolarGrid />
            <PolarAngleAxis dataKey="flowType" />
            <PolarRadiusAxis angle={90} domain={[0, 100]} />
            <Radar name="Confidence" dataKey="confidence" stroke="#1890ff" fill="#1890ff" fillOpacity={0.6} />
            <Radar name="Success Rate" dataKey="successRate" stroke="#52c41a" fill="#52c41a" fillOpacity={0.6} />
            <Radar name="Speed" dataKey="speed" stroke="#fa8c16" fill="#fa8c16" fillOpacity={0.6} />
            <Legend />
          </RadarChart>
        </ResponsiveContainer>
      </Card>
    );
  };

  const renderTimeSeriesChart = () => {
    if (timeSeriesData.length === 0) {
      return (
        <Card title="Execution Trends">
          <Empty description="No time series data available" />
        </Card>
      );
    }

    return (
      <Card title="Execution Trends">
        <ResponsiveContainer width="100%" height={height}>
          <LineChart data={timeSeriesData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis yAxisId="left" />
            <YAxis yAxisId="right" orientation="right" />
            <Tooltip />
            <Legend />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="executions"
              stroke="#1890ff"
              name="Executions"
            />
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="averageConfidence"
              stroke="#52c41a"
              name="Avg Confidence %"
            />
          </LineChart>
        </ResponsiveContainer>
      </Card>
    );
  };

  const renderTrendingFlows = () => {
    const columns = [
      {
        title: 'Flow Type',
        dataIndex: 'flowType',
        key: 'flowType',
        render: (flowType: string) => (
          <Space>
            <Tag color={FLOW_TYPE_COLORS[flowType] || 'default'}>
              {getFlowTypeLabel(flowType)}
            </Tag>
          </Space>
        ),
      },
      {
        title: 'Trend',
        dataIndex: 'trendCategory',
        key: 'trendCategory',
        render: (category: string, record: TrendingFlow) => {
          const color = category === 'Hot' ? 'red' : category === 'Rising' ? 'orange' : 'blue';
          const icon = record.trendScore >= 0.6 ? <TrendingUpOutlined /> : <TrendingDownOutlined />;
          return (
            <Tag color={color} icon={icon}>
              {category}
            </Tag>
          );
        },
      },
      {
        title: 'Usage',
        dataIndex: 'usageCount',
        key: 'usageCount',
        sorter: (a, b) => a.usageCount - b.usageCount,
      },
      {
        title: 'Confidence',
        dataIndex: 'averageConfidence',
        key: 'averageConfidence',
        render: (confidence: number) => (
          <Progress
            percent={confidence}
            size="small"
            strokeColor={confidence >= 80 ? '#52c41a' : '#faad14'}
            format={(percent) => `${percent}%`}
          />
        ),
      },
      {
        title: 'Success Rate',
        dataIndex: 'successRate',
        key: 'successRate',
        render: (rate: number) => `${(rate * 100).toFixed(1)}%`,
      },
    ];

    return (
      <Card
        title={
          <Space>
            <FireOutlined style={{ color: '#fa8c16' }} />
            Trending Flow Types
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={trendingFlows}
          rowKey="flowType"
          pagination={false}
          size="small"
        />
      </Card>
    );
  };

  if (loading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
          <div style={{ marginTop: '16px' }}>Loading analytics...</div>
        </div>
      </Card>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <Title level={4} style={{ margin: 0 }}>
            <BulbOutlined style={{ marginRight: 8 }} />
            Agentic Flow Analytics
          </Title>
          <Space>
            <Select
              mode="multiple"
              placeholder="Filter by flow types"
              style={{ minWidth: 200 }}
              value={selectedFlowTypes}
              onChange={setSelectedFlowTypes}
              allowClear
            >
              {Object.keys(FLOW_TYPE_COLORS).map(flowType => (
                <Select.Option key={flowType} value={flowType}>
                  {getFlowTypeLabel(flowType)}
                </Select.Option>
              ))}
            </Select>
            <RangePicker
              value={dateRange}
              onChange={(dates) => dates && setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs])}
              format="YYYY-MM-DD"
            />
          </Space>
        </div>

        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="Overview" key="overview">
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              {renderOverviewMetrics()}
              <Row gutter={[16, 16]}>
                <Col xs={24} lg={12}>
                  {renderFlowTypeDistribution()}
                </Col>
                <Col xs={24} lg={12}>
                  {renderPerformanceComparison()}
                </Col>
              </Row>
            </Space>
          </TabPane>

          <TabPane tab="Trends" key="trends">
            <Space direction="vertical" size="large" style={{ width: '100%' }}>
              {renderTimeSeriesChart()}
              {renderTrendingFlows()}
            </Space>
          </TabPane>

          <TabPane tab="Performance" key="performance">
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Card title="Execution Time Analysis">
                  <ResponsiveContainer width="100%" height={height}>
                    <BarChart data={flowTypeStats}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="flowType" tickFormatter={getFlowTypeLabel} />
                      <YAxis />
                      <Tooltip />
                      <Bar dataKey="averageExecutionTime" fill="#1890ff" name="Avg Execution Time (ms)" />
                    </BarChart>
                  </ResponsiveContainer>
                </Card>
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default AgenticFlowAnalytics;