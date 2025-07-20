import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';

export interface EndNodeData {
  label: string;
  configuration?: {
    status?: 'success' | 'failure' | 'cancelled';
    notification?: boolean;
    notificationChannels?: string[];
  };
}

const EndNode: React.FC<NodeProps<EndNodeData>> = ({ data, selected }) => {
  const getStatusColor = () => {
    switch (data.configuration?.status) {
      case 'success': return '#4CAF50';
      case 'failure': return '#f44336';
      case 'cancelled': return '#FF9800';
      default: return '#757575';
    }
  };

  const getStatusIcon = () => {
    switch (data.configuration?.status) {
      case 'success': return '✓';
      case 'failure': return '✗';
      case 'cancelled': return '⊘';
      default: return '■';
    }
  };

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.05 }}
      style={{
        background: getStatusColor(),
        color: 'white',
        padding: '10px 20px',
        borderRadius: '50px',
        border: selected ? '2px solid #424242' : '2px solid transparent',
        minWidth: '120px',
        textAlign: 'center',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        cursor: 'pointer',
      }}
    >
      <div style={{ fontWeight: 'bold', fontSize: '14px' }}>
        <span style={{ marginRight: '8px' }}>{getStatusIcon()}</span>
        {data.label || 'End'}
      </div>
      {data.configuration?.notification && (
        <div style={{ fontSize: '11px', marginTop: '4px', opacity: 0.9 }}>
          🔔 Notify: {data.configuration.notificationChannels?.join(', ') || 'Default'}
        </div>
      )}
      <Handle
        type="target"
        position={Position.Left}
        style={{
          background: '#424242',
          width: '12px',
          height: '12px',
          border: '2px solid white',
        }}
      />
    </motion.div>
  );
};

export default memo(EndNode);