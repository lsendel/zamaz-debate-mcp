import React from "react";
import {
  Card,
  Progress,
} from "antd";
import {
  RiseOutlined,
  MessageOutlined,
  ClockCircleOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { useAppSelector } from "../store";

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color }) => {
  const colorStyles = {
    primary: { background: '#e6f4ff', color: '#1677ff' },
    success: { background: '#f6ffed', color: '#52c41a' },
    warning: { background: '#fffbe6', color: '#faad14' },
    info: { background: '#e6f7ff', color: '#1890ff' },
  };

  return (
    <Card>
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '12px' }}>
        <div
          style={{
            padding: '8px',
            borderRadius: '8px',
            marginRight: '12px',
            ...colorStyles[color as keyof typeof colorStyles]
          }}
        >
          {icon}
        </div>
        <div style={{ fontSize: '24px', fontWeight: '600' }}>
          {value}
        </div>
      </div>
      <p style={{ fontSize: '14px', color: '#666', margin: 0 }}>
        {title}
      </p>
    </Card>
  );
};

const AnalyticsPage: React.FC = () => {
  const { debates } = useAppSelector((state) => state.debate);
  const { currentOrganization } = useAppSelector((state) => state.organization);

  // Calculate statistics
  const totalDebates = debates.length;
  const completedDebates = debates.filter(
    (d) => d.status === "completed",
  ).length;
  const inProgressDebates = debates.filter(
    (d) => d.status === "in_progress",
  ).length;
  const totalParticipants = debates.reduce(
    (acc, d) => acc + d.participants.length,
    0,
  );
  const avgParticipants =
    totalDebates > 0 ? (totalParticipants / totalDebates).toFixed(1) : 0;
  const avgRounds =
    totalDebates > 0
      ? (
          debates.reduce((acc, d) => acc + d.maxRounds, 0) / totalDebates
        ).toFixed(1)
      : 0;

  return (
    <div>
      <h1 style={{ fontSize: '30px', fontWeight: 'bold', marginBottom: '8px' }}>Analytics</h1>
      <p style={{ color: '#666', marginBottom: '24px' }}>
        Organization: {currentOrganization?.name}
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '24px', marginBottom: '32px' }}>
        <StatCard
          title="Total Debates"
          value={totalDebates}
          icon={<MessageOutlined style={{ fontSize: '20px' }} />}
          color="primary"
        />
        <StatCard
          title="Completed"
          value={completedDebates}
          icon={<RiseOutlined style={{ fontSize: '20px' }} />}
          color="success"
        />
        <StatCard
          title="In Progress"
          value={inProgressDebates}
          icon={<ClockCircleOutlined style={{ fontSize: '20px' }} />}
          color="warning"
        />
        <StatCard
          title="Avg. Participants"
          value={avgParticipants}
          icon={<UserOutlined style={{ fontSize: '20px' }} />}
          color="info"
        />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '24px', marginBottom: '32px' }}>
        <Card title="Debate Status Distribution">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {["created", "in_progress", "completed", "cancelled"].map(
                (status) => {
                  const count = debates.filter(
                    (d) => d.status === status,
                  ).length;
                  const percentage =
                    totalDebates > 0 ? (count / totalDebates) * 100 : 0;
                  
                  const statusLabels = {
                    created: "Created",
                    in_progress: "In Progress",
                    completed: "Completed",
                    cancelled: "Cancelled"
                  };
                  
                  return (
                    <div key={status}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                        <span style={{ fontSize: '14px', fontWeight: '500' }}>
                          {statusLabels[status as keyof typeof statusLabels]}
                        </span>
                        <span style={{ fontSize: '14px', color: '#666' }}>{count}</span>
                      </div>
                      <Progress
                        percent={percentage}
                        strokeColor={
                          status === "completed" ? '#52c41a' :
                          status === "in_progress" ? '#faad14' :
                          status === "cancelled" ? '#ff4d4f' :
                          '#1677ff'
                        }
                        showInfo={false}
                      />
                    </div>
                  );
                },
              )}
          </div>
        </Card>

        <Card title="Recent Activity">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {debates.slice(0, 5).map((debate) => (
                <div key={debate.id} style={{ paddingBottom: '12px', borderBottom: '1px solid #f0f0f0' }}>
                  <p style={{ fontWeight: '500', fontSize: '14px', margin: '0 0 4px 0' }}>
                    {debate.topic}
                  </p>
                  <p style={{ fontSize: '12px', color: '#999', margin: 0 }}>
                    {new Date(debate.createdAt).toLocaleDateString()} -{" "}
                    {debate.status.replace("_", " ")}
                  </p>
                </div>
              ))}
              {debates.length === 0 && (
                <p style={{ fontSize: '14px', color: '#999' }}>
                  No debates yet
                </p>
              )}
          </div>
        </Card>
      </div>

      <Card title="Performance Metrics">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '24px' }}>
          <div>
            <p style={{ fontSize: '14px', color: '#666', marginBottom: '4px' }}>
              Average Debate Duration
            </p>
            <p style={{ fontSize: '24px', fontWeight: '600', margin: 0 }}>
              {completedDebates > 0 ? "N/A" : "-"}
            </p>
          </div>
          <div>
            <p style={{ fontSize: '14px', color: '#666', marginBottom: '4px' }}>
              Average Rounds per Debate
            </p>
            <p style={{ fontSize: '24px', fontWeight: '600', margin: 0 }}>{avgRounds}</p>
          </div>
          <div>
            <p style={{ fontSize: '14px', color: '#666', marginBottom: '4px' }}>
              Success Rate
            </p>
            <p style={{ fontSize: '24px', fontWeight: '600', margin: 0 }}>
              {totalDebates > 0
                ? `${((completedDebates / totalDebates) * 100).toFixed(1)}%`
                : "-"}
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default AnalyticsPage;