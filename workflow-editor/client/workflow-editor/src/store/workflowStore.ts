import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { Node, Edge } from 'reactflow';

interface Workflow {
  id: string;
  name: string;
  description?: string;
  nodes: Node[];
  edges: Edge[];
  createdAt: string;
  updatedAt: string;
  status: 'draft' | 'published' | 'archived';
}

interface WorkflowExecution {
  workflowId: string;
  executionId: string;
  status: 'running' | 'completed' | 'failed';
  startedAt: string;
  completedAt?: string;
  currentNodeId?: string;
  nodeStatuses: Record<string, 'pending' | 'running' | 'completed' | 'failed'>;
}

interface WorkflowStore {
  workflows: Record<string, Workflow>;
  executions: Record<string, WorkflowExecution>;
  selectedWorkflowId: string | null;
  
  // Workflow actions
  createWorkflow: (workflow: Omit<Workflow, 'createdAt' | 'updatedAt'>) => void;
  updateWorkflow: (id: string, updates: Partial<Workflow>) => void;
  deleteWorkflow: (id: string) => void;
  selectWorkflow: (id: string | null) => void;
  
  // Execution actions
  startExecution: (workflowId: string) => string;
  updateExecutionStatus: (executionId: string, status: WorkflowExecution['status']) => void;
  updateNodeStatus: (executionId: string, nodeId: string, status: 'pending' | 'running' | 'completed' | 'failed') => void;
  
  // Telemetry actions
  telemetryData: Record<string, any[]>;
  addTelemetryData: (source: string, data: any) => void;
  clearTelemetryData: (source: string) => void;
}

export const useWorkflowStore = create<WorkflowStore>()(
  devtools(
    persist(
      (set, get) => ({
        workflows: {},
        executions: {},
        selectedWorkflowId: null,
        telemetryData: {},

        createWorkflow: (workflow) => {
          const now = new Date().toISOString();
          const newWorkflow = {
            ...workflow,
            createdAt: now,
            updatedAt: now,
          };
          
          set((state) => ({
            workflows: {
              ...state.workflows,
              [workflow.id]: newWorkflow,
            },
          }));
        },

        updateWorkflow: (id, updates) => {
          set((state) => ({
            workflows: {
              ...state.workflows,
              [id]: {
                ...state.workflows[id],
                ...updates,
                updatedAt: new Date().toISOString(),
              },
            },
          }));
        },

        deleteWorkflow: (id) => {
          set((state) => {
            const { [id]: deleted, ...rest } = state.workflows;
            return {
              workflows: rest,
              selectedWorkflowId: state.selectedWorkflowId === id ? null : state.selectedWorkflowId,
            };
          });
        },

        selectWorkflow: (id) => {
          set({ selectedWorkflowId: id });
        },

        startExecution: (workflowId) => {
          const executionId = `exec-${Date.now()}`;
          const workflow = get().workflows[workflowId];
          
          if (!workflow) {
            throw new Error(`Workflow ${workflowId} not found`);
          }
          
          const nodeStatuses: Record<string, 'pending' | 'running' | 'completed' | 'failed'> = {};
          workflow.nodes.forEach((node) => {
            nodeStatuses[node.id] = 'pending';
          });
          
          const execution: WorkflowExecution = {
            workflowId,
            executionId,
            status: 'running',
            startedAt: new Date().toISOString(),
            nodeStatuses,
          };
          
          set((state) => ({
            executions: {
              ...state.executions,
              [executionId]: execution,
            },
          }));
          
          return executionId;
        },

        updateExecutionStatus: (executionId, status) => {
          set((state) => ({
            executions: {
              ...state.executions,
              [executionId]: {
                ...state.executions[executionId],
                status,
                completedAt: status !== 'running' ? new Date().toISOString() : undefined,
              },
            },
          }));
        },

        updateNodeStatus: (executionId, nodeId, status) => {
          set((state) => ({
            executions: {
              ...state.executions,
              [executionId]: {
                ...state.executions[executionId],
                currentNodeId: status === 'running' ? nodeId : state.executions[executionId].currentNodeId,
                nodeStatuses: {
                  ...state.executions[executionId].nodeStatuses,
                  [nodeId]: status,
                },
              },
            },
          }));
        },

        addTelemetryData: (source, data) => {
          set((state) => ({
            telemetryData: {
              ...state.telemetryData,
              [source]: [...(state.telemetryData[source] || []), data].slice(-100), // Keep last 100 entries
            },
          }));
        },

        clearTelemetryData: (source) => {
          set((state) => ({
            telemetryData: {
              ...state.telemetryData,
              [source]: [],
            },
          }));
        },
      }),
      {
        name: 'workflow-store',
        partialize: (state) => ({
          workflows: state.workflows,
          selectedWorkflowId: state.selectedWorkflowId,
        }),
      }
    )
  )
);

// Selectors
export const selectCurrentWorkflow = () => {
  const { workflows, selectedWorkflowId } = useWorkflowStore.getState();
  return selectedWorkflowId ? workflows[selectedWorkflowId] : null;
};

export const selectWorkflowExecutions = (workflowId: string) => {
  const { executions } = useWorkflowStore.getState();
  return Object.values(executions).filter((exec) => exec.workflowId === workflowId);
};

export const selectLatestExecution = (workflowId: string) => {
  const executions = selectWorkflowExecutions(workflowId);
  return executions.sort((a, b) => b.startedAt.localeCompare(a.startedAt))[0];
};