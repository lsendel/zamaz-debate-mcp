import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';
import { HelpCircle, BarChart } from 'lucide-react';

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
  const getEvaluationBorder = () => {
    if (data.evaluationResult === true) {
      return 'border-green-500 border-4';
    } else if (data.evaluationResult === false) {
      return 'border-red-500 border-4';
    }
    return 'border-transparent border-2';
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
      className={`
        bg-orange-500 text-white p-4 rounded-lg min-w-[200px] shadow-md cursor-pointer relative
        transform rotate-45 ${getEvaluationBorder()}
        ${selected ? 'ring-2 ring-orange-700 ring-offset-2' : ''}
      `}
    >
      <div className="transform -rotate-45 text-center">
        <div className="font-bold text-sm mb-2 flex items-center justify-center gap-2">
          <HelpCircle className="h-4 w-4" />
          {data.label || 'Decision'}
        </div>
        
        {data.configuration?.decisionType && (
          <div className="text-xs opacity-90 mb-1">
            Type: {data.configuration.decisionType}
          </div>
        )}
        
        <div className="text-xs opacity-80">
          {formatCondition()}
        </div>
        
        {data.configuration?.telemetrySource && (
          <div className="text-xs opacity-80 mt-1 flex items-center justify-center gap-1">
            <BarChart className="h-3 w-3" />
            {data.configuration.telemetrySource}
          </div>
        )}
      </div>
      
      <Handle
        type="target"
        position={Position.Left}
        className="w-3 h-3 bg-orange-700 border-2 border-white left-1/2 -top-1.5 transform -translate-x-1/2 -rotate-45"
      />
      
      <Handle
        type="source"
        position={Position.Right}
        id="true"
        className="w-3 h-3 bg-green-600 border-2 border-white -right-1.5 top-1/2 transform -translate-y-1/2 -rotate-45"
      />
      
      <Handle
        type="source"
        position={Position.Bottom}
        id="false"
        className="w-3 h-3 bg-red-600 border-2 border-white -bottom-1.5 left-1/2 transform -translate-x-1/2 -rotate-45"
      />
      
      {data.evaluationResult === true && (
        <motion.div
          className="absolute -top-5 -right-5 bg-green-600 text-white rounded px-1.5 py-0.5 text-xs transform -rotate-45"
        >
          TRUE
        </motion.div>
      )}
      
      {data.evaluationResult === false && (
        <motion.div
          className="absolute -bottom-5 -left-5 bg-red-600 text-white rounded px-1.5 py-0.5 text-xs transform -rotate-45"
        >
          FALSE
        </motion.div>
      )}
    </motion.div>
  );
};

export default memo(DecisionNode);