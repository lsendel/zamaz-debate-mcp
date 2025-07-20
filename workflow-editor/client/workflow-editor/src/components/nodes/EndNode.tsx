import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { motion } from 'framer-motion';
import { Check, X, Ban, Square, Bell } from 'lucide-react';

export interface EndNodeData {
  label: string;
  configuration?: {
    status?: 'success' | 'failure' | 'cancelled';
    notification?: boolean;
    notificationChannels?: string[];
  };
}

const EndNode: React.FC<NodeProps<EndNodeData>> = ({ data, selected }) => {
  const getStatusStyles = () => {
    switch (data.configuration?.status) {
      case 'success': return 'bg-green-500';
      case 'failure': return 'bg-red-500';
      case 'cancelled': return 'bg-orange-500';
      default: return 'bg-gray-500';
    }
  };

  const getStatusIcon = () => {
    const iconProps = { className: "h-4 w-4" };
    switch (data.configuration?.status) {
      case 'success': return <Check {...iconProps} />;
      case 'failure': return <X {...iconProps} />;
      case 'cancelled': return <Ban {...iconProps} />;
      default: return <Square {...iconProps} />;
    }
  };

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      whileHover={{ scale: 1.05 }}
      className={`
        ${getStatusStyles()} text-white px-5 py-2.5 rounded-full min-w-[120px] text-center
        shadow-md cursor-pointer transition-all
        ${selected ? 'ring-2 ring-gray-700 ring-offset-2' : ''}
      `}
    >
      <div className="font-bold text-sm flex items-center justify-center gap-2">
        {getStatusIcon()}
        {data.label || 'End'}
      </div>
      {data.configuration?.notification && (
        <div className="text-xs mt-1 opacity-90 flex items-center justify-center gap-1">
          <Bell className="h-3 w-3" />
          Notify: {data.configuration.notificationChannels?.join(', ') || 'Default'}
        </div>
      )}
      <Handle
        type="target"
        position={Position.Left}
        className="w-3 h-3 bg-gray-700 border-2 border-white"
      />
    </motion.div>
  );
};

export default memo(EndNode);