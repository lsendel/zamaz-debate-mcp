import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';
import { Zap, Clock, RotateCw, Settings } from 'lucide-react';

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
  const getStatusStyles = () => {
    switch (data.status) {
      case 'running':
        return 'bg-blue-500 animate-pulse';
      case 'completed':
        return 'bg-green-500';
      case 'failed':
        return 'bg-red-500';
      default:
        return 'bg-blue-500';
    }
  };

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.02 }}
      className={`
        ${getStatusStyles()} text-white p-4 rounded-lg min-w-[180px] shadow-md cursor-pointer relative
        ${selected ? 'ring-2 ring-blue-700 ring-offset-2' : ''}
      `}
    >
      <Handle
        type="target"
        position={Position.Left}
        className="w-3 h-3 bg-blue-700 border-2 border-white"
      />
      
      <div className="font-bold text-sm mb-2 flex items-center gap-2">
        <Zap className="h-4 w-4" />
        {data.label || 'Task'}
      </div>
      
      {data.configuration?.taskType && (
        <div className="text-xs opacity-90 mb-1">
          Type: {data.configuration.taskType}
        </div>
      )}
      
      <div className="text-xs opacity-80 space-y-0.5">
        {data.configuration?.timeout && (
          <div className="flex items-center gap-1">
            <Clock className="h-3 w-3" />
            Timeout: {data.configuration.timeout}s
          </div>
        )}
        {data.configuration?.retryCount && (
          <div className="flex items-center gap-1">
            <RotateCw className="h-3 w-3" />
            Retries: {data.configuration.retryCount}
          </div>
        )}
        {data.configuration?.parallel && (
          <div className="flex items-center gap-1">
            <Zap className="h-3 w-3" />
            Parallel execution
          </div>
        )}
      </div>
      
      {data.status === 'running' && (
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
          className="absolute top-2 right-2"
        >
          <Settings className="h-4 w-4" />
        </motion.div>
      )}
      
      <Handle
        type="source"
        position={Position.Right}
        className="w-3 h-3 bg-blue-700 border-2 border-white"
      />
    </motion.div>
  );
};

export default memo(TaskNode);