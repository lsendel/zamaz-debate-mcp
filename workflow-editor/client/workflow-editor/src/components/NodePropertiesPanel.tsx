import React, { useState, useEffect } from 'react';
import { Node } from 'reactflow';
import { motion, AnimatePresence } from 'framer-motion';

interface NodePropertiesPanelProps {
  node: Node;
  onUpdate: (data: any) => void;
  onClose: () => void;
}

const NodePropertiesPanel: React.FC<NodePropertiesPanelProps> = ({ node, onUpdate, onClose }) => {
  const [formData, setFormData] = useState(node.data);

  useEffect(() => {
    setFormData(node.data);
  }, [node]);

  const handleChange = (field: string, value: any) => {
    const updatedData = {
      ...formData,
      [field]: value,
    };
    setFormData(updatedData);
    onUpdate(updatedData);
  };

  const handleConfigChange = (field: string, value: any) => {
    const updatedData = {
      ...formData,
      configuration: {
        ...formData.configuration,
        [field]: value,
      },
    };
    setFormData(updatedData);
    onUpdate(updatedData);
  };

  const renderNodeSpecificFields = () => {
    switch (node.type) {
      case 'start':
        return (
          <>
            <div className="form-group">
              <label>Trigger Type</label>
              <select
                value={formData.configuration?.trigger || 'manual'}
                onChange={(e) => handleConfigChange('trigger', e.target.value)}
              >
                <option value="manual">Manual</option>
                <option value="scheduled">Scheduled</option>
                <option value="event">Event</option>
              </select>
            </div>
            {formData.configuration?.trigger === 'scheduled' && (
              <div className="form-group">
                <label>Schedule (Cron)</label>
                <input
                  type="text"
                  value={formData.configuration?.schedule || ''}
                  onChange={(e) => handleConfigChange('schedule', e.target.value)}
                  placeholder="0 * * * *"
                />
              </div>
            )}
            {formData.configuration?.trigger === 'event' && (
              <div className="form-group">
                <label>Event Type</label>
                <input
                  type="text"
                  value={formData.configuration?.eventType || ''}
                  onChange={(e) => handleConfigChange('eventType', e.target.value)}
                  placeholder="telemetry.threshold"
                />
              </div>
            )}
          </>
        );

      case 'task':
        return (
          <>
            <div className="form-group">
              <label>Task Type</label>
              <input
                type="text"
                value={formData.configuration?.taskType || ''}
                onChange={(e) => handleConfigChange('taskType', e.target.value)}
                placeholder="HTTP_REQUEST"
              />
            </div>
            <div className="form-group">
              <label>Timeout (seconds)</label>
              <input
                type="number"
                value={formData.configuration?.timeout || 30}
                onChange={(e) => handleConfigChange('timeout', parseInt(e.target.value))}
              />
            </div>
            <div className="form-group">
              <label>Retry Count</label>
              <input
                type="number"
                value={formData.configuration?.retryCount || 0}
                onChange={(e) => handleConfigChange('retryCount', parseInt(e.target.value))}
              />
            </div>
            <div className="form-group">
              <label>
                <input
                  type="checkbox"
                  checked={formData.configuration?.parallel || false}
                  onChange={(e) => handleConfigChange('parallel', e.target.checked)}
                />
                Enable Parallel Execution
              </label>
            </div>
          </>
        );

      case 'decision':
        return (
          <>
            <div className="form-group">
              <label>Decision Type</label>
              <select
                value={formData.configuration?.decisionType || 'simple'}
                onChange={(e) => handleConfigChange('decisionType', e.target.value)}
              >
                <option value="simple">Simple Condition</option>
                <option value="complex">Complex Rules</option>
                <option value="telemetry">Telemetry Based</option>
              </select>
            </div>
            {formData.configuration?.decisionType === 'telemetry' && (
              <div className="form-group">
                <label>Telemetry Source</label>
                <input
                  type="text"
                  value={formData.configuration?.telemetrySource || ''}
                  onChange={(e) => handleConfigChange('telemetrySource', e.target.value)}
                  placeholder="sensor.temperature"
                />
              </div>
            )}
            <div className="form-group">
              <label>Condition Builder</label>
              <button className="condition-builder-btn">
                Open Condition Builder
              </button>
            </div>
          </>
        );

      case 'end':
        return (
          <>
            <div className="form-group">
              <label>End Status</label>
              <select
                value={formData.configuration?.status || 'success'}
                onChange={(e) => handleConfigChange('status', e.target.value)}
              >
                <option value="success">Success</option>
                <option value="failure">Failure</option>
                <option value="cancelled">Cancelled</option>
              </select>
            </div>
            <div className="form-group">
              <label>
                <input
                  type="checkbox"
                  checked={formData.configuration?.notification || false}
                  onChange={(e) => handleConfigChange('notification', e.target.checked)}
                />
                Send Notification
              </label>
            </div>
            {formData.configuration?.notification && (
              <div className="form-group">
                <label>Notification Channels</label>
                <input
                  type="text"
                  value={formData.configuration?.notificationChannels?.join(', ') || ''}
                  onChange={(e) => handleConfigChange('notificationChannels', 
                    e.target.value.split(',').map(s => s.trim()).filter(Boolean)
                  )}
                  placeholder="email, slack, webhook"
                />
              </div>
            )}
          </>
        );

      default:
        return null;
    }
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ x: 300, opacity: 0 }}
        animate={{ x: 0, opacity: 1 }}
        exit={{ x: 300, opacity: 0 }}
        style={{
          position: 'absolute',
          right: 0,
          top: 0,
          bottom: 0,
          width: '300px',
          background: 'white',
          borderLeft: '1px solid #e0e0e0',
          padding: '20px',
          overflowY: 'auto',
          boxShadow: '-2px 0 5px rgba(0, 0, 0, 0.1)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h3 style={{ margin: 0, fontSize: '18px', color: '#333' }}>
            Node Properties
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '20px',
              cursor: 'pointer',
              padding: '0',
              width: '30px',
              height: '30px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              borderRadius: '4px',
            }}
          >
            ✕
          </button>
        </div>

        <div className="form-group">
          <label>Node ID</label>
          <input type="text" value={node.id} disabled />
        </div>

        <div className="form-group">
          <label>Label</label>
          <input
            type="text"
            value={formData.label || ''}
            onChange={(e) => handleChange('label', e.target.value)}
          />
        </div>

        {renderNodeSpecificFields()}

        <style>{`
          .form-group {
            margin-bottom: 15px;
          }
          
          .form-group label {
            display: block;
            margin-bottom: 5px;
            font-size: 14px;
            font-weight: 500;
            color: #555;
          }
          
          .form-group input[type="text"],
          .form-group input[type="number"],
          .form-group select {
            width: 100%;
            padding: 8px 12px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 14px;
            transition: border-color 0.2s;
          }
          
          .form-group input[type="text"]:focus,
          .form-group input[type="number"]:focus,
          .form-group select:focus {
            outline: none;
            border-color: #2196F3;
          }
          
          .form-group input[type="checkbox"] {
            margin-right: 8px;
          }
          
          .form-group input:disabled {
            background: #f5f5f5;
            cursor: not-allowed;
          }
          
          .condition-builder-btn {
            width: 100%;
            padding: 10px;
            background: #2196F3;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 14px;
            transition: background 0.2s;
          }
          
          .condition-builder-btn:hover {
            background: #1976D2;
          }
        `}</style>
      </motion.div>
    </AnimatePresence>
  );
};

export default NodePropertiesPanel;