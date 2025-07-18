import React from "react";
import {
  Box,
  Grid,
  Paper,
  Typography,
  Card,
  CardContent,
  LinearProgress,
} from "@mui/material";
import {
  TrendingUp as TrendingUpIcon,
  Forum as ForumIcon,
  Timer as TimerIcon,
  Group as GroupIcon,
} from "@mui/icons-material";
import { useAppSelector } from "../store";

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon, color }) => (
  <Card>
    <CardContent>
      <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
        <Box
          sx={{
            p: 1,
            borderRadius: 1,
            bgcolor: `${color}.light`,
            color: `${color}.main`,
            mr: 2,
          }}
        >
          {icon}
        </Box>
        <Typography variant="h6" component="div">
          {value}
        </Typography>
      </Box>
      <Typography variant="body2" color="text.secondary">
        {title}
      </Typography>
    </CardContent>
  </Card>
);

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
    <Box>
      <Typography variant="h4" component="h1" gutterBottom>
        Analytics
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Organization: {currentOrganization?.name}
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title="Total Debates"
            value={totalDebates}
            icon={<ForumIcon />}
            color="primary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title="Completed"
            value={completedDebates}
            icon={<TrendingUpIcon />}
            color="success"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title="In Progress"
            value={inProgressDebates}
            icon={<TimerIcon />}
            color="warning"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard
            title="Avg. Participants"
            value={avgParticipants}
            icon={<GroupIcon />}
            color="info"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Debate Status Distribution
            </Typography>
            <Box sx={{ mt: 3 }}>
              {["created", "in_progress", "completed", "cancelled"].map(
                (status) => {
                  const count = debates.filter(
                    (d) => d.status === status,
                  ).length;
                  const percentage =
                    totalDebates > 0 ? (count / totalDebates) * 100 : 0;
                  return (
                    <Box key={status} sx={{ mb: 2 }}>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mb: 1,
                        }}
                      >
                        <Typography variant="body2">
                          {status.replace("_", " ").charAt(0).toUpperCase() +
                            status.slice(1).replace("_", " ")}
                        </Typography>
                        <Typography variant="body2">{count}</Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={percentage}
                        sx={{ height: 8, borderRadius: 1 }}
                      />
                    </Box>
                  );
                },
              )}
            </Box>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            <Box sx={{ mt: 3 }}>
              {debates.slice(0, 5).map((debate) => (
                <Box key={debate.id} sx={{ mb: 2 }}>
                  <Typography variant="body2" gutterBottom>
                    {debate.topic}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {new Date(debate.createdAt).toLocaleDateString()} -{" "}
                    {debate.status.replace("_", " ")}
                  </Typography>
                </Box>
              ))}
              {debates.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No debates yet
                </Typography>
              )}
            </Box>
          </Paper>
        </Grid>
      </Grid>

      <Paper sx={{ p: 3, mt: 3 }}>
        <Typography variant="h6" gutterBottom>
          Performance Metrics
        </Typography>
        <Grid container spacing={3} sx={{ mt: 1 }}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="subtitle2" color="text.secondary">
              Average Debate Duration
            </Typography>
            <Typography variant="h5">
              {completedDebates > 0 ? "N/A" : "-"}
            </Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="subtitle2" color="text.secondary">
              Average Rounds per Debate
            </Typography>
            <Typography variant="h5">{avgRounds}</Typography>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <Typography variant="subtitle2" color="text.secondary">
              Success Rate
            </Typography>
            <Typography variant="h5">
              {totalDebates > 0
                ? `${((completedDebates / totalDebates) * 100).toFixed(1)}%`
                : "-"}
            </Typography>
          </Grid>
        </Grid>
      </Paper>
    </Box>
  );
};

export default AnalyticsPage;
