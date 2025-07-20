import React from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Progress,
} from "@zamaz/ui";
import {
  TrendingUp,
  MessageSquare,
  Clock,
  Users,
} from "lucide-react";
import { useAppSelector } from "../store";

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color }) => {
  const colorClasses = {
    primary: "bg-primary-100 text-primary-700",
    success: "bg-green-100 text-green-700",
    warning: "bg-yellow-100 text-yellow-700",
    info: "bg-blue-100 text-blue-700",
  };

  return (
    <Card>
      <CardContent>
        <div className="flex items-center mb-3">
          <div
            className={`p-2 rounded-lg ${colorClasses[color as keyof typeof colorClasses]} mr-3`}
          >
            {icon}
          </div>
          <div className="text-2xl font-semibold">
            {value}
          </div>
        </div>
        <p className="text-sm text-gray-600">
          {title}
        </p>
      </CardContent>
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
      <h1 className="text-3xl font-bold mb-2">Analytics</h1>
      <p className="text-gray-600 mb-6">
        Organization: {currentOrganization?.name}
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Total Debates"
          value={totalDebates}
          icon={<MessageSquare className="h-5 w-5" />}
          color="primary"
        />
        <StatCard
          title="Completed"
          value={completedDebates}
          icon={<TrendingUp className="h-5 w-5" />}
          color="success"
        />
        <StatCard
          title="In Progress"
          value={inProgressDebates}
          icon={<Clock className="h-5 w-5" />}
          color="warning"
        />
        <StatCard
          title="Avg. Participants"
          value={avgParticipants}
          icon={<Users className="h-5 w-5" />}
          color="info"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <Card>
          <CardHeader>
            <CardTitle>Debate Status Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
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
                      <div className="flex justify-between mb-1">
                        <span className="text-sm font-medium">
                          {statusLabels[status as keyof typeof statusLabels]}
                        </span>
                        <span className="text-sm text-gray-600">{count}</span>
                      </div>
                      <Progress
                        value={percentage}
                        variant={
                          status === "completed" ? "success" :
                          status === "in_progress" ? "warning" :
                          status === "cancelled" ? "error" :
                          "default"
                        }
                        showLabel={false}
                      />
                    </div>
                  );
                },
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {debates.slice(0, 5).map((debate) => (
                <div key={debate.id} className="border-b border-gray-100 pb-3 last:border-0">
                  <p className="font-medium text-sm">
                    {debate.topic}
                  </p>
                  <p className="text-xs text-gray-500">
                    {new Date(debate.createdAt).toLocaleDateString()} -{" "}
                    {debate.status.replace("_", " ")}
                  </p>
                </div>
              ))}
              {debates.length === 0 && (
                <p className="text-sm text-gray-500">
                  No debates yet
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Performance Metrics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <p className="text-sm text-gray-600 mb-1">
                Average Debate Duration
              </p>
              <p className="text-2xl font-semibold">
                {completedDebates > 0 ? "N/A" : "-"}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-600 mb-1">
                Average Rounds per Debate
              </p>
              <p className="text-2xl font-semibold">{avgRounds}</p>
            </div>
            <div>
              <p className="text-sm text-gray-600 mb-1">
                Success Rate
              </p>
              <p className="text-2xl font-semibold">
                {totalDebates > 0
                  ? `${((completedDebates / totalDebates) * 100).toFixed(1)}%`
                  : "-"}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AnalyticsPage;