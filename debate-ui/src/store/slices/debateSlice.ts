import { createSlice, createAsyncThunk, PayloadAction } from "@reduxjs/toolkit";
import debateClient, {
  Debate,
  CreateDebateRequest,
  DebateEvent,
} from "../../api/debateClient";

interface DebateState {
  debates: Debate[];
  currentDebate: Debate | null;
  loading: boolean;
  error: string | null;
  isConnected: boolean;
  actionLoading: {
    start: boolean;
    pause: boolean;
    cancel: boolean;
  };
}

const initialState: DebateState = {
  debates: [],
  currentDebate: null,
  loading: false,
  error: null,
  isConnected: false,
  actionLoading: {
    start: false,
    pause: false,
    cancel: false,
  },
};

export const fetchDebates = createAsyncThunk(
  "debate/fetchAll",
  async (params?: { status?: string; limit?: number; offset?: number }) => {
    const debates = await debateClient.listDebates(params);
    return debates;
  },
);

export const fetchDebate = createAsyncThunk(
  "debate/fetchOne",
  async (debateId: string) => {
    const debate = await debateClient.getDebate(debateId);
    return debate;
  },
);

export const createDebate = createAsyncThunk(
  "debate/create",
  async (data: CreateDebateRequest) => {
    const debate = await debateClient.createDebate(data);
    return debate;
  },
);

export const startDebate = createAsyncThunk(
  "debate/start",
  async (debateId: string) => {
    await debateClient.startDebate(debateId);
    const debate = await debateClient.getDebate(debateId);
    return debate;
  },
);

export const pauseDebate = createAsyncThunk(
  "debate/pause",
  async (debateId: string) => {
    await debateClient.pauseDebate(debateId);
    const debate = await debateClient.getDebate(debateId);
    return debate;
  },
);

export const cancelDebate = createAsyncThunk(
  "debate/cancel",
  async (debateId: string) => {
    await debateClient.cancelDebate(debateId);
    const debate = await debateClient.getDebate(debateId);
    return debate;
  },
);

export const connectToDebate = createAsyncThunk(
  "debate/connect",
  async (debateId: string, { dispatch }) => {
    debateClient.connectWebSocket(debateId);

    // Set up event handlers
    debateClient.on("debate_started", (event) => {
      dispatch(handleDebateEvent(event));
    });

    debateClient.on("round_started", (event) => {
      dispatch(handleDebateEvent(event));
    });

    debateClient.on("response_received", (event) => {
      dispatch(handleDebateEvent(event));
    });

    debateClient.on("round_completed", (event) => {
      dispatch(handleDebateEvent(event));
    });

    debateClient.on("debate_completed", (event) => {
      dispatch(handleDebateEvent(event));
    });

    return debateId;
  },
);

export const disconnectFromDebate = createAsyncThunk(
  "debate/disconnect",
  async () => {
    debateClient.disconnectWebSocket();
  },
);

const debateSlice = createSlice({
  name: "debate",
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    handleDebateEvent: (state, action: PayloadAction<DebateEvent>) => {
      const event = action.payload;

      // Update current debate if it matches
      if (state.currentDebate && state.currentDebate.id === event.debateId) {
        // This is a simplified update - in reality, we'd need to merge the event data
        // more carefully based on the event type
        Object.assign(state.currentDebate, event.data);
      }

      // Update debate in list
      const index = state.debates.findIndex((d) => d.id === event.debateId);
      if (index !== -1) {
        Object.assign(state.debates[index], event.data);
      }
    },
  },
  extraReducers: (builder) => {
    // Fetch debates
    builder
      .addCase(fetchDebates.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchDebates.fulfilled, (state, action) => {
        state.loading = false;
        state.debates = action.payload;
      })
      .addCase(fetchDebates.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "Failed to fetch debates";
      });

    // Fetch single debate
    builder
      .addCase(fetchDebate.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchDebate.fulfilled, (state, action) => {
        state.loading = false;
        state.currentDebate = action.payload;

        // Update in list too
        const index = state.debates.findIndex(
          (d) => d.id === action.payload.id,
        );
        if (index !== -1) {
          state.debates[index] = action.payload;
        }
      })
      .addCase(fetchDebate.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || "Failed to fetch debate";
      });

    // Create debate
    builder.addCase(createDebate.fulfilled, (state, action) => {
      state.debates.unshift(action.payload);
      state.currentDebate = action.payload;
    });

    // Start debate
    builder
      .addCase(startDebate.pending, (state) => {
        state.actionLoading.start = true;
        state.error = null;
      })
      .addCase(startDebate.fulfilled, (state, action) => {
        state.actionLoading.start = false;
        state.currentDebate = action.payload;
        const index = state.debates.findIndex((d) => d.id === action.payload.id);
        if (index !== -1) {
          state.debates[index] = action.payload;
        }
      })
      .addCase(startDebate.rejected, (state, action) => {
        state.actionLoading.start = false;
        state.error = action.error.message || "Failed to start debate";
      });

    // Pause debate
    builder
      .addCase(pauseDebate.pending, (state) => {
        state.actionLoading.pause = true;
        state.error = null;
      })
      .addCase(pauseDebate.fulfilled, (state, action) => {
        state.actionLoading.pause = false;
        state.currentDebate = action.payload;
        const index = state.debates.findIndex((d) => d.id === action.payload.id);
        if (index !== -1) {
          state.debates[index] = action.payload;
        }
      })
      .addCase(pauseDebate.rejected, (state, action) => {
        state.actionLoading.pause = false;
        state.error = action.error.message || "Failed to pause debate";
      });

    // Cancel debate
    builder
      .addCase(cancelDebate.pending, (state) => {
        state.actionLoading.cancel = true;
        state.error = null;
      })
      .addCase(cancelDebate.fulfilled, (state, action) => {
        state.actionLoading.cancel = false;
        state.currentDebate = action.payload;
        const index = state.debates.findIndex((d) => d.id === action.payload.id);
        if (index !== -1) {
          state.debates[index] = action.payload;
        }
      })
      .addCase(cancelDebate.rejected, (state, action) => {
        state.actionLoading.cancel = false;
        state.error = action.error.message || "Failed to cancel debate";
      });

    // WebSocket connection
    builder
      .addCase(connectToDebate.fulfilled, (state) => {
        state.isConnected = true;
      })
      .addCase(disconnectFromDebate.fulfilled, (state) => {
        state.isConnected = false;
      });
  },
});

export const { clearError, handleDebateEvent } = debateSlice.actions;
export default debateSlice.reducer;
