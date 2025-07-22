import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';

export interface TelemetryAlert {
  id: string;
  timestamp: number;
  source: string;
  severity: 'info' | 'warning' | 'error' | 'critical';
  title: string;
  message: string;
  metric?: string;
  value?: number;
  threshold?: number;
  acknowledged?: boolean;
}

interface TelemetryAlertPanelProps {
  alerts: TelemetryAlert[];
  onAcknowledge?: (alertId: string) => void;
  onClear?: (alertId: string) => void;
  onClearAll?: () => void;
  maxAlerts?: number;
  position?: 'top-right' | 'bottom-right' | 'top-left' | 'bottom-left';
}

const TelemetryAlertPanel: React.FC<TelemetryAlertPanelProps> = ({
  alerts,
  onAcknowledge,
  onClear,
  onClearAll,
  maxAlerts = 10,
  position = 'top-right'
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [filter, setFilter] = useState<'all' | 'unacknowledged'>('unacknowledged');

  const filteredAlerts = alerts
    .filter(alert => filter === 'all' || !alert.acknowledged)
    .slice(0, maxAlerts);

  const unacknowledgedCount = alerts.filter(a => !a.acknowledged).length;

  const getSeverityColor = (severity: TelemetryAlert['severity']) => {
    switch (severity) {
      case 'critical': return '#f44336';
      case 'error': return '#ff5722';
      case 'warning': return '#ff9800';
      case 'info': return '#2196f3';
      default: return '#666';
    }
  };

  const getSeverityIcon = (severity: TelemetryAlert['severity']) => {
    switch (severity) {
      case 'critical': return '🚨';
      case 'error': return '❌';
      case 'warning': return '⚠️';
      case 'info': return 'ℹ️';
      default: return '📋';
    }
  };

  const getPositionStyles = () => {
    const base = {
      position: 'fixed' as const,
      zIndex: 1000,
      margin: '20px'
    };

    switch (position) {
      case 'top-right':
        return { ...base, top: 0, right: 0 };
      case 'bottom-right':
        return { ...base, bottom: 0, right: 0 };
      case 'top-left':
        return { ...base, top: 0, left: 0 };
      case 'bottom-left':
        return { ...base, bottom: 0, left: 0 };
    }
  };

  return (
    <div style={getPositionStyles()} className="telemetry-alert-panel">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="alert-panel-container"
      >
        <div className="alert-panel-header" onClick={() => setIsExpanded(!isExpanded)}>
          <div className="header-title">
            <span className="alert-icon">🔔</span>
            <span>Alerts</span>
            {unacknowledgedCount > 0 && (
              <span className="alert-badge">{unacknowledgedCount}</span>
            )}
          </div>
          <button className="expand-button">
            {isExpanded ? '▼' : '▲'}
          </button>
        </div>

        <AnimatePresence>
          {isExpanded && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="alert-panel-content"
            >
              <div className="alert-controls">
                <div className="filter-buttons">
                  <button
                    className={filter === 'unacknowledged' ? 'active' : ''}
                    onClick={() => setFilter('unacknowledged')}
                  >
                    Unacknowledged
                  </button>
                  <button
                    className={filter === 'all' ? 'active' : ''}
                    onClick={() => setFilter('all')}
                  >
                    All
                  </button>
                </div>
                {onClearAll && filteredAlerts.length > 0 && (
                  <button className="clear-all-button" onClick={onClearAll}>
                    Clear All
                  </button>
                )}
              </div>

              <div className="alerts-list">
                <AnimatePresence>
                  {filteredAlerts.map((alert) => (
                    <motion.div
                      key={alert.id}
                      initial={{ opacity: 0, x: 50 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: -50 }}
                      className={`alert-item ${alert.acknowledged ? 'acknowledged' : ''}`}
                      style={{ borderLeftColor: getSeverityColor(alert.severity) }}
                    >
                      <div className="alert-content">
                        <div className="alert-header">
                          <span className="severity-icon">
                            {getSeverityIcon(alert.severity)}
                          </span>
                          <span className="alert-title">{alert.title}</span>
                          <span className="alert-time">
                            {new Date(alert.timestamp).toLocaleTimeString()}
                          </span>
                        </div>
                        <div className="alert-message">{alert.message}</div>
                        {alert.metric && (
                          <div className="alert-details">
                            <span className="metric-name">{alert.metric}:</span>
                            <span className="metric-value">{alert.value}</span>
                            {alert.threshold && (
                              <span className="threshold">
                                (threshold: {alert.threshold})
                              </span>
                            )}
                          </div>
                        )}
                      </div>
                      <div className="alert-actions">
                        {!alert.acknowledged && onAcknowledge && (
                          <button
                            className="acknowledge-button"
                            onClick={() => onAcknowledge(alert.id)}
                            title="Acknowledge"
                          >
                            ✓
                          </button>
                        )}
                        {onClear && (
                          <button
                            className="clear-button"
                            onClick={() => onClear(alert.id)}
                            title="Clear"
                          >
                            ✕
                          </button>
                        )}
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>

                {filteredAlerts.length === 0 && (
                  <div className="no-alerts">
                    No {filter === 'unacknowledged' ? 'unacknowledged ' : ''}alerts
                  </div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      <style>{`
        .telemetry-alert-panel {
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }

        .alert-panel-container {
          background: white;
          border-radius: 8px;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
          width: 400px;
          max-width: 90vw;
        }

        .alert-panel-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 15px 20px;
          border-bottom: 1px solid #e0e0e0;
          cursor: pointer;
          user-select: none;
        }

        .header-title {
          display: flex;
          align-items: center;
          gap: 10px;
          font-weight: 600;
          color: #333;
        }

        .alert-icon {
          font-size: 20px;
        }

        .alert-badge {
          background: #f44336;
          color: white;
          border-radius: 10px;
          padding: 2px 8px;
          font-size: 12px;
          font-weight: bold;
        }

        .expand-button {
          background: none;
          border: none;
          font-size: 12px;
          cursor: pointer;
          padding: 5px;
          color: #666;
        }

        .alert-panel-content {
          overflow: hidden;
        }

        .alert-controls {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 20px;
          border-bottom: 1px solid #e0e0e0;
        }

        .filter-buttons {
          display: flex;
          gap: 5px;
        }

        .filter-buttons button {
          padding: 5px 12px;
          border: 1px solid #ddd;
          background: white;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .filter-buttons button.active {
          background: #2196F3;
          color: white;
          border-color: #2196F3;
        }

        .clear-all-button {
          padding: 5px 12px;
          border: 1px solid #f44336;
          background: white;
          color: #f44336;
          border-radius: 4px;
          font-size: 12px;
          cursor: pointer;
          transition: all 0.2s;
        }

        .clear-all-button:hover {
          background: #f44336;
          color: white;
        }

        .alerts-list {
          max-height: 400px;
          overflow-y: auto;
        }

        .alert-item {
          padding: 15px 20px;
          border-bottom: 1px solid #f0f0f0;
          border-left: 4px solid;
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          transition: background 0.2s;
        }

        .alert-item:hover {
          background: #f9f9f9;
        }

        .alert-item.acknowledged {
          opacity: 0.6;
        }

        .alert-content {
          flex: 1;
        }

        .alert-header {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 5px;
        }

        .severity-icon {
          font-size: 16px;
        }

        .alert-title {
          font-weight: 500;
          color: #333;
          flex: 1;
        }

        .alert-time {
          font-size: 12px;
          color: #999;
        }

        .alert-message {
          font-size: 14px;
          color: #666;
          margin-bottom: 5px;
        }

        .alert-details {
          font-size: 12px;
          color: #999;
          display: flex;
          gap: 10px;
        }

        .metric-value {
          font-weight: 600;
          color: #333;
        }

        .alert-actions {
          display: flex;
          gap: 5px;
          margin-left: 10px;
        }

        .acknowledge-button,
        .clear-button {
          width: 30px;
          height: 30px;
          border-radius: 4px;
          border: 1px solid #ddd;
          background: white;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          transition: all 0.2s;
        }

        .acknowledge-button:hover {
          background: #4CAF50;
          color: white;
          border-color: #4CAF50;
        }

        .clear-button:hover {
          background: #f44336;
          color: white;
          border-color: #f44336;
        }

        .no-alerts {
          padding: 40px;
          text-align: center;
          color: #999;
          font-size: 14px;
        }
      `}</style>
    </div>
  );
};

export default TelemetryAlertPanel;