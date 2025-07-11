export interface Implementation {
  id: string;
  debateId: string;
  organizationId: string;
  commitHash?: string;
  branch?: string;
  repository?: string;
  pullRequestUrl?: string;
  implementedAt: string;
  implementedBy: string;
  status: 'planned' | 'in_progress' | 'completed' | 'deployed';
  description: string;
  files: ImplementationFile[];
  metadata?: Record<string, any>;
}

export interface ImplementationFile {
  path: string;
  changes: string;
  additions: number;
  deletions: number;
  language?: string;
}

export interface ImplementationTracking {
  debateId: string;
  implementations: Implementation[];
  lastUpdated: string;
  totalCommits: number;
  totalFiles: number;
  totalAdditions: number;
  totalDeletions: number;
}