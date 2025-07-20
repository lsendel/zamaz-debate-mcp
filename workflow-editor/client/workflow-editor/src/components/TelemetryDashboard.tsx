import React, { useState, useEffect, memo, useMemo, useCallback } from 'react';
import TelemetryChart, { TelemetryDataPoint } from './TelemetryChart';
import { motion } from 'framer-motion';
import { useWorkflowStore } from '../store/workflowStore';
import { Button, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Checkbox, Label } from '@zamaz/ui';
import { Play, Pause } from 'lucide-react';

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

  // Memoize time range calculation
  const timeRangeMs = useMemo(() => {
    const ranges = {
      '1m': 60 * 1000,
      '5m': 5 * 60 * 1000,
      '15m': 15 * 60 * 1000,
      '30m': 30 * 60 * 1000,
      '1h': 60 * 60 * 1000
    };
    return ranges[timeRange];
  }, [timeRange]);

  const filterDataByTimeRange = useCallback((data: TelemetryDataPoint[]) => {
    const now = Date.now();
    return data.filter(point => now - point.timestamp <= timeRangeMs);
  }, [timeRangeMs]);

  // Memoize filtered metrics
  const filteredMetrics = useMemo(() => {
    return metrics.filter(metric => selectedMetrics.includes(metric.id));
  }, [metrics, selectedMetrics]);

  return (
    <div className="p-5 bg-gray-50 min-h-screen">
      <div className="flex justify-between items-center mb-8 flex-wrap gap-5">
        <h2 className="text-2xl font-bold text-gray-900">Telemetry Dashboard</h2>
        <div className="flex items-center gap-5 flex-wrap">
          <Button
            variant={isSimulating ? 'danger' : 'primary'}
            onClick={() => setIsSimulating(!isSimulating)}
            leftIcon={isSimulating ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
          >
            {isSimulating ? 'Stop Simulation' : 'Start Simulation'}
          </Button>
          
          <Select value={timeRange} onValueChange={(value) => setTimeRange(value as any)}>
            <SelectTrigger className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1m">Last 1 minute</SelectItem>
              <SelectItem value="5m">Last 5 minutes</SelectItem>
              <SelectItem value="15m">Last 15 minutes</SelectItem>
              <SelectItem value="30m">Last 30 minutes</SelectItem>
              <SelectItem value="1h">Last 1 hour</SelectItem>
            </SelectContent>
          </Select>
          
          <div className="flex gap-4 flex-wrap">
            {metrics.map(metric => (
              <div key={metric.id} className="flex items-center gap-2">
                <Checkbox
                  id={metric.id}
                  checked={selectedMetrics.includes(metric.id)}
                  onCheckedChange={(checked) => {
                    if (checked) {
                      setSelectedMetrics([...selectedMetrics, metric.id]);
                    } else {
                      setSelectedMetrics(selectedMetrics.filter(id => id !== metric.id));
                    }
                  }}
                />
                <Label htmlFor={metric.id} style={{ color: metric.color }} className="cursor-pointer font-medium">
                  {metric.name}
                </Label>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className={`grid gap-5 ${layout === 'grid' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1'}`}>
        {filteredMetrics.map((metric, index) => (
            <motion.div
              key={metric.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
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
        <div className="bg-white rounded-lg p-10 text-center text-gray-500">
          <p>No metrics selected. Please select at least one metric to display.</p>
        </div>
      )}
    </div>
  );
};

export default memo(TelemetryDashboard);