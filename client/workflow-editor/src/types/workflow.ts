export interface WorkflowNode {
  id: string;
  type: 'start' | 'decision' | 'task' | 'end';
  position: { x: number; y: number };
  data: {
    label: string;
    config?: Record<string, any>;
    conditions?: Condition[];
  };
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  type?: string;
  data?: {
    label?: string;
  };
}

export interface Workflow {
  id: string;
  name: string;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  organizationId: string;
  status: 'draft' | 'active' | 'paused' | 'completed';
  createdAt: string;
  updatedAt: string;
}

export interface Condition {
  id: string;
  field: string;
  operator: 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte' | 'contains' | 'in';
  value: any;
  logicalOperator?: 'and' | 'or';
}

export interface TelemetryData {
  id: string;
  deviceId: string;
  timestamp: string;
  metrics: Record<string, number | string | boolean>;
  location?: {
    lat: number;
    lng: number;
  };
}

export interface WorkflowExecution {
  id: string;
  workflowId: string;
  status: 'running' | 'completed' | 'failed' | 'paused';
  currentNodeId?: string;
  startedAt: string;
  completedAt?: string;
  telemetryData?: TelemetryData;
}