import React, { useState, useEffect, useMemo, useCallback } from 'react';
import MapViewer, { MapMarker } from './MapViewer';
import MapStyleSelector from './MapStyleSelector';
import { motion } from 'framer-motion';
import { useWorkflowStore } from '../store/workflowStore';
import { MAP_CONFIG, getMapStyle } from '../config/mapConfig';
import { getTelemetryService } from '../services/telemetryWebSocket';
import { Card, CardContent, CardHeader, CardTitle, Button, Checkbox, Label, Badge } from '@zamaz/ui';
import { Play, Pause, X } from 'lucide-react';

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
  const [showAlertsState, setShowAlerts] = useState(showAlerts);
  const [mapStyle, setMapStyle] = useState('cartoLight');

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
        if (!showAlertsState && device.status === 'alert') return false;
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
  }, [allDevices, showAlertsState]);

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
    <Card>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle>Spatial Telemetry Visualization</CardTitle>
          <div className="flex items-center gap-5">
            <Button
              variant={isSimulating ? 'danger' : 'primary'}
              size="sm"
              onClick={() => setIsSimulating(!isSimulating)}
              leftIcon={isSimulating ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            >
              {isSimulating ? 'Stop' : 'Simulate'}
            </Button>
          
            <div className="flex gap-4">
              <div className="flex items-center gap-2">
                <Checkbox
                  id="clustering"
                  checked={showClustering}
                  disabled
                />
                <Label htmlFor="clustering" className="text-gray-500">
                  Clustering
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <Checkbox
                  id="heatmap"
                  checked={showHeatmap}
                  disabled
                />
                <Label htmlFor="heatmap" className="text-gray-500">
                  Heatmap
                </Label>
              </div>
              <div className="flex items-center gap-2">
                <Checkbox
                  id="alerts"
                  checked={showAlertsState}
                  onCheckedChange={(checked) => setShowAlerts(!!checked)}
                />
                <Label htmlFor="alerts">
                  Alerts
                </Label>
              </div>
            </div>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
          <motion.div
            className="bg-gray-50 p-4 rounded-lg text-center cursor-pointer hover:shadow-md transition-shadow"
            whileHover={{ scale: 1.05 }}
          >
            <span className="block text-3xl font-bold text-gray-900 mb-1">{stats.total}</span>
            <span className="text-sm text-gray-600">Total Devices</span>
          </motion.div>
          <motion.div
            className="bg-green-50 p-4 rounded-lg text-center cursor-pointer hover:shadow-md transition-shadow"
            whileHover={{ scale: 1.05 }}
          >
            <span className="block text-3xl font-bold text-green-700 mb-1">{stats.active}</span>
            <span className="text-sm text-green-600">Active</span>
          </motion.div>
          <motion.div
            className="bg-red-50 p-4 rounded-lg text-center cursor-pointer hover:shadow-md transition-shadow"
            whileHover={{ scale: 1.05 }}
          >
            <span className="block text-3xl font-bold text-red-700 mb-1">{stats.alerts}</span>
            <span className="text-sm text-red-600">Alerts</span>
          </motion.div>
          <motion.div
            className="bg-gray-100 p-4 rounded-lg text-center cursor-pointer hover:shadow-md transition-shadow"
            whileHover={{ scale: 1.05 }}
          >
            <span className="block text-3xl font-bold text-gray-600 mb-1">{stats.inactive}</span>
            <span className="text-sm text-gray-500">Inactive</span>
          </motion.div>
        </div>

        <div className="relative">
          <MapViewer
            center={center || MAP_CONFIG.bounds.stamfordCT.center as [number, number]}
            zoom={zoom || MAP_CONFIG.bounds.stamfordCT.zoom}
            markers={markers}
            height={height}
            onMarkerClick={handleMarkerClick}
            onBoundsChange={setMapBounds}
            style={getMapStyle(mapStyle)}
            minZoom={MAP_CONFIG.zoomLevels.min}
            maxZoom={MAP_CONFIG.zoomLevels.max}
          />
          <MapStyleSelector
            currentStyle={mapStyle}
            onStyleChange={setMapStyle}
            position="top-right"
          />
        </div>

        {selectedDevice && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="absolute bottom-5 right-5 bg-white p-5 rounded-lg shadow-lg max-w-sm z-50"
          >
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSelectedDevice(null)}
              className="absolute top-3 right-3 p-1"
            >
              <X className="h-4 w-4" />
            </Button>
            <h4 className="text-lg font-semibold mb-3">{selectedDevice.name}</h4>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">ID:</span>
                <span className="font-medium">{selectedDevice.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Type:</span>
                <span className="font-medium">{selectedDevice.type}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Status:</span>
                <Badge
                  variant={
                    selectedDevice.status === 'active' ? 'success' :
                    selectedDevice.status === 'alert' ? 'danger' :
                    'secondary'
                  }
                >
                  {selectedDevice.status}
                </Badge>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Location:</span>
                <span className="font-medium">
                  {selectedDevice.location.lat.toFixed(4)}, {selectedDevice.location.lng.toFixed(4)}
                </span>
              </div>
              {selectedDevice.lastValue !== undefined && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Last Value:</span>
                  <span className="font-medium">{selectedDevice.lastValue.toFixed(2)}</span>
                </div>
              )}
              {selectedDevice.lastUpdate && (
                <div className="flex justify-between">
                  <span className="text-gray-600">Last Update:</span>
                  <span className="font-medium">
                    {new Date(selectedDevice.lastUpdate).toLocaleTimeString()}
                  </span>
                </div>
              )}
            </div>
          </motion.div>
        )}

      </CardContent>
    </Card>
  );
};

export default TelemetryMap;