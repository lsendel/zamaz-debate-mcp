'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Implementation, ImplementationTracking } from '@/types/implementation';
import { useOrganization } from '@/hooks/use-organization';
import { 
  GitCommit, 
  GitBranch, 
  FileCode, 
  Calendar, 
  User, 
  Plus,
  ExternalLink,
  CheckCircle,
  Circle,
  AlertCircle,
  Loader2
} from 'lucide-react';

interface ImplementationTrackerProps {
  debateId: string;
  onImplementationAdded?: (implementation: Implementation) => void;
}

export function ImplementationTracker({ debateId, onImplementationAdded }: ImplementationTrackerProps) {
  const { currentOrg, addHistoryEntry } = useOrganization();
  const [tracking, setTracking] = useState<ImplementationTracking | null>(null);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  
  // Form state
  const [commitHash, setCommitHash] = useState('');
  const [branch, setBranch] = useState('main');
  const [repository, setRepository] = useState('');
  const [pullRequestUrl, setPullRequestUrl] = useState('');
  const [description, setDescription] = useState('');
  const [status, setStatus] = useState<Implementation['status']>('planned');

  useEffect(() => {
    loadImplementations();
  }, [debateId, currentOrg]);

  const loadImplementations = async () => {
    if (!currentOrg) return;
    
    try {
      setIsLoading(true);
      // Load from localStorage - in production this would be an API call
      const key = `implementations_${currentOrg.id}_${debateId}`;
      const saved = localStorage.getItem(key);
      
      if (saved) {
        setTracking(JSON.parse(saved));
      } else {
        setTracking({
          debateId,
          implementations: [],
          lastUpdated: new Date().toISOString(),
          totalCommits: 0,
          totalFiles: 0,
          totalAdditions: 0,
          totalDeletions: 0
        });
      }
    } catch (error) {
      console.error('Failed to load implementations:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const addImplementation = async () => {
    if (!currentOrg || !tracking) return;

    const newImpl: Implementation = {
      id: `impl-${Date.now()}`,
      debateId,
      organizationId: currentOrg.id,
      commitHash,
      branch,
      repository,
      pullRequestUrl,
      implementedAt: new Date().toISOString(),
      implementedBy: 'current-user', // In production, get from auth
      status,
      description,
      files: [], // Would be populated from git integration
      metadata: {
        source: 'manual_entry'
      }
    };

    const updatedTracking: ImplementationTracking = {
      ...tracking,
      implementations: [...tracking.implementations, newImpl],
      lastUpdated: new Date().toISOString(),
      totalCommits: tracking.totalCommits + 1
    };

    // Save to localStorage
    const key = `implementations_${currentOrg.id}_${debateId}`;
    localStorage.setItem(key, JSON.stringify(updatedTracking));
    setTracking(updatedTracking);

    // Add to organization history
    addHistoryEntry({
      organizationId: currentOrg.id,
      action: 'implementation_added',
      description: `Added implementation: ${description}`,
      metadata: {
        debateId,
        commitHash,
        status
      }
    });

    // Notify parent
    onImplementationAdded?.(newImpl);

    // Reset form
    setIsAddOpen(false);
    setCommitHash('');
    setBranch('main');
    setRepository('');
    setPullRequestUrl('');
    setDescription('');
    setStatus('planned');
  };

  const getStatusIcon = (status: Implementation['status']) => {
    switch (status) {
      case 'completed':
      case 'deployed':
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'in_progress':
        return <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />;
      case 'planned':
        return <Circle className="h-4 w-4 text-gray-400" />;
      default:
        return <AlertCircle className="h-4 w-4 text-yellow-500" />;
    }
  };

  const getStatusColor = (status: Implementation['status']) => {
    switch (status) {
      case 'completed':
        return 'bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300';
      case 'deployed':
        return 'bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300';
      case 'in_progress':
        return 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300';
      case 'planned':
      default:
        return 'bg-gray-100 text-gray-700 dark:bg-gray-900 dark:text-gray-300';
    }
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg flex items-center gap-2">
                <GitCommit className="h-5 w-5" />
                Implementation Tracking
              </CardTitle>
              <CardDescription>
                Track code implementations from this debate
              </CardDescription>
            </div>
            <Button onClick={() => setIsAddOpen(true)} size="sm">
              <Plus className="h-4 w-4 mr-1" />
              Add Implementation
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {!tracking || tracking.implementations.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <GitCommit className="h-12 w-12 mx-auto mb-4 opacity-20" />
              <p>No implementations tracked yet</p>
              <p className="text-sm mt-1">
                Add implementations to track code changes from this debate
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Summary Stats */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <div className="text-center">
                  <div className="text-2xl font-bold">{tracking.totalCommits}</div>
                  <div className="text-xs text-muted-foreground">Commits</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-green-600">
                    {tracking.implementations.filter(i => i.status === 'completed').length}
                  </div>
                  <div className="text-xs text-muted-foreground">Completed</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-blue-600">
                    {tracking.implementations.filter(i => i.status === 'deployed').length}
                  </div>
                  <div className="text-xs text-muted-foreground">Deployed</div>
                </div>
                <div className="text-center">
                  <div className="text-2xl font-bold text-yellow-600">
                    {tracking.implementations.filter(i => i.status === 'in_progress').length}
                  </div>
                  <div className="text-xs text-muted-foreground">In Progress</div>
                </div>
              </div>

              {/* Implementation List */}
              <div className="space-y-3">
                {tracking.implementations.map((impl) => (
                  <div 
                    key={impl.id}
                    className="p-4 rounded-lg border bg-card hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          {getStatusIcon(impl.status)}
                          <h4 className="font-medium">{impl.description}</h4>
                          <Badge className={getStatusColor(impl.status)}>
                            {impl.status}
                          </Badge>
                        </div>
                        
                        <div className="flex items-center gap-4 text-sm text-muted-foreground">
                          {impl.commitHash && (
                            <div className="flex items-center gap-1">
                              <GitCommit className="h-3 w-3" />
                              <code className="font-mono">{impl.commitHash.slice(0, 7)}</code>
                            </div>
                          )}
                          
                          {impl.branch && (
                            <div className="flex items-center gap-1">
                              <GitBranch className="h-3 w-3" />
                              <span>{impl.branch}</span>
                            </div>
                          )}
                          
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3 w-3" />
                            <span>{formatDate(impl.implementedAt)}</span>
                          </div>
                          
                          <div className="flex items-center gap-1">
                            <User className="h-3 w-3" />
                            <span>{impl.implementedBy}</span>
                          </div>
                        </div>
                      </div>
                      
                      {impl.pullRequestUrl && (
                        <Button
                          variant="ghost"
                          size="sm"
                          asChild
                        >
                          <a href={impl.pullRequestUrl} target="_blank" rel="noopener noreferrer">
                            <ExternalLink className="h-4 w-4" />
                          </a>
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={isAddOpen} onOpenChange={setIsAddOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Track Implementation</DialogTitle>
            <DialogDescription>
              Add details about code implemented from this debate
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div>
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What was implemented from this debate?"
                className="mt-1"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="commit">Commit Hash</Label>
                <Input
                  id="commit"
                  value={commitHash}
                  onChange={(e) => setCommitHash(e.target.value)}
                  placeholder="abc123def"
                  className="mt-1 font-mono"
                />
              </div>

              <div>
                <Label htmlFor="branch">Branch</Label>
                <Input
                  id="branch"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  placeholder="main"
                  className="mt-1"
                />
              </div>
            </div>

            <div>
              <Label htmlFor="repository">Repository</Label>
              <Input
                id="repository"
                value={repository}
                onChange={(e) => setRepository(e.target.value)}
                placeholder="owner/repo"
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="pr-url">Pull Request URL</Label>
              <Input
                id="pr-url"
                value={pullRequestUrl}
                onChange={(e) => setPullRequestUrl(e.target.value)}
                placeholder="https://github.com/owner/repo/pull/123"
                className="mt-1"
              />
            </div>

            <div>
              <Label>Status</Label>
              <div className="grid grid-cols-2 gap-2 mt-1">
                {(['planned', 'in_progress', 'completed', 'deployed'] as const).map((s) => (
                  <Button
                    key={s}
                    variant={status === s ? 'default' : 'outline'}
                    size="sm"
                    onClick={() => setStatus(s)}
                    className="justify-start"
                  >
                    {getStatusIcon(s)}
                    <span className="ml-2 capitalize">{s.replace('_', ' ')}</span>
                  </Button>
                ))}
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button onClick={addImplementation} disabled={!description.trim()}>
              <GitCommit className="h-4 w-4 mr-2" />
              Track Implementation
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}