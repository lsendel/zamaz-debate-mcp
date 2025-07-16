import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UIState {
  sidebarOpen: boolean;
  createDebateDialogOpen: boolean;
  selectedTab: 'debates' | 'analytics' | 'settings';
  notifications: Array<{
    id: string;
    type: 'success' | 'error' | 'info' | 'warning';
    message: string;
    timestamp: number;
  }>;
}

const initialState: UIState = {
  sidebarOpen: true,
  createDebateDialogOpen: false,
  selectedTab: 'debates',
  notifications: [],
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
    },
    openCreateDebateDialog: (state) => {
      state.createDebateDialogOpen = true;
    },
    closeCreateDebateDialog: (state) => {
      state.createDebateDialogOpen = false;
    },
    setSelectedTab: (state, action: PayloadAction<'debates' | 'analytics' | 'settings'>) => {
      state.selectedTab = action.payload;
    },
    addNotification: (
      state,
      action: PayloadAction<{
        type: 'success' | 'error' | 'info' | 'warning';
        message: string;
      }>
    ) => {
      state.notifications.push({
        id: Date.now().toString(),
        timestamp: Date.now(),
        ...action.payload,
      });
    },
    removeNotification: (state, action: PayloadAction<string>) => {
      state.notifications = state.notifications.filter(n => n.id !== action.payload);
    },
    clearNotifications: (state) => {
      state.notifications = [];
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  openCreateDebateDialog,
  closeCreateDebateDialog,
  setSelectedTab,
  addNotification,
  removeNotification,
  clearNotifications,
} = uiSlice.actions;

export default uiSlice.reducer;