import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { TelemetryData } from '../types/workflow';

interface TelemetryState {
  // Real-time telemetry data
  telemetryData: TelemetryData[];
  
  // Connection status
  isConnected: boolean;
  
  // Spatial data for map
  spatialData: TelemetryData[];
  
  // Selected device for detailed view
  selectedDeviceId: string | null;
  
  // Actions
  addTelemetryData: (data: TelemetryData) => void;
  updateTelemetryData: (data: TelemetryData[]) => void;
  setConnectionStatus: (connected: boolean) => void;
  setSpatialData: (data: TelemetryData[]) => void;
  setSelectedDevice: (deviceId: string | null) => void;
  clearTelemetryData: () => void;
}

export const useTelemetryStore = create<TelemetryState>()(
  devtools(
    (set, get) => ({
      telemetryData: [],
      isConnected: false,
      spatialData: [],
      selectedDeviceId: null,

      addTelemetryData: (data) => set((state) => {
        // Keep only last 1000 data points for performance
        const newData = [...state.telemetryData, data].slice(-1000);
        
        // Update spatial data if location is present
        const newSpatialData = data.location 
          ? [...state.spatialData.filter(d => d.deviceId !== data.deviceId), data]
          : state.spatialData;
        
        return {
          telemetryData: newData,
          spatialData: newSpatialData
        };
      }),

      updateTelemetryData: (data) => set({
        telemetryData: data,
        spatialData: data.filter(d => d.location)
      }),

      setConnectionStatus: (connected) => set({ isConnected: connected }),

      setSpatialData: (data) => set({ spatialData: data }),

      setSelectedDevice: (deviceId) => set({ selectedDeviceId: deviceId }),

      clearTelemetryData: () => set({
        telemetryData: [],
        spatialData: [],
        selectedDeviceId: null
      })
    }),
    {
      name: 'telemetry-store'
    }
  )
);