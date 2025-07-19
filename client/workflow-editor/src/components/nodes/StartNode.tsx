import React from 'react';
import { Handle, Position } from 'react-flow-renderer';

const StartNode: React.FC<any> = ({ data }) => {
  return (
    <div style={{ 
      padding: '10px', 
      border: '2px solid #4CAF50', 
      borderRadius: '50%',
      background: '#E8F5E8',
      minWidth: '60px',
      textAlign: 'center'
    }}>
      <div>Start</div>
      <Handle type="source" position={Position.Right} />
    </div>
  );
};

export default StartNode;