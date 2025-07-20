import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';

export interface StartNodeData {
  label: string;
  configuration?: {
    trigger?: 'manual' | 'scheduled' | 'event';
    schedule?: string;
    eventType?: string;
  };
}

const StartNode: React.FC<NodeProps<StartNodeData>> = ({ data, selected }) => {
  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.05 }}
      style={{
        background: '#4CAF50',
        color: 'white',
        padding: '10px 20px',
        borderRadius: '50px',
        border: selected ? '2px solid #2E7D32' : '2px solid transparent',
        minWidth: '120px',
        textAlign: 'center',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        cursor: 'pointer',
      }}
    >
      <div style={{ fontWeight: 'bold', fontSize: '14px' }}>
        <span style={{ marginRight: '8px' }}>▶</span>
        {data.label || 'Start'}
      </div>
      {data.configuration?.trigger && (
        <div style={{ fontSize: '11px', marginTop: '4px', opacity: 0.9 }}>
          {data.configuration.trigger === 'scheduled' && `⏰ ${data.configuration.schedule}`}
          {data.configuration.trigger === 'event' && `📨 ${data.configuration.eventType}`}
          {data.configuration.trigger === 'manual' && '👤 Manual'}
        </div>
      )}
      <Handle
        type="source"
        position={Position.Right}
        style={{
          background: '#2E7D32',
          width: '12px',
          height: '12px',
          border: '2px solid white',
        }}
      />
    </motion.div>
  );
};

export default memo(StartNode);