import React from 'react';
import { Handle, Position } from 'react-flow-renderer';

const TaskNode: React.FC<any> = ({ data }) => {
  return (
    <div style={{ 
      padding: '10px', 
      border: '2px solid #2196F3', 
      borderRadius: '4px',
      background: '#E3F2FD',
      minWidth: '100px',
      textAlign: 'center'
    }}>
      <div>{data.label || 'Task'}</div>
      <Handle type="target" position={Position.Left} />
      <Handle type="source" position={Position.Right} />
    </div>
  );
};

export default TaskNode;