import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';

export interface DecisionNodeData {
  label: string;
  configuration?: {
    condition?: {
      field: string;
      operator: string;
      value: any;
      combinator?: 'AND' | 'OR';
      rules?: any[];
    };
    decisionType?: 'simple' | 'complex' | 'telemetry';
    telemetrySource?: string;
  };
  evaluationResult?: boolean | null;
}

const DecisionNode: React.FC<NodeProps<DecisionNodeData>> = ({ data, selected }) => {
  const getEvaluationStyle = () => {
    if (data.evaluationResult === true) {
      return { borderColor: '#4CAF50', borderWidth: '3px' };
    } else if (data.evaluationResult === false) {
      return { borderColor: '#f44336', borderWidth: '3px' };
    }
    return {};
  };

  const formatCondition = () => {
    if (!data.configuration?.condition) return 'No condition';
    const { field, operator, value } = data.configuration.condition;
    return `${field} ${operator} ${value}`;
  };

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.02 }}
      style={{
        background: '#FF9800',
        color: 'white',
        padding: '15px',
        borderRadius: '8px',
        border: selected ? '2px solid #F57C00' : '2px solid transparent',
        minWidth: '200px',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
        cursor: 'pointer',
        position: 'relative',
        transform: 'rotate(45deg)',
        ...getEvaluationStyle(),
      }}
    >
      <div
        style={{
          transform: 'rotate(-45deg)',
          textAlign: 'center',
        }}
      >
        <div style={{ fontWeight: 'bold', fontSize: '14px', marginBottom: '8px' }}>
          <span style={{ marginRight: '8px' }}>❓</span>
          {data.label || 'Decision'}
        </div>
        
        {data.configuration?.decisionType && (
          <div style={{ fontSize: '11px', opacity: 0.9, marginBottom: '4px' }}>
            Type: {data.configuration.decisionType}
          </div>
        )}
        
        <div style={{ fontSize: '10px', opacity: 0.8 }}>
          {formatCondition()}
        </div>
        
        {data.configuration?.telemetrySource && (
          <div style={{ fontSize: '10px', opacity: 0.8, marginTop: '4px' }}>
            📊 {data.configuration.telemetrySource}
          </div>
        )}
      </div>
      
      <Handle
        type="target"
        position={Position.Left}
        style={{
          background: '#F57C00',
          width: '12px',
          height: '12px',
          border: '2px solid white',
          left: '50%',
          top: '-6px',
          transform: 'translateX(-50%) rotate(-45deg)',
        }}
      />
      
      <Handle
        type="source"
        position={Position.Right}
        id="true"
        style={{
          background: '#4CAF50',
          width: '12px',
          height: '12px',
          border: '2px solid white',
          right: '-6px',
          top: '50%',
          transform: 'translateY(-50%) rotate(-45deg)',
        }}
      />
      
      <Handle
        type="source"
        position={Position.Bottom}
        id="false"
        style={{
          background: '#f44336',
          width: '12px',
          height: '12px',
          border: '2px solid white',
          bottom: '-6px',
          left: '50%',
          transform: 'translateX(-50%) rotate(-45deg)',
        }}
      />
      
      <motion.div
        style={{
          position: 'absolute',
          top: '-20px',
          right: '-20px',
          background: '#4CAF50',
          color: 'white',
          borderRadius: '4px',
          padding: '2px 6px',
          fontSize: '10px',
          transform: 'rotate(-45deg)',
          display: data.evaluationResult === true ? 'block' : 'none',
        }}
      >
        TRUE
      </motion.div>
      
      <motion.div
        style={{
          position: 'absolute',
          bottom: '-20px',
          left: '-20px',
          background: '#f44336',
          color: 'white',
          borderRadius: '4px',
          padding: '2px 6px',
          fontSize: '10px',
          transform: 'rotate(-45deg)',
          display: data.evaluationResult === false ? 'block' : 'none',
        }}
      >
        FALSE
      </motion.div>
    </motion.div>
  );
};

export default memo(DecisionNode);