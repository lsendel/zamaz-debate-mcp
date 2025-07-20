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
import { motion } from 'framer-motion';
import { Card, CardContent, CardHeader, CardTitle, Button, Badge } from '@zamaz/ui';
import { Play, Pause } from 'lucide-react';

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
  color = '#0066ff',
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
      stroke: '#6b7280',
      style: { fontSize: 12 }
    };

    const renderContent = () => {
      switch (chartType) {
        case 'area':
          return (
            <AreaChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
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
              />
              {showBrush && <Brush dataKey="time" height={30} stroke={color} />}
            </AreaChart>
          );

        case 'bar':
          return (
            <BarChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
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
              />
            </BarChart>
          );

        case 'scatter':
          return (
            <ScatterChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
              <XAxis dataKey="time" {...commonAxisProps} />
              <YAxis dataKey="value" {...commonAxisProps} />
              <Tooltip content={<CustomTooltip unit={unit} />} />
              <Legend />
              <Scatter
                name={metric}
                data={displayData}
                fill={color}
              />
            </ScatterChart>
          );

        default:
          return (
            <LineChart {...commonProps}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
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
    >
      <Card>
        <CardHeader className="pb-3">
          <div className="flex justify-between items-center">
            <CardTitle>{title}</CardTitle>
            <div className="flex items-center gap-3">
              {realtime && (
                <Button
                  variant={isLive ? "primary" : "secondary"}
                  size="sm"
                  onClick={() => setIsLive(!isLive)}
                  leftIcon={isLive ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                >
                  {isLive ? 'Pause' : 'Live'}
                </Button>
              )}
              <Badge variant="secondary">
                {displayData.length} points
              </Badge>
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          <div ref={chartRef}>
            {displayData.length > 0 ? (
              renderChart()
            ) : (
              <div className="flex items-center justify-center h-48 text-gray-500">
                No telemetry data available
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
};

// Custom tooltip component
const CustomTooltip: React.FC<any> = ({ active, payload, label, unit }) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-gray-900 text-white p-3 rounded shadow-lg text-sm">
        <p className="mb-1">{label}</p>
        <p className="font-bold">
          {payload[0].name}: {payload[0].value} {unit}
        </p>
        {payload[0].payload.deviceId && (
          <p className="mt-1 text-gray-300 text-xs">
            Device: {payload[0].payload.deviceId}
          </p>
        )}
      </div>
    );
  }
  return null;
};

export default TelemetryChart;