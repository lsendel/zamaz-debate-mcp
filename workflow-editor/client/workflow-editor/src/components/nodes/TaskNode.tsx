import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';

export interface TaskNodeData {
  label: string;
  configuration?: {
    taskType?: string;
    timeout?: number;
    retryCount?: number;
    retryDelay?: number;
    parallel?: boolean;
    inputs?: Record<string, any>;
    outputs?: Record<string, any>;
  };
  status?: 'idle' | 'running' | 'completed' | 'failed';
}

const TaskNode: React.FC<NodeProps<TaskNodeData>> = ({ data, selected }) => {
  const getStatusStyle = () => {
    switch (data.status) {
      case 'running':
        return {
          background: '#2196F3',
          animation: 'pulse 2s infinite',
        };
      case 'completed':
        return { background: '#4CAF50' };
      case 'failed':
        return { background: '#f44336' };
      default:
        return { background: '#2196F3' };
    }
  };

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.02 }}
      style={{
        ...getStatusStyle(),
        color: 'white',
        padding: '15px',
        borderRadius: '8px',
        border: selected ? '2px solid #1976D2' : '2px solid transparent',
        minWidth: '180px',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        cursor: 'pointer',
        position: 'relative',
      }}
    >
      <Handle
        type="target"
        position={Position.Left}
        style={{
          background: '#1976D2',
          width: '12px',
          height: '12px',
          border: '2px solid white',
        }}
      />
      
      <div style={{ fontWeight: 'bold', fontSize: '14px', marginBottom: '8px' }}>
        <span style={{ marginRight: '8px' }}>⚡</span>
        {data.label || 'Task'}
      </div>
      
      {data.configuration?.taskType && (
        <div style={{ fontSize: '12px', opacity: 0.9, marginBottom: '4px' }}>
          Type: {data.configuration.taskType}
        </div>
      )}
      
      <div style={{ fontSize: '11px', opacity: 0.8 }}>
        {data.configuration?.timeout && (
          <div>⏱ Timeout: {data.configuration.timeout}s</div>
        )}
        {data.configuration?.retryCount && (
          <div>🔄 Retries: {data.configuration.retryCount}</div>
        )}
        {data.configuration?.parallel && (
          <div>⚡ Parallel execution</div>
        )}
      </div>
      
      {data.status === 'running' && (
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          style={{
            position: 'absolute',
            top: '5px',
            right: '5px',
            width: '16px',
            height: '16px',
          }}
        >
          ⚙️
        </motion.div>
      )}
      
      <Handle
        type="source"
        position={Position.Right}
        style={{
          background: '#1976D2',
          width: '12px',
          height: '12px',
          border: '2px solid white',
        }}
      />
      
      <style>{`
        @keyframes pulse {
          0% { opacity: 1; }
          50% { opacity: 0.7; }
          100% { opacity: 1; }
        }
      `}</style>
    </motion.div>
  );
};

export default memo(TaskNode);