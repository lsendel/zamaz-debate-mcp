import React, { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Button,
  Card,
  Avatar,
  Divider,
  Progress,
  Tooltip,
  Badge,
  Alert,
  notification,
} from "antd";
import {
  ArrowLeftOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  StopOutlined,
  DownloadOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { useAppSelector, useAppDispatch } from "../store";
import {
  fetchDebate,
  startDebate,
  pauseDebate,
  cancelDebate,
  connectToDebate,
  disconnectFromDebate,
  clearError,
} from "../store/slices/debateSlice";
import { addNotification } from "../store/slices/uiSlice";
import debateClient from "../api/debateClient";
import { useDebatePolling } from "../hooks/useDebatePolling";
import DebateProgress from "./DebateProgress";

const DebateDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { currentDebate, loading, isConnected, error, actionLoading } = useAppSelector(
    (state) => state.debate,
  );
  const [showUpdateNotification, setShowUpdateNotification] = useState(false);
  const lastRoundCountRef = useRef(0);

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

  // Use polling for active debates (only IN_PROGRESS, not CREATED)
  const { isPolling } = useDebatePolling({
    debateId: id || '',
    enabled: !!id && !!currentDebate && 
             (currentDebate.status === 'IN_PROGRESS'),
    interval: 2000,
    onUpdate: React.useCallback((debate) => {
      // Show notification when new rounds are added
      const newRoundCount = debate.rounds?.length || 0;
      if (newRoundCount > lastRoundCountRef.current) {
        setShowUpdateNotification(true);
        lastRoundCountRef.current = newRoundCount;
      }
    }, [])
  });

  useEffect(() => {
    if (currentDebate) {
      lastRoundCountRef.current = currentDebate.rounds?.length || 0;
    }
  }, [currentDebate]);

  const handleStartDebate = async () => {
    if (!currentDebate) return;
    
    const result = await dispatch(startDebate(currentDebate.id));
    if (startDebate.rejected.match(result)) {
      notification.error({
        message: 'Failed to start debate',
        description: result.error?.message || 'The debate service is not available. Please ensure all backend services are running.',
        duration: 5,
      });
    } else if (startDebate.fulfilled.match(result)) {
      notification.success({
        message: 'Debate started successfully',
        duration: 3,
      });
    }
  };

  const handlePauseDebate = async () => {
    if (!currentDebate) return;
    
    const result = await dispatch(pauseDebate(currentDebate.id));
    if (pauseDebate.rejected.match(result)) {
      notification.error({
        message: 'Failed to pause debate',
        description: result.error?.message || 'Unable to pause the debate. Please try again.',
        duration: 5,
      });
    }
  };

  const handleCancelDebate = async () => {
    if (!currentDebate) return;
    
    const result = await dispatch(cancelDebate(currentDebate.id));
    if (cancelDebate.rejected.match(result)) {
      notification.error({
        message: 'Failed to cancel debate',
        description: result.error?.message || 'Unable to cancel the debate. Please try again.',
        duration: 5,
      });
    }
  };

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

  const getStatusColor = (status: string): string => {
    switch (status?.toUpperCase()) {
      case "CREATED":
        return "default";
      case "IN_PROGRESS":
        return "blue";
      case "COMPLETED":
        return "green";
      case "CANCELLED":
        return "red";
      default:
        return "default";
    }
  };

  const getParticipantColor = (index: number) => {
    const colors = ["#1976d2", "#dc004e", "#388e3c", "#f57c00", "#7b1fa2"];
    return colors[index % colors.length];
  };

  React.useEffect(() => {
    if (showUpdateNotification) {
      notification.info({
        message: 'New round added to the debate!',
        duration: 3,
      });
      setShowUpdateNotification(false);
    }
  }, [showUpdateNotification]);

  if (loading || !currentDebate) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '256px' }}>
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
        <div style={{ width: '32px', height: '32px', border: '3px solid #f3f3f3', borderTop: '3px solid #1677ff', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Show error alerts for various error types */}
      {error && (
        <Alert
          message={error.includes('Failed to fetch') || error.includes('Network') || error.includes('ERR_NETWORK') 
            ? "Service Connection Error" 
            : "Error"}
          description={
            error.includes('Failed to fetch') || error.includes('Network') || error.includes('ERR_NETWORK')
              ? "The debate service is not available. Please ensure all backend services are running."
              : error
          }
          type="error"
          closable
          onClose={() => dispatch(clearError())}
          style={{ marginBottom: '16px' }}
          action={
            (error.includes('Failed to fetch') || error.includes('Network') || error.includes('ERR_NETWORK')) && (
              <Button size="small" danger onClick={() => dispatch(fetchDebate(id!))}>
                Retry
              </Button>
            )
          }
        />
      )}
      <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px' }}>
        <Button
          type="text"
          size="small"
          onClick={() => navigate("/debates")}
          icon={<ArrowLeftOutlined />}
          style={{ marginRight: '16px' }}
        />
        <h1 style={{ fontSize: '30px', fontWeight: 'bold', flex: 1, margin: 0 }}>
          {currentDebate.topic}
        </h1>
        <div style={{ display: 'flex', gap: '8px' }}>
          <Badge color={getStatusColor(currentDebate.status)} text={currentDebate.status.replace("_", " ")} />
          <Badge color="default" text={`Format: ${currentDebate.format || 'OXFORD'}`} />
          {(isPolling || isConnected) && (
            <Badge color="green" text="Live" />
          )}
        </div>
      </div>

      {currentDebate.description && (
        <Card style={{ marginBottom: '24px' }}>
          <p style={{ color: '#595959', margin: 0 }}>{currentDebate.description}</p>
        </Card>
      )}

      {/* Add Debate Progress Component */}
          {currentDebate && (
            <DebateProgress debate={currentDebate} isPolling={isPolling} />
          )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '24px' }}>
        <div>
          <Card
            title="Debate Responses"
            extra={
              <div style={{ display: 'flex', gap: '8px' }}>
                {currentDebate.status === "CREATED" && (
                  <Button
                    type="primary"
                    size="small"
                    onClick={handleStartDebate}
                    loading={actionLoading.start}
                    icon={<PlayCircleOutlined />}
                  >
                    Start
                  </Button>
                )}
                {currentDebate.status === "IN_PROGRESS" && (
                  <Button
                    type="default"
                    size="small"
                    onClick={handlePauseDebate}
                    loading={actionLoading.pause}
                    icon={<PauseCircleOutlined />}
                  >
                    Pause
                  </Button>
                )}
                {(currentDebate.status === "CREATED" ||
                  currentDebate.status === "IN_PROGRESS") && (
                  <Button
                    danger
                    size="small"
                    onClick={handleCancelDebate}
                    loading={actionLoading.cancel}
                    icon={<StopOutlined />}
                  >
                    Cancel
                  </Button>
                )}
                <Tooltip title="Refresh">
                  <Button
                    type="text"
                    size="small"
                    onClick={() => dispatch(fetchDebate(currentDebate.id))}
                    icon={<ReloadOutlined />}
                  />
                </Tooltip>
              </div>
            }
          >

            <div style={{ maxHeight: '600px', overflowY: 'auto' }}>
                  {currentDebate.rounds && currentDebate.rounds.length > 0 ? (
                currentDebate.rounds.map((round) => (
                      <div key={round.roundNumber} style={{ marginBottom: '24px' }}>
                        <h3 style={{ fontSize: '18px', fontWeight: '600', marginBottom: '12px' }}>
                          Round {round.roundNumber}
                        </h3>
                    {round.responses.map((response) => {
                          // Handle both string and object participants
                          let participant;
                          let participantIndex = -1;
                          let participantName = 'Unknown';
                          
                          if (typeof currentDebate.participants[0] === 'string') {
                            // Participants are strings - use response index or participantId
                            participantIndex = parseInt(response.participantId?.split('-').pop() || '0') || 0;
                            participantName = currentDebate.participants[participantIndex] || `Participant ${participantIndex + 1}`;
                          } else {
                            // Participants are objects with id property
                            participant = currentDebate.participants.find(
                              (p) => p.id === response.participantId,
                            );
                            participantIndex = currentDebate.participants.findIndex(
                              (p) => p.id === response.participantId,
                            );
                            participantName = participant?.name || 'Unknown';
                          }
                          
                          return (
                            <Card key={response.id} style={{ marginBottom: '12px' }}>
                              <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                                <Avatar
                                  size="small"
                                  style={{
                                    backgroundColor: getParticipantColor(participantIndex),
                                    marginRight: '8px'
                                  }}
                                >
                                  {participantName.charAt(0)}
                                </Avatar>
                                <div style={{ flex: 1 }}>
                                  <p style={{ fontWeight: '500', margin: 0 }}>
                                    {participantName}
                                  </p>
                                </div>
                                <p style={{ fontSize: '14px', color: '#999', margin: 0 }}>
                                  {new Date(
                                    response.timestamp,
                                  ).toLocaleTimeString()}
                                </p>
                              </div>
                              <p style={{ color: '#595959', margin: '0 0 8px 0' }}>
                                {response.content}
                              </p>
                              {response.tokenCount && (
                                <p style={{ fontSize: '14px', color: '#999', margin: 0 }}>
                                  {response.tokenCount} tokens
                                </p>
                              )}
                            </Card>
                          );
                        })}
                  </div>
                    ))
              ) : (
                    <div style={{ textAlign: 'center', padding: '32px 0' }}>
                      <h3 style={{ fontSize: '18px', fontWeight: '500', color: '#999', marginBottom: '8px' }}>
                        No debate rounds yet
                      </h3>
                      <p style={{ fontSize: '14px', color: '#bfbfbf' }}>
                        {currentDebate.status === 'CREATED' ? 'Start the debate to see rounds' : 'This debate has no recorded rounds'}
                      </p>
                    </div>
                  )}
            </div>
          </Card>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          <Card title="Participants">
            {currentDebate.participants && Array.isArray(currentDebate.participants) ? (
                    currentDebate.participants.map((participant, index) => {
                      // Handle both string and object participants
                      const isString = typeof participant === 'string';
                      const name = isString ? participant : participant.name;
                      const llmProvider = isString ? 'Unknown' : participant.llmProvider;
                      const model = isString ? participant : participant.model;
                      
                      return (
                        <div key={isString ? participant : participant.id} style={{ marginBottom: '16px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                            <Avatar
                              style={{
                                backgroundColor: getParticipantColor(index),
                                marginRight: '12px'
                              }}
                            >
                              {name.charAt(0)}
                            </Avatar>
                            <div style={{ flex: 1 }}>
                              <p style={{ fontWeight: '500', margin: 0 }}>
                                {name}
                              </p>
                              <p style={{ fontSize: '14px', color: '#999', margin: 0 }}>
                                {isString ? `Model: ${participant}` : `${llmProvider} - ${model}`}
                              </p>
                            </div>
                          </div>
                          {!isString && participant.systemPrompt && (
                            <p style={{ fontSize: '14px', color: '#666', marginLeft: '48px' }}>
                              {participant.systemPrompt}
                            </p>
                          )}
                          {index < currentDebate.participants.length - 1 && (
                            <Divider style={{ marginTop: '16px' }} />
                          )}
                        </div>
                      );
                    })
                  ) : (
                    <p style={{ fontSize: '14px', color: '#999' }}>
                      No participants found
                    </p>
                  )}
          </Card>

          <Card title="Export">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <Button
                type="default"
                onClick={() => handleExport("json")}
                icon={<DownloadOutlined />}
                block
              >
                Export as JSON
              </Button>
              <Button
                type="default"
                onClick={() => handleExport("markdown")}
                icon={<DownloadOutlined />}
                block
              >
                Export as Markdown
              </Button>
              <Button
                type="default"
                onClick={() => handleExport("pdf")}
                icon={<DownloadOutlined />}
                block
              >
                Export as PDF
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default DebateDetailPage;
