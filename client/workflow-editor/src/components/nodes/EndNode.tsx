import React from 'react';
import { Handle, Position } from 'react-flow-renderer';

const EndNode: React.FC<any> = ({ data }) => {
  return (
    <div style={{ 
      padding: '10px', 
      border: '2px solid #F44336', 
      borderRadius: '50%',
      background: '#FFEBEE',
      minWidth: '60px',
      textAlign: 'center'
    }}>
      <div>End</div>
      <Handle type="target" position={Position.Left} />
    </div>
  );
};

export default EndNode;