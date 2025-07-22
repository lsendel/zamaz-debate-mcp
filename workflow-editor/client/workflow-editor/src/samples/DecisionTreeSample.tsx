import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import ReactFlow, {
  Node,
  Edge,
  useNodesState,
  useEdgesState,
  Controls,
  Background,
  BackgroundVariant,
  MiniMap,
  ReactFlowProvider
} from 'reactflow';
import 'reactflow/dist/style.css';
import { useWorkflowStore } from '../store/workflowStore';
import ConditionBuilder from '../components/ConditionBuilder';
import { RuleGroupType } from 'react-querybuilder';

interface DecisionPath {
  nodeId: string;
  decision: boolean;
  timestamp: number;
  value?: any;
}

const DecisionTreeSample: React.FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [executionPath, setExecutionPath] = useState<DecisionPath[]>([]);
  const [isExecuting, setIsExecuting] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('loan-approval');
  const [showConditionBuilder, setShowConditionBuilder] = useState(false);
// // // //   const [editingNodeId, setEditingNodeId] = useState<string | null>(null); // SonarCloud: removed useless assignment // SonarCloud: removed useless assignment // Removed: useless assignment // Removed: useless assignment
// // // //   const { createWorkflow, startExecution, updateNodeStatus } = useWorkflowStore(); // SonarCloud: removed useless assignment // SonarCloud: removed useless assignment // Removed: useless assignment // Removed: useless assignment

  // Decision tree templates
  const templates = {
    'loan-approval': {
      name: 'Loan Approval Process',
      description: 'Automated loan approval based on credit score, income, and debt ratio',
      nodes: [
        {
          id: 'start',
          type: 'start',
          position: { x: 250, y: 50 },
          data: { label: 'Loan Application' }
        },
        {
          id: 'check-credit',
          type: 'decision',
          position: { x: 250, y: 150 },
          data: {
            label: 'Check Credit Score',
            condition: {
              combinator: 'and',
              rules: [
                { field: 'creditScore', operator: '>=', value: 650 }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'check-income',
          type: 'decision',
          position: { x: 150, y: 250 },
          data: {
            label: 'Verify Income',
            condition: {
              combinator: 'and',
              rules: [
                { field: 'annualIncome', operator: '>=', value: 50000 }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'check-debt',
          type: 'decision',
          position: { x: 350, y: 250 },
          data: {
            label: 'Check Debt Ratio',
            condition: {
              combinator: 'and',
              rules: [
                { field: 'debtToIncomeRatio', operator: '<=', value: 0.4 }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'approve',
          type: 'task',
          position: { x: 250, y: 350 },
          data: { label: 'Approve Loan' }
        },
        {
          id: 'reject',
          type: 'task',
          position: { x: 450, y: 350 },
          data: { label: 'Reject Application' }
        },
        {
          id: 'manual-review',
          type: 'task',
          position: { x: 50, y: 350 },
          data: { label: 'Manual Review' }
        },
        {
          id: 'end',
          type: 'end',
          position: { x: 250, y: 450 },
          data: { label: 'Process Complete' }
        }
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'check-credit', animated: true },
        { id: 'e2', source: 'check-credit', sourceHandle: 'true', target: 'check-income', label: 'Score ≥ 650', animated: true },
        { id: 'e3', source: 'check-credit', sourceHandle: 'false', target: 'reject', label: 'Score < 650', animated: true },
        { id: 'e4', source: 'check-income', sourceHandle: 'true', target: 'check-debt', label: 'Income OK', animated: true },
        { id: 'e5', source: 'check-income', sourceHandle: 'false', target: 'manual-review', label: 'Low Income', animated: true },
        { id: 'e6', source: 'check-debt', sourceHandle: 'true', target: 'approve', label: 'Ratio OK', animated: true },
        { id: 'e7', source: 'check-debt', sourceHandle: 'false', target: 'reject', label: 'High Debt', animated: true },
        { id: 'e8', source: 'approve', target: 'end', animated: true },
        { id: 'e9', source: 'reject', target: 'end', animated: true },
        { id: 'e10', source: 'manual-review', target: 'end', animated: true }
      ]
    },
    'fraud-detection': {
      name: 'Fraud Detection System',
      description: 'Real-time transaction fraud detection based on patterns and thresholds',
      nodes: [
        {
          id: 'start',
          type: 'start',
          position: { x: 300, y: 50 },
          data: { label: 'New Transaction' }
        },
        {
          id: 'check-amount',
          type: 'decision',
          position: { x: 300, y: 150 },
          data: {
            label: 'Check Amount',
            condition: {
              combinator: 'or',
              rules: [
                { field: 'amount', operator: '>', value: 5000 },
                { field: 'dailyTotal', operator: '>', value: 10000 }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'check-location',
          type: 'decision',
          position: { x: 200, y: 250 },
          data: {
            label: 'Location Check',
            condition: {
              combinator: 'and',
              rules: [
                { field: 'location.suspicious', operator: '=', value: true }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'check-pattern',
          type: 'decision',
          position: { x: 400, y: 250 },
          data: {
            label: 'Pattern Analysis',
            condition: {
              combinator: 'or',
              rules: [
                { field: 'rapidTransactions', operator: '>', value: 5 },
                { field: 'unusualMerchant', operator: '=', value: true }
              ]
            } as RuleGroupType
          }
        },
        {
          id: 'flag-suspicious',
          type: 'task',
          position: { x: 300, y: 350 },
          data: { label: 'Flag as Suspicious' }
        },
        {
          id: 'approve-transaction',
          type: 'task',
          position: { x: 500, y: 350 },
          data: { label: 'Approve Transaction' }
        },
        {
          id: 'block-transaction',
          type: 'task',
          position: { x: 100, y: 350 },
          data: { label: 'Block Transaction' }
        },
        {
          id: 'end',
          type: 'end',
          position: { x: 300, y: 450 },
          data: { label: 'Complete' }
        }
      ],
      edges: [
        { id: 'e1', source: 'start', target: 'check-amount', animated: true },
        { id: 'e2', source: 'check-amount', sourceHandle: 'true', target: 'check-location', label: 'High Amount', animated: true },
        { id: 'e3', source: 'check-amount', sourceHandle: 'false', target: 'check-pattern', label: 'Normal Amount', animated: true },
        { id: 'e4', source: 'check-location', sourceHandle: 'true', target: 'block-transaction', label: 'Suspicious', animated: true },
        { id: 'e5', source: 'check-location', sourceHandle: 'false', target: 'flag-suspicious', label: 'OK', animated: true },
        { id: 'e6', source: 'check-pattern', sourceHandle: 'true', target: 'flag-suspicious', label: 'Anomaly', animated: true },
        { id: 'e7', source: 'check-pattern', sourceHandle: 'false', target: 'approve-transaction', label: 'Normal', animated: true },
        { id: 'e8', source: 'flag-suspicious', target: 'end', animated: true },
        { id: 'e9', source: 'approve-transaction', target: 'end', animated: true },
        { id: 'e10', source: 'block-transaction', target: 'end', animated: true }
      ]
    }
  };

  // Load selected template
  useEffect(() => {
    const template = templates[selectedTemplate as keyof typeof templates];
    if (template) {
      setNodes(template.nodes as any);
      setEdges(template.edges);
      setExecutionPath([]);
    }
  }, [selectedTemplate]);

  // Execute decision tree with sample data
  const executeDecisionTree = async () => {
    setIsExecuting(true);
    setExecutionPath([]);
    
    // Sample input data
    const sampleData = selectedTemplate === 'loan-approval' 
      ? {
          creditScore: 720,
          annualIncome: 75000,
          debtToIncomeRatio: 0.35
        }
      : {
          amount: 6000,
          dailyTotal: 8000,
          location: { suspicious: false },
          rapidTransactions: 3,
          unusualMerchant: false
        };

    // Simulate execution
    const execution = await simulateExecution(nodes, edges, sampleData);
    
    // Animate the execution path
    for (const step of execution) {
      await new Promise(resolve => setTimeout(resolve, 1000));
      setExecutionPath(prev => [...prev, step]);
      
      // Update node visual state
      setNodes(nds => nds.map(node => {
        if (node.id === step.nodeId) {
          return {
            ...node,
            style: {
              ...node.style,
              background: step.decision ? '#4CAF50' : '#f44336',
              color: 'white'
            }
          };
        }
        return node;
      }));
      
      // Update edge visual state
      setEdges(eds => eds.map(edge => {
        const pathEdge = execution.find(p => 
          edge.source === p.nodeId && 
          ((p.decision && edge.sourceHandle === 'true') || 
           (!p.decision && edge.sourceHandle === 'false'))
        );
        if (pathEdge) {
          return {
            ...edge,
            style: { stroke: '#4CAF50', strokeWidth: 3 },
            animated: true
          };
        }
        return edge;
      }));
    }
    
    setIsExecuting(false);
  };

  // Simulate decision tree execution
  const simulateExecution = async (
    nodes: Node[], 
    edges: Edge[], 
    data: any
  ): Promise<DecisionPath[]> => {
    const path: DecisionPath[] = [];
    let currentNodeId = 'start';
    
    while (currentNodeId && currentNodeId !== 'end') {
      const currentNode = nodes.find(n => n.id === currentNodeId);
      if (!currentNode) break;
      
      if (currentNode.type === 'decision') {
        const condition = currentNode.data.condition as RuleGroupType;
        const decision = evaluateCondition(condition, data);
        
        path.push({
          nodeId: currentNodeId,
          decision,
          timestamp: Date.now(),
          value: data
        });
        
        // Find next node based on decision
        const nextEdge = edges.find(e => 
          e.source === currentNodeId && 
          ((decision && e.sourceHandle === 'true') || 
           (!decision && e.sourceHandle === 'false'))
        );
        currentNodeId = nextEdge?.target || 'end';
      } else if (currentNode.type === 'task') {
        path.push({
          nodeId: currentNodeId,
          decision: true,
          timestamp: Date.now()
        });
        
        const nextEdge = edges.find(e => e.source === currentNodeId);
        currentNodeId = nextEdge?.target || 'end';
      } else {
        const nextEdge = edges.find(e => e.source === currentNodeId);
        currentNodeId = nextEdge?.target || 'end';
      }
    }
    
    return path;
  };

  // Simple condition evaluator
  const evaluateCondition = (condition: RuleGroupType, data: any): boolean => {
    const { combinator, rules } = condition;
    
    const results = rules.map((rule: any) => {
      if ('rules' in rule) {
        return evaluateCondition(rule, data);
      }
      
      const value = getNestedValue(data, rule.field);
      
      switch (rule.operator) {
        case '=': return value == rule.value;
        case '!=': return value != rule.value;
        case '<': return value < rule.value;
        case '>': return value > rule.value;
        case '<=': return value <= rule.value;
        case '>=': return value >= rule.value;
        default: return false;
      }
    });
    
    return combinator === 'and' 
      ? results.every(Boolean)
      : results.some(Boolean);
  };

  const getNestedValue = (obj: any, path: string): any => {
    return path.split('.').reduce((curr, key) => curr?.[key], obj);
  };

  // Reset execution
  const resetExecution = () => {
    setExecutionPath([]);
    const template = templates[selectedTemplate as keyof typeof templates];
    if (template) {
      setNodes(template.nodes as any);
      setEdges(template.edges);
    }
  };

  // Save as workflow
  const saveAsWorkflow = () => {
    const template = templates[selectedTemplate as keyof typeof templates];
    createWorkflow({
      id: `decision-tree-${Date.now()}`,
      name: template.name,
      description: template.description,
      status: 'published',
      nodes,
      edges
    });
  };

  return (
    <div className="decision-tree-sample">
      <div className="sample-header">
        <h2>Decision Tree Sample</h2>
        <p>Interactive decision tree visualization with real-time execution</p>
      </div>

      <div className="sample-controls">
        <select
          value={selectedTemplate}
          onChange={(e) => setSelectedTemplate(e.target.value)}
          className="template-selector"
        >
          <option value="loan-approval">Loan Approval Process</option>
          <option value="fraud-detection">Fraud Detection System</option>
        </select>
        
        <button
          className="execute-button"
          onClick={executeDecisionTree}
          disabled={isExecuting}
        >
          {isExecuting ? '⏳ Executing...' : '▶️ Execute'}
        </button>
        
        <button
          className="reset-button"
          onClick={resetExecution}
        >
          🔄 Reset
        </button>
        
        <button
          className="save-button"
          onClick={saveAsWorkflow}
        >
          💾 Save as Workflow
        </button>
        
        <button
          className="condition-button"
          onClick={() => setShowConditionBuilder(true)}
        >
          🔧 Condition Builder
        </button>
      </div>

      <div className="tree-visualization">
        <ReactFlowProvider>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            fitView
          >
            <Background variant={BackgroundVariant.Dots} gap={12} size={1} />
            <Controls />
            <MiniMap
              nodeColor={(node) => {
                const pathNode = executionPath.find(p => p.nodeId === node.id);
                if (pathNode) {
                  return pathNode.decision ? '#4CAF50' : '#f44336';
                }
                return '#ddd';
              }}
            />
          </ReactFlow>
        </ReactFlowProvider>
      </div>

      {executionPath.length > 0 && (
        <div className="execution-log">
          <h3>Execution Path</h3>
          <div className="log-entries">
            {executionPath.map((step, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.1 }}
                className={`log-entry ${step.decision ? 'success' : 'failure'}`}
              >
                <span className="step-number">#{index + 1}</span>
                <span className="node-id">{step.nodeId}</span>
                <span className="decision">
                  {step.decision ? '✓ True' : '✗ False'}
                </span>
                <span className="timestamp">
                  {new Date(step.timestamp).toLocaleTimeString()}
                </span>
              </motion.div>
            ))}
          </div>
        </div>
      )}

      <AnimatePresence>
        {showConditionBuilder && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="condition-builder-modal"
          >
            <div className="modal-content">
              <button
                className="close-button"
                onClick={() => setShowConditionBuilder(false)}
              >
                ✕
              </button>
              <h3>Decision Tree Condition Builder</h3>
              <p>Build and test conditions for decision nodes</p>
              <ConditionBuilder
                onQueryChange={(query) => console.log('Query:', query)}
                fields={[
                  { name: 'creditScore', label: 'Credit Score', inputType: 'number' },
                  { name: 'annualIncome', label: 'Annual Income', inputType: 'number' },
                  { name: 'debtToIncomeRatio', label: 'Debt to Income Ratio', inputType: 'number' },
                  { name: 'amount', label: 'Transaction Amount', inputType: 'number' },
                  { name: 'location.suspicious', label: 'Suspicious Location', inputType: 'checkbox' }
                ]}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <style>{`
        .decision-tree-sample {
          padding: 20px;
          max-width: 1400px;
          margin: 0 auto;
        }

        .sample-header {
          text-align: center;
          margin-bottom: 30px;
        }

        .sample-header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .sample-controls {
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 15px;
          margin-bottom: 20px;
          flex-wrap: wrap;
        }

        .template-selector {
          padding: 10px 15px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 14px;
        }

        .execute-button,
        .reset-button,
        .save-button,
        .condition-button {
          padding: 10px 20px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background 0.2s;
        }

        .execute-button {
          background: #4CAF50;
          color: white;
        }

        .execute-button:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .reset-button {
          background: #2196F3;
          color: white;
        }

        .save-button {
          background: #FF9800;
          color: white;
        }

        .condition-button {
          background: #9C27B0;
          color: white;
        }

        .tree-visualization {
          height: 500px;
          background: white;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          margin-bottom: 20px;
        }

        .execution-log {
          background: white;
          border-radius: 8px;
          padding: 20px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }

        .execution-log h3 {
          margin: 0 0 15px 0;
          color: #333;
        }

        .log-entries {
          display: flex;
          flex-direction: column;
          gap: 10px;
        }

        .log-entry {
          display: flex;
          align-items: center;
          gap: 15px;
          padding: 12px;
          background: #f9f9f9;
          border-radius: 4px;
          border-left: 4px solid;
        }

        .log-entry.success {
          border-left-color: #4CAF50;
        }

        .log-entry.failure {
          border-left-color: #f44336;
        }

        .step-number {
          font-weight: bold;
          color: #666;
        }

        .node-id {
          flex: 1;
          font-family: monospace;
          color: #333;
        }

        .decision {
          font-weight: 500;
        }

        .log-entry.success .decision {
          color: #4CAF50;
        }

        .log-entry.failure .decision {
          color: #f44336;
        }

        .timestamp {
          font-size: 12px;
          color: #999;
        }

        .condition-builder-modal {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .modal-content {
          background: white;
          border-radius: 8px;
          padding: 30px;
          max-width: 800px;
          width: 90%;
          max-height: 80vh;
          overflow-y: auto;
          position: relative;
        }

        .close-button {
          position: absolute;
          top: 15px;
          right: 15px;
          background: none;
          border: none;
          font-size: 20px;
          cursor: pointer;
          color: #999;
        }

        .modal-content h3 {
          margin: 0 0 10px 0;
          color: #333;
        }

        .modal-content p {
          color: #666;
          margin-bottom: 20px;
        }
      `}</style>
    </div>
  );
};

export default DecisionTreeSample;