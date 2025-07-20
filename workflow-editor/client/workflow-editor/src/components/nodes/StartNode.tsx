import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';
import { Play, Clock, Mail, User } from 'lucide-react';

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
      className={`
        bg-green-500 text-white px-5 py-2.5 rounded-full min-w-[120px] text-center
        shadow-md cursor-pointer transition-all
        ${selected ? 'ring-2 ring-green-700 ring-offset-2' : ''}
      `}
    >
      <div className="font-bold text-sm flex items-center justify-center gap-2">
        <Play className="h-4 w-4" />
        {data.label || 'Start'}
      </div>
      {data.configuration?.trigger && (
        <div className="text-xs mt-1 opacity-90 flex items-center justify-center gap-1">
          {data.configuration.trigger === 'scheduled' && (
            <>
              <Clock className="h-3 w-3" />
              {data.configuration.schedule}
            </>
          )}
          {data.configuration.trigger === 'event' && (
            <>
              <Mail className="h-3 w-3" />
              {data.configuration.eventType}
            </>
          )}
          {data.configuration.trigger === 'manual' && (
            <>
              <User className="h-3 w-3" />
              Manual
            </>
          )}
        </div>
      )}
      <Handle
        type="source"
        position={Position.Right}
        className="w-3 h-3 bg-green-700 border-2 border-white"
      />
    </motion.div>
  );
};

export default memo(StartNode);