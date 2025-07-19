import { create } from 'zustand';
import { devtools } from 'zustand/middleware';
import { Workflow, WorkflowNode, WorkflowEdge, WorkflowExecution } from '../types/workflow';

interface WorkflowState {
  // Current workflow being edited
  currentWorkflow: Workflow | null;
  
  // All workflows
  workflows: Workflow[];
  
  // Workflow executions
  executions: WorkflowExecution[];
  
  // UI state
  selectedNodeId: string | null;
  isExecuting: boolean;
  
  // Actions
  setCurrentWorkflow: (workflow: Workflow | null) => void;
  updateWorkflow: (workflow: Workflow) => void;
  addNode: (node: WorkflowNode) => void;
  updateNode: (nodeId: string, updates: Partial<WorkflowNode>) => void;
  removeNode: (nodeId: string) => void;
  addEdge: (edge: WorkflowEdge) => void;
  removeEdge: (edgeId: string) => void;
  setSelectedNode: (nodeId: string | null) => void;
  setExecuting: (executing: boolean) => void;
  addExecution: (execution: WorkflowExecution) => void;
  updateExecution: (executionId: string, updates: Partial<WorkflowExecution>) => void;
}

export const useWorkflowStore = create<WorkflowState>()(
  devtools(
    (set, get) => ({
      currentWorkflow: null,
      workflows: [],
      executions: [],
      selectedNodeId: null,
      isExecuting: false,

      setCurrentWorkflow: (workflow) => set({ currentWorkflow: workflow }),

      updateWorkflow: (workflow) => set((state) => ({
        currentWorkflow: workflow,
        workflows: state.workflows.map(w => w.id === workflow.id ? workflow : w)
      })),

      addNode: (node) => set((state) => {
        if (!state.currentWorkflow) return state;
        
        const updatedWorkflow = {
          ...state.currentWorkflow,
          nodes: [...state.currentWorkflow.nodes, node],
          updatedAt: new Date().toISOString()
        };
        
        return {
          currentWorkflow: updatedWorkflow,
          workflows: state.workflows.map(w => w.id === updatedWorkflow.id ? updatedWorkflow : w)
        };
      }),

      updateNode: (nodeId, updates) => set((state) => {
        if (!state.currentWorkflow) return state;
        
        const updatedWorkflow = {
          ...state.currentWorkflow,
          nodes: state.currentWorkflow.nodes.map(node => 
            node.id === nodeId ? { ...node, ...updates } : node
          ),
          updatedAt: new Date().toISOString()
        };
        
        return {
          currentWorkflow: updatedWorkflow,
          workflows: state.workflows.map(w => w.id === updatedWorkflow.id ? updatedWorkflow : w)
        };
      }),

      removeNode: (nodeId) => set((state) => {
        if (!state.currentWorkflow) return state;
        
        const updatedWorkflow = {
          ...state.currentWorkflow,
          nodes: state.currentWorkflow.nodes.filter(node => node.id !== nodeId),
          edges: state.currentWorkflow.edges.filter(edge => 
            edge.source !== nodeId && edge.target !== nodeId
          ),
          updatedAt: new Date().toISOString()
        };
        
        return {
          currentWorkflow: updatedWorkflow,
          workflows: state.workflows.map(w => w.id === updatedWorkflow.id ? updatedWorkflow : w)
        };
      }),

      addEdge: (edge) => set((state) => {
        if (!state.currentWorkflow) return state;
        
        const updatedWorkflow = {
          ...state.currentWorkflow,
          edges: [...state.currentWorkflow.edges, edge],
          updatedAt: new Date().toISOString()
        };
        
        return {
          currentWorkflow: updatedWorkflow,
          workflows: state.workflows.map(w => w.id === updatedWorkflow.id ? updatedWorkflow : w)
        };
      }),

      removeEdge: (edgeId) => set((state) => {
        if (!state.currentWorkflow) return state;
        
        const updatedWorkflow = {
          ...state.currentWorkflow,
          edges: state.currentWorkflow.edges.filter(edge => edge.id !== edgeId),
          updatedAt: new Date().toISOString()
        };
        
        return {
          currentWorkflow: updatedWorkflow,
          workflows: state.workflows.map(w => w.id === updatedWorkflow.id ? updatedWorkflow : w)
        };
      }),

      setSelectedNode: (nodeId) => set({ selectedNodeId: nodeId }),

      setExecuting: (executing) => set({ isExecuting: executing }),

      addExecution: (execution) => set((state) => ({
        executions: [...state.executions, execution]
      })),

      updateExecution: (executionId, updates) => set((state) => ({
        executions: state.executions.map(exec => 
          exec.id === executionId ? { ...exec, ...updates } : exec
        )
      }))
    }),
    {
      name: 'workflow-store'
    }
  )
);