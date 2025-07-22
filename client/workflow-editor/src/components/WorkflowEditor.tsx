import React from 'react';
import { useWorkflowStore } from '../store/workflowStore';
import StartNode from './nodes/StartNode';
import DecisionNode from './nodes/DecisionNode';
import TaskNode from './nodes/TaskNode';
import EndNode from './nodes/EndNode';

const nodeTypes = {
  start: StartNode,
  decision: DecisionNode,
  task: TaskNode,
  end: EndNode,
};

const WorkflowEditor: React.FC = () => {
  const { nodes, edges, onNodesChange, onEdgesChange } = useWorkflowStore();

  return (
    <div style={{ height: '600px', width: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        fitView
      />
    </div>
  );
};

export default WorkflowEditor;