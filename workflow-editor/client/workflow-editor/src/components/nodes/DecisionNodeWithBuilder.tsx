import React, { memo, useState } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion, AnimatePresence } from 'framer-motion';
import { RuleGroupType } from 'react-querybuilder';
import ConditionBuilder from '../ConditionBuilder';
import Modal from '../Modal';

export interface DecisionNodeData {
  label: string;
  configuration?: {
    condition?: RuleGroupType;
    decisionType?: 'simple' | 'complex' | 'telemetry';
    telemetrySource?: string;
  };
  evaluationResult?: boolean | null;
}

const DecisionNodeWithBuilder: React.FC<NodeProps<DecisionNodeData>> = ({ 
  data, 
  selected,
  id
}) => {
  const [showBuilder, setShowBuilder] = useState(false);
  const [localCondition, setLocalCondition] = useState<RuleGroupType>(
    data.configuration?.condition || {
      combinator: 'and',
      rules: []
    }
  );

  const getEvaluationStyle = () => {
    if (data.evaluationResult === true) {
      return { borderColor: '#4CAF50', borderWidth: '3px' };
    } else if (data.evaluationResult === false) {
      return { borderColor: '#f44336', borderWidth: '3px' };
    }
    return {};
  };

  const formatConditionSummary = () => {
    if (!data.configuration?.condition || data.configuration.condition.rules.length === 0) {
      return 'Click to set condition';
    }
    
    const { combinator, rules } = data.configuration.condition;
    if (rules.length === 1) {
      const rule = rules[0];
      if ('field' in rule) {
        return `${rule.field} ${rule.operator} ${rule.value}`;
      }
    }
    
    return `${rules.length} rules (${combinator.toUpperCase()})`;
  };

  const handleSaveCondition = (query: RuleGroupType) => {
    // In a real app, this would update the node through the workflow store
    console.log('Saving condition for node:', id, query);
    setShowBuilder(false);
  };

  return (
    <>
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
        onClick={() => setShowBuilder(true)}
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
          
          <div style={{ 
            fontSize: '10px', 
            opacity: 0.8,
            padding: '4px',
            background: 'rgba(255, 255, 255, 0.2)',
            borderRadius: '4px',
            marginTop: '4px'
          }}>
            {formatConditionSummary()}
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

      <AnimatePresence>
        {showBuilder && (
          <Modal onClose={() => setShowBuilder(false)}>
            <div style={{ minWidth: '800px', maxWidth: '90vw' }}>
              <ConditionBuilder
                initialQuery={localCondition}
                onQueryChange={setLocalCondition}
                onSave={handleSaveCondition}
                onCancel={() => setShowBuilder(false)}
                telemetryFields={[
                  { name: 'telemetry.temperature', label: 'Telemetry Temperature' },
                  { name: 'telemetry.pressure', label: 'Telemetry Pressure' },
                  { name: 'telemetry.speed', label: 'Telemetry Speed' },
                  { name: 'telemetry.location.distance', label: 'Distance from Origin' },
                ]}
              />
            </div>
          </Modal>
        )}
      </AnimatePresence>
    </>
  );
};

export default memo(DecisionNodeWithBuilder);