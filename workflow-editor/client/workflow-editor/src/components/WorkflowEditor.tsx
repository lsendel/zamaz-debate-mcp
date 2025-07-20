import React, { useCallback, useMemo, useState } from 'react';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  addEdge,
  Connection,
  Edge,
  Node,
  useNodesState,
  useEdgesState,
  ReactFlowProvider,
  Panel,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { motion } from 'framer-motion';

import StartNode from './nodes/StartNode';
import EndNode from './nodes/EndNode';
import TaskNode from './nodes/TaskNode';
import DecisionNodeWithBuilder from './nodes/DecisionNodeWithBuilder';
import NodePalette from './NodePalette';
import NodePropertiesPanel from './NodePropertiesPanel';
import { useWorkflowStore } from '../store/workflowStore';

const nodeTypes = {
  start: StartNode,
  end: EndNode,
  task: TaskNode,
  decision: DecisionNodeWithBuilder,
};

interface WorkflowEditorProps {
  workflowId?: string;
  onSave?: (nodes: Node[], edges: Edge[]) => void;
}

const WorkflowEditor: React.FC<WorkflowEditorProps> = ({ workflowId, onSave }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  
  const { workflows, updateWorkflow } = useWorkflowStore();
  
  // Load workflow if ID provided
  React.useEffect(() => {
    if (workflowId && workflows[workflowId]) {
      const workflow = workflows[workflowId];
      setNodes(workflow.nodes || []);
      setEdges(workflow.edges || []);
    }
  }, [workflowId, workflows, setNodes, setEdges]);

  const onConnect = useCallback(
    (params: Connection) => {
      // Validate connection
      const sourceNode = nodes.find(n => n.id === params.source);
      const targetNode = nodes.find(n => n.id === params.target);
      
      if (!sourceNode || !targetNode) return;
      
      // Prevent invalid connections
      if (targetNode.type === 'start' || sourceNode.type === 'end') {
        return;
      }
      
      setEdges((eds) => addEdge({
        ...params,
        animated: true,
        style: { stroke: '#4CAF50', strokeWidth: 2 }
      }, eds));
    },
    [nodes, setEdges]
  );

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const type = event.dataTransfer.getData('application/reactflow');
      if (!type) return;

      const reactFlowBounds = event.currentTarget.getBoundingClientRect();
      const position = {
        x: event.clientX - reactFlowBounds.left,
        y: event.clientY - reactFlowBounds.top,
      };

      const newNode: Node = {
        id: `${type}-${Date.now()}`,
        type,
        position,
        data: { 
          label: `${type.charAt(0).toUpperCase() + type.slice(1)} Node`,
          configuration: {}
        },
      };

      setNodes((nds) => nds.concat(newNode));
    },
    [setNodes]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const updateNodeData = useCallback((nodeId: string, data: any) => {
    setNodes((nds) =>
      nds.map((node) => {
        if (node.id === nodeId) {
          return {
            ...node,
            data: {
              ...node.data,
              ...data,
            },
          };
        }
        return node;
      })
    );
  }, [setNodes]);

  const handleSave = useCallback(() => {
    if (onSave) {
      onSave(nodes, edges);
    }
    if (workflowId) {
      updateWorkflow(workflowId, { nodes, edges });
    }
  }, [nodes, edges, onSave, workflowId, updateWorkflow]);

  const isValidWorkflow = useMemo(() => {
    const startNodes = nodes.filter(n => n.type === 'start');
    const endNodes = nodes.filter(n => n.type === 'end');
    return startNodes.length === 1 && endNodes.length >= 1;
  }, [nodes]);

  return (
    <div className="workflow-editor-container" style={{ height: '100vh', display: 'flex' }}>
      <NodePalette />
      
      <div style={{ flex: 1, position: 'relative' }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeClick={onNodeClick}
          onDrop={onDrop}
          onDragOver={onDragOver}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background variant="dots" gap={12} size={1} />
          <Controls />
          <MiniMap
            nodeStrokeColor={(n) => {
              if (n.type === 'start') return '#4CAF50';
              if (n.type === 'end') return '#f44336';
              if (n.type === 'decision') return '#FF9800';
              return '#2196F3';
            }}
            nodeColor={(n) => {
              if (n.type === 'start') return '#81C784';
              if (n.type === 'end') return '#ef5350';
              if (n.type === 'decision') return '#FFB74D';
              return '#64B5F6';
            }}
            style={{
              backgroundColor: 'rgba(0, 0, 0, 0.1)',
            }}
          />
          
          <Panel position="top-right">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleSave}
              disabled={!isValidWorkflow}
              style={{
                padding: '10px 20px',
                backgroundColor: isValidWorkflow ? '#4CAF50' : '#ccc',
                color: 'white',
                border: 'none',
                borderRadius: '5px',
                cursor: isValidWorkflow ? 'pointer' : 'not-allowed',
                fontSize: '16px',
              }}
            >
              Save Workflow
            </motion.button>
          </Panel>
        </ReactFlow>
      </div>
      
      {selectedNode && (
        <NodePropertiesPanel
          node={selectedNode}
          onUpdate={(data) => updateNodeData(selectedNode.id, data)}
          onClose={() => setSelectedNode(null)}
        />
      )}
    </div>
  );
};

const WorkflowEditorWrapper: React.FC<WorkflowEditorProps> = (props) => {
  return (
    <ReactFlowProvider>
      <WorkflowEditor {...props} />
    </ReactFlowProvider>
  );
};

export default WorkflowEditorWrapper;