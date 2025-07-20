import React, { useState, useEffect } from 'react';
import TelemetryChart, { TelemetryDataPoint } from './TelemetryChart';
import { motion } from 'framer-motion';
import { useWorkflowStore } from '../store/workflowStore';

interface TelemetryMetric {
  id: string;
  name: string;
  unit: string;
  color: string;
  chartType: 'line' | 'area' | 'bar' | 'scatter';
  threshold?: {
    value: number;
    label: string;
    color: string;
  };
}

const defaultMetrics: TelemetryMetric[] = [
  {
    id: 'temperature',
    name: 'Temperature',
    unit: '°C',
    color: '#FF5722',
    chartType: 'line',
    threshold: {
      value: 35,
      label: 'Critical',
      color: '#f44336'
    }
  },
  {
    id: 'pressure',
    name: 'Pressure',
    unit: 'kPa',
    color: '#2196F3',
    chartType: 'area'
  },
  {
    id: 'humidity',
    name: 'Humidity',
    unit: '%',
    color: '#4CAF50',
    chartType: 'line',
    threshold: {
      value: 80,
      label: 'High',
      color: '#FF9800'
    }
  },
  {
    id: 'speed',
    name: 'Speed',
    unit: 'km/h',
    color: '#9C27B0',
    chartType: 'bar'
  }
];

interface TelemetryDashboardProps {
  sources?: string[];
  metrics?: TelemetryMetric[];
  layout?: 'grid' | 'list';
  refreshInterval?: number;
  onMetricClick?: (metric: TelemetryMetric, dataPoint: TelemetryDataPoint) => void;
}

const TelemetryDashboard: React.FC<TelemetryDashboardProps> = ({
  sources = [],
  metrics = defaultMetrics,
  layout = 'grid',
  refreshInterval = 1000,
  onMetricClick
}) => {
  const { telemetryData, addTelemetryData } = useWorkflowStore();
  const [selectedMetrics, setSelectedMetrics] = useState<string[]>(
    metrics.map(m => m.id)
  );
  const [timeRange, setTimeRange] = useState<'1m' | '5m' | '15m' | '30m' | '1h'>('5m');
  const [isSimulating, setIsSimulating] = useState(false);

  // Simulate telemetry data for demo
  useEffect(() => {
    if (!isSimulating) return;

    const interval = setInterval(() => {
      metrics.forEach(metric => {
        const baseValue = metric.id === 'temperature' ? 25 : 
                         metric.id === 'pressure' ? 101 :
                         metric.id === 'humidity' ? 60 : 50;
        
        const variation = (Math.random() - 0.5) * 10;
        const value = baseValue + variation;
        
        addTelemetryData(metric.id, {
          timestamp: Date.now(),
          value,
          deviceId: `sensor-${Math.floor(Math.random() * 5) + 1}`,
          metadata: {
            quality: Math.random() > 0.1 ? 'good' : 'poor'
          }
        });
      });
    }, refreshInterval);

    return () => clearInterval(interval);
  }, [isSimulating, metrics, refreshInterval, addTelemetryData]);

  const getTimeRangeMs = () => {
    const ranges = {
      '1m': 60 * 1000,
      '5m': 5 * 60 * 1000,
      '15m': 15 * 60 * 1000,
      '30m': 30 * 60 * 1000,
      '1h': 60 * 60 * 1000
    };
    return ranges[timeRange];
  };

  const filterDataByTimeRange = (data: TelemetryDataPoint[]) => {
    const now = Date.now();
    const rangeMs = getTimeRangeMs();
    return data.filter(point => now - point.timestamp <= rangeMs);
  };

  return (
    <div className="telemetry-dashboard">
      <div className="dashboard-header">
        <h2>Telemetry Dashboard</h2>
        <div className="dashboard-controls">
          <button
            className={`simulate-button ${isSimulating ? 'active' : ''}`}
            onClick={() => setIsSimulating(!isSimulating)}
          >
            {isSimulating ? '⏹ Stop Simulation' : '▶ Start Simulation'}
          </button>
          
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value as any)}
            className="time-range-select"
          >
            <option value="1m">Last 1 minute</option>
            <option value="5m">Last 5 minutes</option>
            <option value="15m">Last 15 minutes</option>
            <option value="30m">Last 30 minutes</option>
            <option value="1h">Last 1 hour</option>
          </select>
          
          <div className="metric-toggles">
            {metrics.map(metric => (
              <label key={metric.id} className="metric-toggle">
                <input
                  type="checkbox"
                  checked={selectedMetrics.includes(metric.id)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setSelectedMetrics([...selectedMetrics, metric.id]);
                    } else {
                      setSelectedMetrics(selectedMetrics.filter(id => id !== metric.id));
                    }
                  }}
                />
                <span style={{ color: metric.color }}>{metric.name}</span>
              </label>
            ))}
          </div>
        </div>
      </div>

      <div className={`charts-container ${layout}`}>
        {metrics
          .filter(metric => selectedMetrics.includes(metric.id))
          .map((metric, index) => (
            <motion.div
              key={metric.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              className="chart-wrapper"
            >
              <TelemetryChart
                data={filterDataByTimeRange(telemetryData[metric.id] || [])}
                title={metric.name}
                metric={metric.name}
                unit={metric.unit}
                chartType={metric.chartType}
                color={metric.color}
                threshold={metric.threshold}
                showBrush={layout === 'list'}
                height={layout === 'grid' ? 250 : 300}
                realtime={isSimulating}
                onDataPointClick={(point) => onMetricClick?.(metric, point)}
              />
            </motion.div>
          ))}
      </div>

      {selectedMetrics.length === 0 && (
        <div className="no-metrics-selected">
          <p>No metrics selected. Please select at least one metric to display.</p>
        </div>
      )}

      <style>{`
        .telemetry-dashboard {
          padding: 20px;
          background: #f5f5f5;
          min-height: 100vh;
        }

        .dashboard-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 30px;
          flex-wrap: wrap;
          gap: 20px;
        }

        .dashboard-header h2 {
          margin: 0;
          color: #333;
        }

        .dashboard-controls {
          display: flex;
          align-items: center;
          gap: 20px;
          flex-wrap: wrap;
        }

        .simulate-button {
          padding: 10px 20px;
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

        .simulate-button.active:hover {
          background: #d32f2f;
        }

        .time-range-select {
          padding: 8px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
          background: white;
          font-size: 14px;
          cursor: pointer;
        }

        .metric-toggles {
          display: flex;
          gap: 15px;
          flex-wrap: wrap;
        }

        .metric-toggle {
          display: flex;
          align-items: center;
          gap: 5px;
          cursor: pointer;
          font-size: 14px;
        }

        .metric-toggle input[type="checkbox"] {
          cursor: pointer;
        }

        .charts-container {
          display: grid;
          gap: 20px;
        }

        .charts-container.grid {
          grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
        }

        .charts-container.list {
          grid-template-columns: 1fr;
        }

        .chart-wrapper {
          min-width: 0;
        }

        .no-metrics-selected {
          background: white;
          border-radius: 8px;
          padding: 40px;
          text-align: center;
          color: #666;
        }

        @media (max-width: 768px) {
          .dashboard-header {
            flex-direction: column;
            align-items: stretch;
          }

          .dashboard-controls {
            flex-direction: column;
            align-items: stretch;
          }

          .metric-toggles {
            justify-content: center;
          }

          .charts-container.grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>
    </div>
  );
};

export default TelemetryDashboard;