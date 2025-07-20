import React, { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import TelemetryMap from './TelemetryMap';
import { calculateDistance, findNearbyDevices, createHeatmapData } from '../utils/spatialAnalysis';

interface ProximityAnalysisProps {
  devices: any[];
  onAnalysisComplete?: (results: any) => void;
}

const ProximityAnalysis: React.FC<ProximityAnalysisProps> = ({
  devices,
  onAnalysisComplete
}) => {
  const [analysisType, setAnalysisType] = useState<'proximity' | 'density' | 'hotspot'>('proximity');
  const [selectedDevice, setSelectedDevice] = useState<string | null>(null);
  const [radius, setRadius] = useState<number>(1000);
  const [results, setResults] = useState<any>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const runAnalysis = async () => {
    setIsAnalyzing(true);
    let analysisResults = null;

    switch (analysisType) {
      case 'proximity':
        if (selectedDevice) {
          const device = devices.find(d => d.id === selectedDevice);
          if (device) {
            const nearby = findNearbyDevices(device, devices, radius);
            analysisResults = {
              type: 'proximity',
              centerDevice: device,
              nearbyDevices: nearby,
              radius
            };
          }
        }
        break;

      case 'density':
        const heatmap = createHeatmapData(devices);
        analysisResults = {
          type: 'density',
          heatmapData: heatmap,
          totalDevices: devices.length
        };
        break;

      case 'hotspot':
        // Identify hotspots based on device clustering
        analysisResults = {
          type: 'hotspot',
          hotspots: identifyHotspots(devices)
        };
        break;
    }

    setResults(analysisResults);
    setIsAnalyzing(false);
    
    if (onAnalysisComplete && analysisResults) {
      onAnalysisComplete(analysisResults);
    }
  };

  const identifyHotspots = (devices: any[]) => {
    // Simple clustering algorithm to identify hotspots
    const threshold = 500; // meters
    const minDevices = 5;
    const hotspots: any[] = [];
    
    devices.forEach((device, index) => {
      const cluster = devices.filter((d, i) => {
        if (i === index) return false;
        const distance = calculateDistance(
          device.location.lat,
          device.location.lng,
          d.location.lat,
          d.location.lng
        );
        return distance <= threshold;
      });
      
      if (cluster.length >= minDevices) {
        hotspots.push({
          center: device.location,
          deviceCount: cluster.length + 1,
          devices: [device, ...cluster]
        });
      }
    });
    
    return hotspots;
  };

  return (
    <div className="proximity-analysis">
      <div className="analysis-controls">
        <h3>Spatial Analysis Tools</h3>
        
        <div className="analysis-type-selector">
          <button
            className={analysisType === 'proximity' ? 'active' : ''}
            onClick={() => setAnalysisType('proximity')}
          >
            📍 Proximity Analysis
          </button>
          <button
            className={analysisType === 'density' ? 'active' : ''}
            onClick={() => setAnalysisType('density')}
          >
            🗺️ Density Mapping
          </button>
          <button
            className={analysisType === 'hotspot' ? 'active' : ''}
            onClick={() => setAnalysisType('hotspot')}
          >
            🔥 Hotspot Detection
          </button>
        </div>

        {analysisType === 'proximity' && (
          <div className="proximity-controls">
            <div className="control-group">
              <label>Select Device:</label>
              <select
                value={selectedDevice || ''}
                onChange={(e) => setSelectedDevice(e.target.value)}
              >
                <option value="">Choose a device...</option>
                {devices.map(device => (
                  <option key={device.id} value={device.id}>
                    {device.name} ({device.id})
                  </option>
                ))}
              </select>
            </div>
            
            <div className="control-group">
              <label>Radius (meters):</label>
              <input
                type="range"
                min="100"
                max="5000"
                step="100"
                value={radius}
                onChange={(e) => setRadius(Number(e.target.value))}
              />
              <span>{radius}m</span>
            </div>
          </div>
        )}

        <button
          className="analyze-button"
          onClick={runAnalysis}
          disabled={isAnalyzing || (analysisType === 'proximity' && !selectedDevice)}
        >
          {isAnalyzing ? '⏳ Analyzing...' : '🔍 Run Analysis'}
        </button>
      </div>

      {results && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="analysis-results"
        >
          <h4>Analysis Results</h4>
          
          {results.type === 'proximity' && (
            <div className="proximity-results">
              <p>Found <strong>{results.nearbyDevices.length}</strong> devices within {results.radius}m</p>
              <div className="device-list">
                {results.nearbyDevices.map((device: any) => (
                  <div key={device.id} className="device-item">
                    <span>{device.name}</span>
                    <span className="distance">{device.distance.toFixed(0)}m</span>
                  </div>
                ))}
              </div>
            </div>
          )}
          
          {results.type === 'density' && (
            <div className="density-results">
              <p>Total devices analyzed: <strong>{results.totalDevices}</strong></p>
              <p>Density heatmap generated</p>
            </div>
          )}
          
          {results.type === 'hotspot' && (
            <div className="hotspot-results">
              <p>Identified <strong>{results.hotspots.length}</strong> hotspot areas</p>
              {results.hotspots.map((hotspot: any, index: number) => (
                <div key={index} className="hotspot-item">
                  <span>Hotspot {index + 1}</span>
                  <span>{hotspot.deviceCount} devices</span>
                </div>
              ))}
            </div>
          )}
        </motion.div>
      )}

      <style>{`
        .proximity-analysis {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .analysis-controls h3 {
          margin: 0 0 20px 0;
          color: #333;
        }

        .analysis-type-selector {
          display: flex;
          gap: 10px;
          margin-bottom: 20px;
        }

        .analysis-type-selector button {
          flex: 1;
          padding: 12px;
          border: 2px solid #ddd;
          background: white;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: all 0.2s;
        }

        .analysis-type-selector button.active {
          border-color: #2196F3;
          background: #e3f2fd;
          color: #1976D2;
        }

        .proximity-controls {
          margin-bottom: 20px;
        }

        .control-group {
          margin-bottom: 15px;
        }

        .control-group label {
          display: block;
          margin-bottom: 5px;
          font-size: 14px;
          color: #666;
        }

        .control-group select,
        .control-group input[type="range"] {
          width: 100%;
          padding: 8px;
          border: 1px solid #ddd;
          border-radius: 4px;
        }

        .analyze-button {
          width: 100%;
          padding: 12px;
          background: #2196F3;
          color: white;
          border: none;
          border-radius: 4px;
          font-size: 16px;
          cursor: pointer;
          transition: background 0.2s;
        }

        .analyze-button:hover:not(:disabled) {
          background: #1976D2;
        }

        .analyze-button:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .analysis-results {
          margin-top: 20px;
          padding: 20px;
          background: #f5f5f5;
          border-radius: 4px;
        }

        .analysis-results h4 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .device-list,
        .hotspot-item {
          display: flex;
          justify-content: space-between;
          padding: 8px;
          background: white;
          border-radius: 4px;
          margin-bottom: 5px;
        }

        .distance {
          color: #666;
          font-size: 14px;
        }
      `}</style>
    </div>
  );
};

export default ProximityAnalysis;