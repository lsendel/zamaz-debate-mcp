import React, { useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Paper,
  Typography,
  Chip,
  IconButton,
  Button,
  Card,
  CardContent,
  Avatar,
  Divider,
  LinearProgress,
  Tooltip,
  Grid,
} from "@mui/material";
import {
  ArrowBack as ArrowBackIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Stop as StopIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { useAppSelector, useAppDispatch } from "../store";
import {
  fetchDebate,
  startDebate,
  pauseDebate,
  cancelDebate,
  connectToDebate,
  disconnectFromDebate,
} from "../store/slices/debateSlice";
import { addNotification } from "../store/slices/uiSlice";
import debateClient from "../api/debateClient";

const DebateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { currentDebate, loading, isConnected } = useAppSelector(
    (state) => state.debate,
  );

  useEffect(() => {
    if (id) {
      dispatch(fetchDebate(id));
      dispatch(connectToDebate(id));
    }

    return () => {
      dispatch(disconnectFromDebate());
    };
  }, [id, dispatch]);

  const handleExport = async (format: "json" | "pdf" | "markdown") => {
    if (!currentDebate) return;

    try {
      const blob = await debateClient.exportDebate(currentDebate.id, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `debate-${currentDebate.id}.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      dispatch(
        addNotification({
          type: "success",
          message: `Debate exported as ${format.toUpperCase()}`,
        }),
      );
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message: "Failed to export debate",
        }),
      );
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "created":
        return "default";
      case "in_progress":
        return "primary";
      case "completed":
        return "success";
      case "cancelled":
        return "error";
      default:
        return "default";
    }
  };

  const getParticipantColor = (index: number) => {
    const colors = ["#1976d2", "#dc004e", "#388e3c", "#f57c00", "#7b1fa2"];
    return colors[index % colors.length];
  };

  if (loading || !currentDebate) {
    return <LinearProgress />;
  }

  return (
    <Box>
      <Box sx={{ display: "flex", alignItems: "center", mb: 3 }}>
        <IconButton onClick={() => navigate("/debates")} sx={{ mr: 2 }}>
          <ArrowBackIcon />
        </IconButton>
        <Typography variant="h4" component="h1" sx={{ flexGrow: 1 }}>
          {currentDebate.topic}
        </Typography>
        <Box sx={{ display: "flex", gap: 1 }}>
          <Chip
            label={currentDebate.status.replace("_", " ")}
            color={getStatusColor(currentDebate.status) as any}
          />
          <Chip
            label={`Round ${currentDebate.currentRound}/${currentDebate.maxRounds}`}
            variant="outlined"
          />
          {isConnected && (
            <Chip
              label="Live"
              color="success"
              size="small"
              sx={{
                animation: "pulse 2s infinite",
                "@keyframes pulse": {
                  "0%": { opacity: 1 },
                  "50%": { opacity: 0.5 },
                  "100%": { opacity: 1 },
                },
              }}
            />
          )}
        </Box>
      </Box>

      {currentDebate.description && (
        <Paper sx={{ p: 2, mb: 3 }}>
          <Typography variant="body1">{currentDebate.description}</Typography>
        </Paper>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
              <Typography variant="h6" sx={{ flexGrow: 1 }}>
                Debate Progress
              </Typography>
              <Box sx={{ display: "flex", gap: 1 }}>
                {currentDebate.status === "created" && (
                  <Button
                    startIcon={<PlayIcon />}
                    variant="contained"
                    color="primary"
                    onClick={() => dispatch(startDebate(currentDebate.id))}
                  >
                    Start
                  </Button>
                )}
                {currentDebate.status === "in_progress" && (
                  <Button
                    startIcon={<PauseIcon />}
                    variant="outlined"
                    onClick={() => dispatch(pauseDebate(currentDebate.id))}
                  >
                    Pause
                  </Button>
                )}
                {(currentDebate.status === "created" ||
                  currentDebate.status === "in_progress") && (
                  <Button
                    startIcon={<StopIcon />}
                    variant="outlined"
                    color="error"
                    onClick={() => dispatch(cancelDebate(currentDebate.id))}
                  >
                    Cancel
                  </Button>
                )}
                <Tooltip title="Refresh">
                  <IconButton
                    onClick={() => dispatch(fetchDebate(currentDebate.id))}
                  >
                    <RefreshIcon />
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>

            <Box sx={{ maxHeight: 600, overflowY: "auto" }}>
              {currentDebate.rounds.map((round) => (
                <Box key={round.roundNumber} sx={{ mb: 3 }}>
                  <Typography variant="subtitle1" sx={{ mb: 2 }}>
                    Round {round.roundNumber}
                  </Typography>
                  {round.responses.map((response) => {
                    const participant = currentDebate.participants.find(
                      (p) => p.id === response.participantId,
                    );
                    const participantIndex =
                      currentDebate.participants.findIndex(
                        (p) => p.id === response.participantId,
                      );
                    return (
                      <Card key={response.id} sx={{ mb: 2 }}>
                        <CardContent>
                          <Box
                            sx={{
                              display: "flex",
                              alignItems: "center",
                              mb: 1,
                            }}
                          >
                            <Avatar
                              sx={{
                                bgcolor: getParticipantColor(participantIndex),
                                width: 32,
                                height: 32,
                                mr: 1,
                              }}
                            >
                              {participant?.name.charAt(0)}
                            </Avatar>
                            <Typography
                              variant="subtitle2"
                              sx={{ flexGrow: 1 }}
                            >
                              {participant?.name}
                            </Typography>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                            >
                              {new Date(
                                response.timestamp,
                              ).toLocaleTimeString()}
                            </Typography>
                          </Box>
                          <Typography variant="body2">
                            {response.content}
                          </Typography>
                          {response.tokenCount && (
                            <Typography
                              variant="caption"
                              color="text.secondary"
                            >
                              {response.tokenCount} tokens
                            </Typography>
                          )}
                        </CardContent>
                      </Card>
                    );
                  })}
                </Box>
              ))}
            </Box>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Participants
            </Typography>
            {currentDebate.participants.map((participant, index) => (
              <Box key={participant.id} sx={{ mb: 2 }}>
                <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
                  <Avatar
                    sx={{
                      bgcolor: getParticipantColor(index),
                      width: 40,
                      height: 40,
                      mr: 2,
                    }}
                  >
                    {participant.name.charAt(0)}
                  </Avatar>
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography variant="subtitle1">
                      {participant.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {participant.llmProvider} - {participant.model}
                    </Typography>
                  </Box>
                </Box>
                {participant.systemPrompt && (
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ ml: 7 }}
                  >
                    {participant.systemPrompt}
                  </Typography>
                )}
                <Divider sx={{ mt: 2 }} />
              </Box>
            ))}
          </Paper>

          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Export
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
              <Button
                startIcon={<DownloadIcon />}
                onClick={() => handleExport("json")}
                fullWidth
              >
                Export as JSON
              </Button>
              <Button
                startIcon={<DownloadIcon />}
                onClick={() => handleExport("markdown")}
                fullWidth
              >
                Export as Markdown
              </Button>
              <Button
                startIcon={<DownloadIcon />}
                onClick={() => handleExport("pdf")}
                fullWidth
              >
                Export as PDF
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DebateDetailPage;
