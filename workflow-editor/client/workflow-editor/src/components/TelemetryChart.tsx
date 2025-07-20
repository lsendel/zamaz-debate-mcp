import React, { useState, useEffect, useRef } from 'react';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  ScatterChart,
  Scatter,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ReferenceLine,
  Brush
} from 'recharts';
import { motion, AnimatePresence } from 'framer-motion';

export interface TelemetryDataPoint {
  timestamp: number;
  value: number;
  deviceId?: string;
  metadata?: Record<string, any>;
}

interface TelemetryChartProps {
  data: TelemetryDataPoint[];
  title: string;
  metric: string;
  unit?: string;
  chartType?: 'line' | 'area' | 'bar' | 'scatter';
  color?: string;
  showBrush?: boolean;
  threshold?: {
    value: number;
    label: string;
    color: string;
  };
  onDataPointClick?: (point: TelemetryDataPoint) => void;
  height?: number;
  realtime?: boolean;
  maxDataPoints?: number;
}

const TelemetryChart: React.FC<TelemetryChartProps> = ({
  data,
  title,
  metric,
  unit = '',
  chartType = 'line',
  color = '#2196F3',
  showBrush = false,
  threshold,
  onDataPointClick,
  height = 300,
  realtime = false,
  maxDataPoints = 100
}) => {
  const [displayData, setDisplayData] = useState<TelemetryDataPoint[]>([]);
  const [isLive, setIsLive] = useState(realtime);
  const chartRef = useRef<any>(null);

  useEffect(() => {
    // Format data for display
    const formattedData = data
      .slice(-maxDataPoints)
      .map(point => ({
        ...point,
        time: new Date(point.timestamp).toLocaleTimeString(),
        displayValue: point.value.toFixed(2)
      }));
    
    setDisplayData(formattedData);
  }, [data, maxDataPoints]);

  const renderChart = () => {
    const commonProps = {
      data: displayData,
      margin: { top: 5, right: 30, left: 20, bottom: 5 }
    };

    const commonAxisProps = {
      stroke: '#666',
      style: { fontSize: 12 }
    };

    const renderContent = () => {
      switch (chartType) {
        case 'area':
          return (
            <AreaChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="time" {...commonAxisProps} />
              <YAxis {...commonAxisProps} />
              <Tooltip content={<CustomTooltip unit={unit} />} />
              <Legend />
              {threshold && (
                <ReferenceLine 
                  y={threshold.value} 
                  label={threshold.label}
                  stroke={threshold.color}
                  strokeDasharray="5 5"
                />
              )}
              <Area
                type="monotone"
                dataKey="value"
                stroke={color}
                fill={color}
                fillOpacity={0.3}
                name={metric}
                onClick={onDataPointClick}
              />
              {showBrush && <Brush dataKey="time" height={30} stroke={color} />}
            </AreaChart>
          );

        case 'bar':
          return (
            <BarChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="time" {...commonAxisProps} />
              <YAxis {...commonAxisProps} />
              <Tooltip content={<CustomTooltip unit={unit} />} />
              <Legend />
              {threshold && (
                <ReferenceLine 
                  y={threshold.value} 
                  label={threshold.label}
                  stroke={threshold.color}
                  strokeDasharray="5 5"
                />
              )}
              <Bar
                dataKey="value"
                fill={color}
                name={metric}
                onClick={onDataPointClick}
              />
            </BarChart>
          );

        case 'scatter':
          return (
            <ScatterChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="time" {...commonAxisProps} />
              <YAxis dataKey="value" {...commonAxisProps} />
              <Tooltip content={<CustomTooltip unit={unit} />} />
              <Legend />
              <Scatter
                name={metric}
                data={displayData}
                fill={color}
                onClick={onDataPointClick}
              />
            </ScatterChart>
          );

        default:
          return (
            <LineChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="time" {...commonAxisProps} />
              <YAxis {...commonAxisProps} />
              <Tooltip content={<CustomTooltip unit={unit} />} />
              <Legend />
              {threshold && (
                <ReferenceLine 
                  y={threshold.value} 
                  label={threshold.label}
                  stroke={threshold.color}
                  strokeDasharray="5 5"
                />
              )}
              <Line
                type="monotone"
                dataKey="value"
                stroke={color}
                strokeWidth={2}
                dot={{ fill: color, r: 3 }}
                activeDot={{ r: 5 }}
                name={metric}
                onClick={onDataPointClick}
              />
              {showBrush && <Brush dataKey="time" height={30} stroke={color} />}
            </LineChart>
          );
      }
    };

    return (
      <ResponsiveContainer width="100%" height={height}>
        {renderContent()}
      </ResponsiveContainer>
    );
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="telemetry-chart-container"
    >
      <div className="chart-header">
        <h3>{title}</h3>
        <div className="chart-controls">
          {realtime && (
            <button
              className={`live-button ${isLive ? 'active' : ''}`}
              onClick={() => setIsLive(!isLive)}
            >
              {isLive ? '⏸ Pause' : '▶ Live'}
            </button>
          )}
          <span className="data-count">
            {displayData.length} points
          </span>
        </div>
      </div>
      
      <div className="chart-wrapper" ref={chartRef}>
        {renderChart()}
      </div>

      {displayData.length === 0 && (
        <div className="no-data">
          <span>No telemetry data available</span>
        </div>
      )}

      <style>{`
        .telemetry-chart-container {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
          margin-bottom: 20px;
        }

        .chart-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
        }

        .chart-header h3 {
          margin: 0;
          color: #333;
          font-size: 18px;
        }

        .chart-controls {
          display: flex;
          align-items: center;
          gap: 15px;
        }

        .live-button {
          padding: 6px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
          background: white;
          cursor: pointer;
          font-size: 14px;
          transition: all 0.2s;
        }

        .live-button.active {
          background: #4CAF50;
          color: white;
          border-color: #4CAF50;
        }

        .live-button:hover {
          border-color: #999;
        }

        .data-count {
          font-size: 14px;
          color: #666;
        }

        .chart-wrapper {
          position: relative;
        }

        .no-data {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 200px;
          color: #999;
          font-size: 14px;
        }
      `}</style>
    </motion.div>
  );
};

// Custom tooltip component
const CustomTooltip: React.FC<any> = ({ active, payload, label, unit }) => {
  if (active && payload && payload.length) {
    return (
      <div style={{
        background: 'rgba(0, 0, 0, 0.8)',
        color: 'white',
        padding: '10px',
        borderRadius: '4px',
        fontSize: '12px'
      }}>
        <p style={{ margin: '0 0 5px 0' }}>{label}</p>
        <p style={{ margin: 0, fontWeight: 'bold' }}>
          {payload[0].name}: {payload[0].value} {unit}
        </p>
        {payload[0].payload.deviceId && (
          <p style={{ margin: '5px 0 0 0', opacity: 0.8 }}>
            Device: {payload[0].payload.deviceId}
          </p>
        )}
      </div>
    );
  }
  return null;
};

export default TelemetryChart;