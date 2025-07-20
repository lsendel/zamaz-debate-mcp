import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Avatar,
  Divider,
  Progress,
  Tooltip,
  TooltipProvider,
  Badge,
  Toast,
  ToastProvider,
  ToastViewport,
  ToastClose,
  ToastDescription,
  Alert,
} from "@zamaz/ui";
import {
  ArrowLeft,
  Play,
  Pause,
  StopCircle,
  Download,
  RefreshCw,
} from "lucide-react";
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

  const getStatusVariant = (status: string): "default" | "primary" | "success" | "error" => {
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
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
      </div>
    );
  }

  return (
    <TooltipProvider>
      <ToastProvider>
        <div className="space-y-6">
          <div className="flex items-center mb-6">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigate("/debates")}
              className="mr-4"
            >
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <h1 className="text-3xl font-bold flex-1">
              {currentDebate.topic}
            </h1>
            <div className="flex gap-2">
              <Badge variant={getStatusVariant(currentDebate.status)}>
                {currentDebate.status.replace("_", " ")}
              </Badge>
              <Badge variant="outline">
                Format: {currentDebate.format || 'OXFORD'}
              </Badge>
              {(isPolling || isConnected) && (
                <Badge variant="success" className="animate-pulse">
                  Live
                </Badge>
              )}
            </div>
          </div>

      {currentDebate.description && (
            <Card className="mb-6">
              <CardContent>
                <p className="text-gray-700">{currentDebate.description}</p>
              </CardContent>
            </Card>
          )}

      {/* Add Debate Progress Component */}
          {currentDebate && (
            <DebateProgress debate={currentDebate} isPolling={isPolling} />
          )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
              <Card>
                <CardHeader>
                  <div className="flex items-center justify-between">
                    <CardTitle>Debate Responses</CardTitle>
                    <div className="flex gap-2">
                {currentDebate.status === "CREATED" && (
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => dispatch(startDebate(currentDebate.id))}
                          leftIcon={<Play className="h-4 w-4" />}
                        >
                          Start
                        </Button>
                      )}
                      {currentDebate.status === "IN_PROGRESS" && (
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => dispatch(pauseDebate(currentDebate.id))}
                          leftIcon={<Pause className="h-4 w-4" />}
                        >
                          Pause
                        </Button>
                      )}
                      {(currentDebate.status === "CREATED" ||
                        currentDebate.status === "IN_PROGRESS") && (
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => dispatch(cancelDebate(currentDebate.id))}
                          leftIcon={<StopCircle className="h-4 w-4" />}
                        >
                          Cancel
                        </Button>
                      )}
                      <Tooltip content="Refresh">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => dispatch(fetchDebate(currentDebate.id))}
                        >
                          <RefreshCw className="h-4 w-4" />
                        </Button>
                      </Tooltip>
              </div>
                  </div>
                </CardHeader>

            <CardContent className="max-h-[600px] overflow-y-auto">
                  {currentDebate.rounds && currentDebate.rounds.length > 0 ? (
                currentDebate.rounds.map((round) => (
                      <div key={round.roundNumber} className="mb-6">
                        <h3 className="text-lg font-semibold mb-3">
                          Round {round.roundNumber}
                        </h3>
                    {round.responses.map((response) => {
                          const participant = currentDebate.participants.find(
                            (p) => p.id === response.participantId,
                          );
                          const participantIndex =
                            currentDebate.participants.findIndex(
                              (p) => p.id === response.participantId,
                            );
                          return (
                            <Card key={response.id} className="mb-3">
                              <CardContent>
                                <div className="flex items-center mb-2">
                                  <Avatar
                                    size="sm"
                                    style={{
                                      backgroundColor: getParticipantColor(participantIndex),
                                    }}
                                    className="mr-2"
                                  >
                                    {participant?.name.charAt(0)}
                                  </Avatar>
                                  <div className="flex-1">
                                    <p className="font-medium">
                                      {participant?.name}
                                    </p>
                                  </div>
                                  <p className="text-sm text-gray-500">
                                    {new Date(
                                      response.timestamp,
                                    ).toLocaleTimeString()}
                                  </p>
                                </div>
                                <p className="text-gray-700">
                                  {response.content}
                                </p>
                                {response.tokenCount && (
                                  <p className="text-sm text-gray-500 mt-2">
                                    {response.tokenCount} tokens
                                  </p>
                                )}
                              </CardContent>
                            </Card>
                          );
                        })}
                  </div>
                    ))
              ) : (
                    <div className="text-center py-8">
                      <h3 className="text-lg font-medium text-gray-500 mb-2">
                        No debate rounds yet
                      </h3>
                      <p className="text-sm text-gray-400">
                        {currentDebate.status === 'CREATED' ? 'Start the debate to see rounds' : 'This debate has no recorded rounds'}
                      </p>
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>

        <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Participants</CardTitle>
                </CardHeader>
                <CardContent>
            {currentDebate.participants && Array.isArray(currentDebate.participants) ? (
                    currentDebate.participants.map((participant, index) => {
                      // Handle both string and object participants
                      const isString = typeof participant === 'string';
                      const name = isString ? participant : participant.name;
                      const llmProvider = isString ? 'Unknown' : participant.llmProvider;
                      const model = isString ? participant : participant.model;
                      
                      return (
                        <div key={isString ? participant : participant.id} className="mb-4">
                          <div className="flex items-center mb-2">
                            <Avatar
                              size="md"
                              style={{
                                backgroundColor: getParticipantColor(index),
                              }}
                              className="mr-3"
                            >
                              {name.charAt(0)}
                            </Avatar>
                            <div className="flex-1">
                              <p className="font-medium">
                                {name}
                              </p>
                              <p className="text-sm text-gray-500">
                                {isString ? `Model: ${participant}` : `${llmProvider} - ${model}`}
                              </p>
                            </div>
                          </div>
                          {!isString && participant.systemPrompt && (
                            <p className="text-sm text-gray-600 ml-12">
                              {participant.systemPrompt}
                            </p>
                          )}
                          {index < currentDebate.participants.length - 1 && (
                            <Divider className="mt-4" />
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <p className="text-sm text-gray-500">
                      No participants found
                    </p>
                  )}
                </CardContent>
              </Card>

          <Card>
                <CardHeader>
                  <CardTitle>Export</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                  <Button
                    variant="secondary"
                    onClick={() => handleExport("json")}
                    leftIcon={<Download className="h-4 w-4" />}
                    className="w-full"
                  >
                    Export as JSON
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => handleExport("markdown")}
                    leftIcon={<Download className="h-4 w-4" />}
                    className="w-full"
                  >
                    Export as Markdown
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => handleExport("pdf")}
                    leftIcon={<Download className="h-4 w-4" />}
                    className="w-full"
                  >
                    Export as PDF
                  </Button>
                </CardContent>
              </Card>
            </div>
          </div>
      
      {/* Update Notification */}
          {showUpdateNotification && (
            <Toast
              open={showUpdateNotification}
              onOpenChange={setShowUpdateNotification}
              variant="default"
            >
              <ToastDescription>New round added to the debate!</ToastDescription>
              <ToastClose />
            </Toast>
          )}
          <ToastViewport />
        </div>
      </ToastProvider>
    </TooltipProvider>
  );
};

export default DebateDetailPage;
