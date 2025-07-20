import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import TelemetryMap from '../components/TelemetryMap';
import TelemetryDashboard from '../components/TelemetryDashboard';
import ProximityAnalysis from '../components/ProximityAnalysis';
import { useWorkflowStore } from '../store/workflowStore';

// Stamford CT boundaries
const STAMFORD_BOUNDS = {
  north: 41.1468,
  south: 40.9633,
  east: -73.4734,
  west: -73.6346
};

interface StamfordAddress {
  id: string;
  address: string;
  lat: number;
  lng: number;
  type: 'residential' | 'commercial' | 'industrial';
  sensorType: 'temperature' | 'air_quality' | 'noise' | 'traffic';
}

const StamfordGeospatialSample: React.FC = () => {
  const [addresses, setAddresses] = useState<StamfordAddress[]>([]);
  const [selectedView, setSelectedView] = useState<'map' | 'dashboard' | 'analysis'>('map');
  const [isGenerating, setIsGenerating] = useState(false);
  const { addTelemetryData } = useWorkflowStore();

  // Generate random addresses within Stamford boundaries
  const generateStamfordAddresses = () => {
    setIsGenerating(true);
    
    const streetNames = [
      'Main Street', 'Atlantic Street', 'Washington Boulevard', 'Summer Street',
      'Bedford Street', 'Broad Street', 'Tresser Boulevard', 'High Ridge Road',
      'Long Ridge Road', 'Newfield Avenue'
    ];
    
    const sensorTypes: StamfordAddress['sensorType'][] = ['temperature', 'air_quality', 'noise', 'traffic'];
    const addressTypes: StamfordAddress['type'][] = ['residential', 'commercial', 'industrial'];
    
    const newAddresses: StamfordAddress[] = [];
    
    for (let i = 0; i < 10; i++) {
      const lat = STAMFORD_BOUNDS.south + 
        (Math.random() * (STAMFORD_BOUNDS.north - STAMFORD_BOUNDS.south));
      const lng = STAMFORD_BOUNDS.west + 
        (Math.random() * (STAMFORD_BOUNDS.east - STAMFORD_BOUNDS.west));
      
      const streetNumber = Math.floor(Math.random() * 999) + 1;
      const streetName = streetNames[Math.floor(Math.random() * streetNames.length)];
      
      const address: StamfordAddress = {
        id: `stamford-${i + 1}`,
        address: `${streetNumber} ${streetName}, Stamford, CT`,
        lat,
        lng,
        type: addressTypes[Math.floor(Math.random() * addressTypes.length)],
        sensorType: sensorTypes[Math.floor(Math.random() * sensorTypes.length)]
      };
      
      newAddresses.push(address);
    }
    
    setAddresses(newAddresses);
    setIsGenerating(false);
    
    // Start telemetry simulation
    startTelemetrySimulation(newAddresses);
  };

  // Simulate telemetry data at 10Hz
  const startTelemetrySimulation = (addressList: StamfordAddress[]) => {
    const interval = setInterval(() => {
      addressList.forEach(address => {
        let value: number;
        let unit: string;
        
        switch (address.sensorType) {
          case 'temperature':
            value = 20 + Math.random() * 15; // 20-35°C
            unit = '°C';
            break;
          case 'air_quality':
            value = 50 + Math.random() * 100; // AQI 50-150
            unit = 'AQI';
            break;
          case 'noise':
            value = 40 + Math.random() * 40; // 40-80 dB
            unit = 'dB';
            break;
          case 'traffic':
            value = Math.random() * 100; // 0-100% congestion
            unit = '%';
            break;
          default:
            value = Math.random() * 100;
            unit = '';
        }
        
        addTelemetryData(`stamford.${address.sensorType}`, {
          timestamp: Date.now(),
          value,
          deviceId: address.id,
          metadata: {
            address: address.address,
            location: { lat: address.lat, lng: address.lng },
            type: address.type,
            sensorType: address.sensorType,
            unit
          }
        });
      });
    }, 100); // 10Hz = 100ms interval

    // Clean up on unmount
    return () => clearInterval(interval);
  };

  // Convert addresses to device format for map
  const devicesFromAddresses = addresses.map(addr => ({
    id: addr.id,
    name: `${addr.sensorType} sensor`,
    type: addr.sensorType,
    location: { lat: addr.lat, lng: addr.lng },
    status: 'active' as const
  }));

  // Create sample workflow for proximity alerts
  const createProximityWorkflow = () => {
    const { createWorkflow } = useWorkflowStore();
    
    const workflow = {
      id: 'stamford-proximity-workflow',
      name: 'Stamford Proximity Alert Workflow',
      description: 'Alert when sensors detect anomalies in proximity',
      status: 'published' as const,
      nodes: [
        {
          id: 'start-1',
          type: 'start',
          position: { x: 100, y: 100 },
          data: { 
            label: 'Monitor Sensors',
            configuration: { trigger: 'event', eventType: 'sensor.threshold' }
          }
        },
        {
          id: 'decision-1',
          type: 'decision',
          position: { x: 300, y: 100 },
          data: {
            label: 'Check Proximity',
            configuration: {
              decisionType: 'telemetry',
              telemetrySource: 'stamford.sensors'
            }
          }
        },
        {
          id: 'task-1',
          type: 'task',
          position: { x: 500, y: 50 },
          data: {
            label: 'Send Alert',
            configuration: {
              taskType: 'NOTIFICATION',
              timeout: 30
            }
          }
        },
        {
          id: 'end-1',
          type: 'end',
          position: { x: 700, y: 100 },
          data: {
            label: 'Complete',
            configuration: { status: 'success' }
          }
        }
      ],
      edges: [
        { id: 'e1', source: 'start-1', target: 'decision-1', animated: true },
        { id: 'e2', source: 'decision-1', sourceHandle: 'true', target: 'task-1', animated: true },
        { id: 'e3', source: 'task-1', target: 'end-1', animated: true },
        { id: 'e4', source: 'decision-1', sourceHandle: 'false', target: 'end-1', animated: true }
      ]
    };
    
    createWorkflow(workflow);
  };

  useEffect(() => {
    generateStamfordAddresses();
  }, []);

  return (
    <div className="stamford-sample">
      <div className="sample-header">
        <h2>Stamford Connecticut Geospatial Sample</h2>
        <p>Real-time sensor monitoring across Stamford with 10Hz telemetry updates</p>
      </div>

      <div className="sample-controls">
        <button
          className="generate-button"
          onClick={generateStamfordAddresses}
          disabled={isGenerating}
        >
          🔄 Regenerate Addresses
        </button>
        
        <button
          className="workflow-button"
          onClick={createProximityWorkflow}
        >
          📋 Create Sample Workflow
        </button>
        
        <div className="view-selector">
          <button
            className={selectedView === 'map' ? 'active' : ''}
            onClick={() => setSelectedView('map')}
          >
            🗺️ Map View
          </button>
          <button
            className={selectedView === 'dashboard' ? 'active' : ''}
            onClick={() => setSelectedView('dashboard')}
          >
            📊 Dashboard
          </button>
          <button
            className={selectedView === 'analysis' ? 'active' : ''}
            onClick={() => setSelectedView('analysis')}
          >
            🔍 Analysis
          </button>
        </div>
      </div>

      <div className="address-list">
        <h3>Generated Addresses ({addresses.length})</h3>
        <div className="address-grid">
          {addresses.map(addr => (
            <motion.div
              key={addr.id}
              className={`address-card ${addr.type}`}
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              whileHover={{ scale: 1.02 }}
            >
              <div className="address-header">
                <span className="sensor-icon">
                  {addr.sensorType === 'temperature' && '🌡️'}
                  {addr.sensorType === 'air_quality' && '💨'}
                  {addr.sensorType === 'noise' && '🔊'}
                  {addr.sensorType === 'traffic' && '🚗'}
                </span>
                <span className="address-type">{addr.type}</span>
              </div>
              <div className="address-content">
                <p className="address-text">{addr.address}</p>
                <p className="coordinates">
                  {addr.lat.toFixed(4)}, {addr.lng.toFixed(4)}
                </p>
                <p className="sensor-type">{addr.sensorType} sensor</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      <div className="sample-content">
        {selectedView === 'map' && (
          <TelemetryMap
            devices={devicesFromAddresses}
            center={[-73.5387, 41.0534]}
            zoom={12}
            height="600px"
          />
        )}
        
        {selectedView === 'dashboard' && (
          <TelemetryDashboard
            metrics={[
              { id: 'temperature', name: 'Temperature', unit: '°C', color: '#FF5722', chartType: 'line' },
              { id: 'air_quality', name: 'Air Quality', unit: 'AQI', color: '#2196F3', chartType: 'area' },
              { id: 'noise', name: 'Noise Level', unit: 'dB', color: '#4CAF50', chartType: 'bar' },
              { id: 'traffic', name: 'Traffic', unit: '%', color: '#9C27B0', chartType: 'line' }
            ]}
          />
        )}
        
        {selectedView === 'analysis' && (
          <ProximityAnalysis
            devices={devicesFromAddresses}
          />
        )}
      </div>

      <style>{`
        .stamford-sample {
          padding: 20px;
          max-width: 1400px;
          margin: 0 auto;
        }

        .sample-header {
          text-align: center;
          margin-bottom: 30px;
        }

        .sample-header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .sample-header p {
          color: #666;
          font-size: 16px;
        }

        .sample-controls {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 20px;
          margin-bottom: 30px;
          flex-wrap: wrap;
        }

        .generate-button,
        .workflow-button {
          padding: 10px 20px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .generate-button {
          background: #2196F3;
          color: white;
        }

        .generate-button:hover:not(:disabled) {
          background: #1976D2;
        }

        .workflow-button {
          background: #4CAF50;
          color: white;
        }

        .workflow-button:hover {
          background: #45a049;
        }

        .view-selector {
          display: flex;
          gap: 5px;
          background: #f0f0f0;
          padding: 5px;
          border-radius: 4px;
        }

        .view-selector button {
          padding: 8px 16px;
          border: none;
          background: transparent;
          cursor: pointer;
          border-radius: 4px;
          transition: all 0.2s;
        }

        .view-selector button.active {
          background: white;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .address-list {
          margin-bottom: 30px;
        }

        .address-list h3 {
          margin: 0 0 20px 0;
          color: #333;
        }

        .address-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
          gap: 15px;
        }

        .address-card {
          background: white;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          padding: 15px;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
        }

        .address-card.residential {
          border-left: 4px solid #4CAF50;
        }

        .address-card.commercial {
          border-left: 4px solid #2196F3;
        }

        .address-card.industrial {
          border-left: 4px solid #FF9800;
        }

        .address-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;
        }

        .sensor-icon {
          font-size: 24px;
        }

        .address-type {
          font-size: 12px;
          padding: 2px 8px;
          background: #f0f0f0;
          border-radius: 4px;
          text-transform: uppercase;
        }

        .address-content p {
          margin: 5px 0;
          font-size: 14px;
        }

        .address-text {
          font-weight: 500;
          color: #333;
        }

        .coordinates {
          color: #666;
          font-size: 12px;
        }

        .sensor-type {
          color: #2196F3;
          font-size: 13px;
        }

        .sample-content {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
      `}</style>
    </div>
  );
};

export default StamfordGeospatialSample;