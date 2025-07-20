import React from 'react';
import { motion } from 'framer-motion';
import { Card, CardContent } from '@zamaz/ui';
import { Play, Zap, HelpCircle, Square } from 'lucide-react';

interface NodeType {
  type: string;
  label: string;
  icon: React.ReactNode;
  color: string;
  description: string;
}

const nodeTypes: NodeType[] = [
  {
    type: 'start',
    label: 'Start',
    icon: <Play className="h-5 w-5" />,
    color: 'text-green-500 border-green-500',
    description: 'Begin workflow execution'
  },
  {
    type: 'task',
    label: 'Task',
    icon: <Zap className="h-5 w-5" />,
    color: 'text-blue-500 border-blue-500',
    description: 'Execute an action or process'
  },
  {
    type: 'decision',
    label: 'Decision',
    icon: <HelpCircle className="h-5 w-5" />,
    color: 'text-orange-500 border-orange-500',
    description: 'Conditional branching'
  },
  {
    type: 'end',
    label: 'End',
    icon: <Square className="h-5 w-5" />,
    color: 'text-gray-500 border-gray-500',
    description: 'Terminate workflow'
  }
];

const NodePalette: React.FC = () => {
  const onDragStart = (event: React.DragEvent, nodeType: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div className="w-64 bg-gray-50 border-r border-gray-200 p-5 overflow-y-auto">
      <h3 className="text-lg font-semibold text-gray-900 mb-5">
        Node Palette
      </h3>
      
      <div className="space-y-3">
        {nodeTypes.map((nodeType) => (
          <motion.div
            key={nodeType.type}
            draggable
            onDragStart={(event) => onDragStart(event as any, nodeType.type)}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="cursor-move"
          >
            <Card className={`bg-white border-2 ${nodeType.color} hover:shadow-md transition-shadow`}>
              <CardContent className="p-3">
                <div className="flex items-center mb-2">
                  <span className={nodeType.color}>{nodeType.icon}</span>
                  <span className={`ml-2 font-semibold ${nodeType.color}`}>
                    {nodeType.label}
                  </span>
                </div>
                <p className="text-xs text-gray-600">
                  {nodeType.description}
                </p>
              </CardContent>
            </Card>
          </motion.div>
        ))}
      </div>
      
      <Card className="mt-6 bg-green-50 border-green-200">
        <CardContent className="p-4">
          <h4 className="text-sm font-semibold text-green-800 mb-2">
            Quick Tips
          </h4>
          <ul className="text-xs text-gray-700 space-y-1">
            <li>• Drag nodes to canvas</li>
            <li>• Connect nodes by dragging handles</li>
            <li>• Click nodes to edit properties</li>
            <li>• Delete with Del key</li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
};

export default NodePalette;