import React, { useEffect, useState } from "react";
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
  Snackbar,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid2";
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
import { useDebatePolling } from "../hooks/useDebatePolling";
import DebateProgress from "./DebateProgress";

const DebateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { currentDebate, loading, isConnected } = useAppSelector(
    (state) => state.debate,
  );
  const [showUpdateNotification, setShowUpdateNotification] = useState(false);
  const [lastRoundCount, setLastRoundCount] = useState(0);

  useEffect(() => {
    if (id) {
      dispatch(fetchDebate(id));
      // Only connect to WebSocket for active debates
      // For now, disable WebSocket since our simple service doesn't support it
      // dispatch(connectToDebate(id));
    }

    return () => {
      dispatch(disconnectFromDebate());
    };
  }, [id, dispatch]);

  // Use polling for active debates
  const { isPolling } = useDebatePolling({
    debateId: id || '',
    enabled: !!id && !!currentDebate && 
             (currentDebate.status === 'IN_PROGRESS' || currentDebate.status === 'CREATED'),
    interval: 2000,
    onUpdate: (debate) => {
      // Show notification when new rounds are added
      const newRoundCount = debate.rounds?.length || 0;
      if (newRoundCount > lastRoundCount) {
        setShowUpdateNotification(true);
        setLastRoundCount(newRoundCount);
      }
    }
  });

  useEffect(() => {
    if (currentDebate) {
      setLastRoundCount(currentDebate.rounds?.length || 0);
    }
  }, [currentDebate]);

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
    switch (status?.toUpperCase()) {
      case "CREATED":
        return "default";
      case "IN_PROGRESS":
        return "primary";
      case "COMPLETED":
        return "success";
      case "CANCELLED":
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
            label={`Format: ${currentDebate.format || 'OXFORD'}`}
            variant="outlined"
          />
          {(isPolling || isConnected) && (
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

      {/* Add Debate Progress Component */}
      {currentDebate && (
        <DebateProgress debate={currentDebate} isPolling={isPolling} />
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
              <Typography variant="h6" sx={{ flexGrow: 1 }}>
                Debate Responses
              </Typography>
              <Box sx={{ display: "flex", gap: 1 }}>
                {currentDebate.status === "CREATED" && (
                  <Button
                    startIcon={<PlayIcon />}
                    variant="contained"
                    color="primary"
                    onClick={() => dispatch(startDebate(currentDebate.id))}
                  >
                    Start
                  </Button>
                )}
                {currentDebate.status === "IN_PROGRESS" && (
                  <Button
                    startIcon={<PauseIcon />}
                    variant="outlined"
                    onClick={() => dispatch(pauseDebate(currentDebate.id))}
                  >
                    Pause
                  </Button>
                )}
                {(currentDebate.status === "CREATED" ||
                  currentDebate.status === "IN_PROGRESS") && (
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
              {currentDebate.rounds && currentDebate.rounds.length > 0 ? (
                currentDebate.rounds.map((round) => (
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
                ))
              ) : (
                <Box sx={{ textAlign: 'center', py: 4 }}>
                  <Typography variant="h6" color="text.secondary">
                    No debate rounds yet
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {currentDebate.status === 'CREATED' ? 'Start the debate to see rounds' : 'This debate has no recorded rounds'}
                  </Typography>
                </Box>
              )}
            </Box>
          </Paper>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Participants
            </Typography>
            {currentDebate.participants && Array.isArray(currentDebate.participants) ? (
              currentDebate.participants.map((participant, index) => {
                // Handle both string and object participants
                const isString = typeof participant === 'string';
                const name = isString ? participant : participant.name;
                const llmProvider = isString ? 'Unknown' : participant.llmProvider;
                const model = isString ? participant : participant.model;
                
                return (
                  <Box key={isString ? participant : participant.id} sx={{ mb: 2 }}>
                    <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
                      <Avatar
                        sx={{
                          bgcolor: getParticipantColor(index),
                          width: 40,
                          height: 40,
                          mr: 2,
                        }}
                      >
                        {name.charAt(0)}
                      </Avatar>
                      <Box sx={{ flexGrow: 1 }}>
                        <Typography variant="subtitle1">
                          {name}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {isString ? `Model: ${participant}` : `${llmProvider} - ${model}`}
                        </Typography>
                      </Box>
                    </Box>
                    {!isString && participant.systemPrompt && (
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
                );
              })
            ) : (
              <Typography variant="body2" color="text.secondary">
                No participants found
              </Typography>
            )}
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
      
      {/* Update Notification */}
      <Snackbar
        open={showUpdateNotification}
        autoHideDuration={3000}
        onClose={() => setShowUpdateNotification(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setShowUpdateNotification(false)}
          severity="info"
          sx={{ width: '100%' }}
        >
          New round added to the debate!
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default DebateDetailPage;
