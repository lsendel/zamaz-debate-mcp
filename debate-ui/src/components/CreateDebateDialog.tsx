import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Slider,
  Paper,
  Chip,
} from "@mui/material";
import {
  Close as CloseIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
} from "@mui/icons-material";
import { useAppSelector, useAppDispatch } from "../store";
import {
  closeCreateDebateDialog,
  addNotification,
} from "../store/slices/uiSlice";
import { createDebate } from "../store/slices/debateSlice";
import llmClient, { LLMProvider, LLMModel } from "../api/llmClient";

interface Participant {
  name: string;
  llmProvider: string;
  model: string;
  systemPrompt: string;
  temperature: number;
  maxTokens: number;
}

const CreateDebateDialog: React.FC = () => {
  const dispatch = useAppDispatch();
  const { createDebateDialogOpen } = useAppSelector((state) => state.ui);
  const [providers, setProviders] = useState<LLMProvider[]>([]);
  const [loading, setLoading] = useState(false);

  const [topic, setTopic] = useState("");
  const [description, setDescription] = useState("");
  const [maxRounds, setMaxRounds] = useState(5);
  const [turnTimeLimit, setTurnTimeLimit] = useState(60);
  const [participants, setParticipants] = useState<Participant[]>([
    {
      name: "Participant 1",
      llmProvider: "",
      model: "",
      systemPrompt:
        "You are a thoughtful debater who provides well-reasoned arguments.",
      temperature: 0.7,
      maxTokens: 1000,
    },
    {
      name: "Participant 2",
      llmProvider: "",
      model: "",
      systemPrompt:
        "You are a critical thinker who challenges assumptions and provides counterarguments.",
      temperature: 0.7,
      maxTokens: 1000,
    },
  ]);

  useEffect(() => {
    const loadProviders = async () => {
      try {
        const providerList = await llmClient.listProviders();
        setProviders(providerList);
        
        // Set default providers and models after loading
        if (providerList.length > 0) {
          const updatedParticipants = participants.map((participant, index) => {
            if (!participant.llmProvider) {
              const defaultProvider = providerList[index % providerList.length];
              const defaultModel = defaultProvider.models[0];
              return {
                ...participant,
                llmProvider: defaultProvider.id,
                model: defaultModel.id
              };
            }
            return participant;
          });
          setParticipants(updatedParticipants);
        }
      } catch (error) {
        console.error("Failed to load providers:", error);
      }
    };

    if (createDebateDialogOpen) {
      loadProviders();
    }
  }, [createDebateDialogOpen]);

  const handleClose = () => {
    dispatch(closeCreateDebateDialog());
    // Reset form
    setTopic("");
    setDescription("");
    setMaxRounds(5);
    setTurnTimeLimit(60);
    setParticipants([
      {
        name: "Participant 1",
        llmProvider: "",
        model: "",
        systemPrompt:
          "You are a thoughtful debater who provides well-reasoned arguments.",
        temperature: 0.7,
        maxTokens: 1000,
      },
      {
        name: "Participant 2",
        llmProvider: "",
        model: "",
        systemPrompt:
          "You are a critical thinker who challenges assumptions and provides counterarguments.",
        temperature: 0.7,
        maxTokens: 1000,
      },
    ]);
  };

  const handleSubmit = async () => {
    if (!topic || participants.length < 2) {
      dispatch(
        addNotification({
          type: "error",
          message: "Please provide a topic and at least 2 participants",
        }),
      );
      return;
    }

    setLoading(true);
    try {
      const resultAction = await dispatch(
        createDebate({
          topic,
          description,
          participants,
          maxRounds,
          turnTimeLimit,
        }),
      );

      // Check if the action was fulfilled successfully
      if (createDebate.fulfilled.match(resultAction)) {
        dispatch(
          addNotification({
            type: "success",
            message: "Debate created successfully",
          }),
        );
        handleClose();
      } else {
        // Handle rejected case
        const errorMessage =
          resultAction.error?.message || "Failed to create debate";
        throw new Error(errorMessage);
      }
    } catch (error) {
      dispatch(
        addNotification({
          type: "error",
          message:
            error instanceof Error ? error.message : "Failed to create debate",
        }),
      );
    } finally {
      setLoading(false);
    }
  };

  const addParticipant = () => {
    setParticipants([
      ...participants,
      {
        name: `Participant ${participants.length + 1}`,
        llmProvider: providers.length > 0 ? providers[0].id : "",
        model: providers.length > 0 && providers[0].models.length > 0 ? providers[0].models[0].id : "",
        systemPrompt: "",
        temperature: 0.7,
        maxTokens: 1000,
      },
    ]);
  };

  const removeParticipant = (index: number) => {
    setParticipants(participants.filter((_, i) => i !== index));
  };

  const updateParticipant = (
    index: number,
    field: keyof Participant,
    value: any,
  ) => {
    const updated = [...participants];
    updated[index] = { ...updated[index], [field]: value };
    
    // If changing provider, also update the model to first available model
    if (field === 'llmProvider') {
      const provider = providers.find(p => p.id === value);
      if (provider && provider.models.length > 0) {
        updated[index] = { ...updated[index], model: provider.models[0].id };
      }
    }
    
    setParticipants(updated);
  };

  const getAvailableModels = (providerId: string) => {
    const providerInfo = providers.find((p) => p.id === providerId);
    return providerInfo?.models || [];
  };

  return (
    <Dialog
      open={createDebateDialogOpen}
      onClose={handleClose}
      maxWidth="md"
      fullWidth
    >
      <DialogTitle>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <Typography variant="h6">Create New Debate</Typography>
          <IconButton onClick={handleClose}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ mt: 2 }}>
          <TextField
            fullWidth
            label="Topic"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            margin="normal"
            multiline
            rows={3}
          />

          <Box sx={{ mt: 3, mb: 2 }}>
            <Typography gutterBottom>Max Rounds: {maxRounds}</Typography>
            <Slider
              value={maxRounds}
              onChange={(_, value) => setMaxRounds(value as number)}
              min={1}
              max={20}
              marks
              step={1}
            />
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography gutterBottom>
              Turn Time Limit: {turnTimeLimit} seconds
            </Typography>
            <Slider
              value={turnTimeLimit}
              onChange={(_, value) => setTurnTimeLimit(value as number)}
              min={30}
              max={300}
              marks={[
                { value: 30, label: "30s" },
                { value: 60, label: "1m" },
                { value: 120, label: "2m" },
                { value: 300, label: "5m" },
              ]}
              step={30}
            />
          </Box>

          <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Participants
            </Typography>
            <Button
              startIcon={<AddIcon />}
              onClick={addParticipant}
              size="small"
            >
              Add Participant
            </Button>
          </Box>

          {participants.map((participant, index) => (
            <Paper key={index} sx={{ p: 2, mb: 2 }}>
              <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                <TextField
                  label="Name"
                  value={participant.name}
                  onChange={(e) =>
                    updateParticipant(index, "name", e.target.value)
                  }
                  size="small"
                  sx={{ flexGrow: 1, mr: 1 }}
                />
                {participants.length > 2 && (
                  <IconButton
                    onClick={() => removeParticipant(index)}
                    color="error"
                    size="small"
                  >
                    <DeleteIcon />
                  </IconButton>
                )}
              </Box>

              <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
                <FormControl size="small" sx={{ minWidth: 120 }}>
                  <InputLabel>Provider</InputLabel>
                  <Select
                    value={participant.llmProvider}
                    onChange={(e) =>
                      updateParticipant(index, "llmProvider", e.target.value)
                    }
                    label="Provider"
                  >
                    {providers.map((provider) => (
                      <MenuItem key={provider.id} value={provider.id}>
                        <Box
                          sx={{ display: "flex", alignItems: "center", gap: 1 }}
                        >
                          {provider.name}
                          {provider.status !== "ACTIVE" && (
                            <Chip
                              label={provider.status}
                              size="small"
                              color="error"
                              variant="outlined"
                            />
                          )}
                        </Box>
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <FormControl size="small" sx={{ minWidth: 200 }}>
                  <InputLabel>Model</InputLabel>
                  <Select
                    value={participant.model}
                    onChange={(e) =>
                      updateParticipant(index, "model", e.target.value)
                    }
                    label="Model"
                  >
                    {getAvailableModels(participant.llmProvider).map(
                      (model) => (
                        <MenuItem key={model.id} value={model.id}>
                          {model.name}
                        </MenuItem>
                      ),
                    )}
                  </Select>
                </FormControl>
              </Box>

              <TextField
                fullWidth
                label="System Prompt"
                value={participant.systemPrompt}
                onChange={(e) =>
                  updateParticipant(index, "systemPrompt", e.target.value)
                }
                size="small"
                multiline
                rows={2}
                sx={{ mb: 2 }}
              />

              <Box sx={{ display: "flex", gap: 2 }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="caption">
                    Temperature: {participant.temperature}
                  </Typography>
                  <Slider
                    value={participant.temperature}
                    onChange={(_, value) =>
                      updateParticipant(index, "temperature", value)
                    }
                    min={0}
                    max={2}
                    step={0.1}
                    size="small"
                  />
                </Box>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="caption">
                    Max Tokens: {participant.maxTokens}
                  </Typography>
                  <Slider
                    value={participant.maxTokens}
                    onChange={(_, value) =>
                      updateParticipant(index, "maxTokens", value)
                    }
                    min={100}
                    max={4000}
                    step={100}
                    size="small"
                  />
                </Box>
              </Box>
            </Paper>
          ))}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={loading || !topic || participants.length < 2}
        >
          {loading ? "Creating..." : "Create Debate"}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default CreateDebateDialog;
