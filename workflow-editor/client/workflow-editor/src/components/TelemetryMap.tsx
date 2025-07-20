import React, { useState, useEffect, useMemo, useCallback } from 'react';
import MapViewer, { MapMarker } from './MapViewer';
import { motion } from 'framer-motion';
import { useWorkflowStore } from '../store/workflowStore';
import { MAP_CONFIG, getMapStyle } from '../config/mapConfig';
import { getTelemetryService } from '../services/telemetryWebSocket';

interface TelemetryDevice {
  id: string;
  name: string;
  type: string;
  location: {
    lat: number;
    lng: number;
  };
  lastValue?: number;
  lastUpdate?: number;
  status: 'active' | 'inactive' | 'alert';
}

interface TelemetryMapProps {
  devices?: TelemetryDevice[];
  center?: [number, number];
  zoom?: number;
  showClustering?: boolean;
  showHeatmap?: boolean;
  showAlerts?: boolean;
  refreshInterval?: number;
  onDeviceClick?: (device: TelemetryDevice) => void;
  height?: string;
}

const TelemetryMap: React.FC<TelemetryMapProps> = ({
  devices = [],
  center,
  zoom,
  showClustering = true,
  showHeatmap = false,
  showAlerts = true,
  refreshInterval = 5000,
  onDeviceClick,
  height = '600px'
}) => {
  const { telemetryData, addTelemetryData } = useWorkflowStore();
  const [selectedDevice, setSelectedDevice] = useState<TelemetryDevice | null>(null);
  const [mapBounds, setMapBounds] = useState<any>(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [deviceData, setDeviceData] = useState<Map<string, TelemetryDevice>>(new Map());

  // Initialize WebSocket connection
  useEffect(() => {
    const telemetryService = getTelemetryService();
    telemetryService.connect();

    // Subscribe to telemetry updates
    const unsubscribe = telemetryService.subscribe('telemetry.location', (data) => {
      if (data.deviceId && data.location) {
        const device = deviceData.get(data.deviceId);
        if (device) {
          device.location = data.location;
          device.lastValue = data.value;
          device.lastUpdate = Date.now();
          setDeviceData(new Map(deviceData));
        }
      }
    });

    return () => {
      unsubscribe();
    };
  }, [deviceData]);

  // Simulate device data for demo
  useEffect(() => {
    if (!isSimulating) return;

    const interval = setInterval(() => {
      const simulatedDevices: TelemetryDevice[] = Array.from({ length: 20 }, (_, i) => {
        const baseLocation = center || MAP_CONFIG.bounds.stamfordCT.center;
        const variance = 0.05; // Roughly 5km variance
        
        return {
          id: `device-${i + 1}`,
          name: `Sensor ${i + 1}`,
          type: ['temperature', 'pressure', 'humidity'][i % 3],
          location: {
            lat: baseLocation[1] + (Math.random() - 0.5) * variance,
            lng: baseLocation[0] + (Math.random() - 0.5) * variance
          },
          lastValue: Math.random() * 100,
          lastUpdate: Date.now(),
          status: Math.random() > 0.9 ? 'alert' : Math.random() > 0.1 ? 'active' : 'inactive'
        };
      });

      const newDeviceData = new Map<string, TelemetryDevice>();
      simulatedDevices.forEach(device => {
        newDeviceData.set(device.id, device);
        
        // Add telemetry data
        addTelemetryData(`device.${device.id}`, {
          timestamp: Date.now(),
          value: device.lastValue!,
          deviceId: device.id,
          metadata: {
            location: device.location,
            type: device.type
          }
        });
      });
      
      setDeviceData(newDeviceData);
    }, refreshInterval);

    return () => clearInterval(interval);
  }, [isSimulating, refreshInterval, center, addTelemetryData]);

  // Merge provided devices with real-time data
  const allDevices = useMemo(() => {
    const merged = new Map<string, TelemetryDevice>();
    
    // Add provided devices
    devices.forEach(device => merged.set(device.id, device));
    
    // Add/update with real-time data
    deviceData.forEach((device, id) => merged.set(id, device));
    
    return Array.from(merged.values());
  }, [devices, deviceData]);

  // Convert devices to markers
  const markers: MapMarker[] = useMemo(() => {
    return allDevices
      .filter(device => {
        if (!showAlerts && device.status === 'alert') return false;
        return true;
      })
      .map(device => ({
        id: device.id,
        lng: device.location.lng,
        lat: device.location.lat,
        type: device.type,
        color: device.status === 'alert' ? '#f44336' : 
               device.status === 'active' ? '#4CAF50' : '#9E9E9E',
        icon: device.status === 'alert' ? '⚠️' :
              device.type === 'temperature' ? '🌡️' :
              device.type === 'pressure' ? '🎯' :
              device.type === 'humidity' ? '💧' : '📍',
        properties: {
          label: device.name,
          description: `
            <div>
              <p><strong>Type:</strong> ${device.type}</p>
              <p><strong>Status:</strong> ${device.status}</p>
              ${device.lastValue !== undefined ? 
                `<p><strong>Value:</strong> ${device.lastValue.toFixed(2)}</p>` : ''}
              ${device.lastUpdate ? 
                `<p><strong>Last Update:</strong> ${new Date(device.lastUpdate).toLocaleTimeString()}</p>` : ''}
            </div>
          `
        }
      }));
  }, [allDevices, showAlerts]);

  const handleMarkerClick = useCallback((marker: MapMarker) => {
    const device = allDevices.find(d => d.id === marker.id);
    if (device) {
      setSelectedDevice(device);
      if (onDeviceClick) {
        onDeviceClick(device);
      }
    }
  }, [allDevices, onDeviceClick]);

  const getDeviceStats = () => {
    const total = allDevices.length;
    const active = allDevices.filter(d => d.status === 'active').length;
    const alerts = allDevices.filter(d => d.status === 'alert').length;
    const inactive = allDevices.filter(d => d.status === 'inactive').length;
    
    return { total, active, alerts, inactive };
  };

  const stats = getDeviceStats();

  return (
    <div className="telemetry-map-container">
      <div className="map-header">
        <h3>Spatial Telemetry Visualization</h3>
        <div className="map-controls">
          <button
            className={`simulate-button ${isSimulating ? 'active' : ''}`}
            onClick={() => setIsSimulating(!isSimulating)}
          >
            {isSimulating ? '⏹ Stop' : '▶ Simulate'}
          </button>
          
          <div className="view-options">
            <label>
              <input
                type="checkbox"
                checked={showClustering}
                disabled
              />
              Clustering
            </label>
            <label>
              <input
                type="checkbox"
                checked={showHeatmap}
                disabled
              />
              Heatmap
            </label>
            <label>
              <input
                type="checkbox"
                checked={showAlerts}
                onChange={(e) => setShowAlerts(e.target.checked)}
              />
              Alerts
            </label>
          </div>
        </div>
      </div>

      <div className="map-stats">
        <motion.div
          className="stat-card"
          whileHover={{ scale: 1.05 }}
        >
          <span className="stat-value">{stats.total}</span>
          <span className="stat-label">Total Devices</span>
        </motion.div>
        <motion.div
          className="stat-card active"
          whileHover={{ scale: 1.05 }}
        >
          <span className="stat-value">{stats.active}</span>
          <span className="stat-label">Active</span>
        </motion.div>
        <motion.div
          className="stat-card alert"
          whileHover={{ scale: 1.05 }}
        >
          <span className="stat-value">{stats.alerts}</span>
          <span className="stat-label">Alerts</span>
        </motion.div>
        <motion.div
          className="stat-card inactive"
          whileHover={{ scale: 1.05 }}
        >
          <span className="stat-value">{stats.inactive}</span>
          <span className="stat-label">Inactive</span>
        </motion.div>
      </div>

      <MapViewer
        center={center || MAP_CONFIG.bounds.stamfordCT.center as [number, number]}
        zoom={zoom || MAP_CONFIG.bounds.stamfordCT.zoom}
        markers={markers}
        height={height}
        onMarkerClick={handleMarkerClick}
        onBoundsChange={setMapBounds}
        style={getMapStyle()}
        minZoom={MAP_CONFIG.zoomLevels.min}
        maxZoom={MAP_CONFIG.zoomLevels.max}
      />

      {selectedDevice && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="device-details"
        >
          <button
            className="close-button"
            onClick={() => setSelectedDevice(null)}
          >
            ✕
          </button>
          <h4>{selectedDevice.name}</h4>
          <div className="device-info">
            <p><strong>ID:</strong> {selectedDevice.id}</p>
            <p><strong>Type:</strong> {selectedDevice.type}</p>
            <p><strong>Status:</strong> 
              <span className={`status ${selectedDevice.status}`}>
                {selectedDevice.status}
              </span>
            </p>
            <p><strong>Location:</strong> {selectedDevice.location.lat.toFixed(4)}, {selectedDevice.location.lng.toFixed(4)}</p>
            {selectedDevice.lastValue !== undefined && (
              <p><strong>Last Value:</strong> {selectedDevice.lastValue.toFixed(2)}</p>
            )}
            {selectedDevice.lastUpdate && (
              <p><strong>Last Update:</strong> {new Date(selectedDevice.lastUpdate).toLocaleString()}</p>
            )}
          </div>
        </motion.div>
      )}

      <style>{`
        .telemetry-map-container {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .map-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }

        .map-header h3 {
          margin: 0;
          color: #333;
        }

        .map-controls {
          display: flex;
          align-items: center;
          gap: 20px;
        }

        .simulate-button {
          padding: 8px 16px;
          border: none;
          border-radius: 4px;
          background: #2196F3;
          color: white;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .simulate-button:hover {
          background: #1976D2;
        }

        .simulate-button.active {
          background: #f44336;
        }

        .view-options {
          display: flex;
          gap: 15px;
        }

        .view-options label {
          display: flex;
          align-items: center;
          gap: 5px;
          cursor: pointer;
          font-size: 14px;
        }

        .map-stats {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
          gap: 15px;
          margin-bottom: 20px;
        }

        .stat-card {
          background: #f5f5f5;
          padding: 15px;
          border-radius: 8px;
          text-align: center;
          cursor: pointer;
          transition: all 0.2s;
        }

        .stat-card:hover {
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .stat-card.active {
          background: #e8f5e9;
          color: #2e7d32;
        }

        .stat-card.alert {
          background: #ffebee;
          color: #c62828;
        }

        .stat-card.inactive {
          background: #fafafa;
          color: #757575;
        }

        .stat-value {
          display: block;
          font-size: 28px;
          font-weight: bold;
          margin-bottom: 5px;
        }

        .stat-label {
          display: block;
          font-size: 14px;
          opacity: 0.8;
        }

        .device-details {
          position: absolute;
          bottom: 20px;
          right: 20px;
          background: white;
          padding: 20px;
          border-radius: 8px;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
          max-width: 300px;
          z-index: 1000;
        }

        .device-details h4 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .device-info p {
          margin: 8px 0;
          font-size: 14px;
          color: #666;
        }

        .device-info strong {
          color: #333;
        }

        .status {
          margin-left: 8px;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
        }

        .status.active {
          background: #e8f5e9;
          color: #2e7d32;
        }

        .status.alert {
          background: #ffebee;
          color: #c62828;
        }

        .status.inactive {
          background: #fafafa;
          color: #757575;
        }

        .close-button {
          position: absolute;
          top: 15px;
          right: 15px;
          background: none;
          border: none;
          font-size: 18px;
          cursor: pointer;
          color: #999;
          padding: 0;
        }

        .close-button:hover {
          color: #333;
        }
      `}</style>
    </div>
  );
};

export default TelemetryMap;