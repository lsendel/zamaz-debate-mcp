'use client';

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Clock, FileText, Users, GitCommit, Activity } from 'lucide-react';

interface HistoryEntry {
  id: string;
  organizationId: string;
  action: string;
  description: string;
  timestamp: string;
  userId?: string;
  metadata?: Record<string, any>;
}

interface OrganizationHistoryProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  organizationId: string;
  organizationName: string;
}

export function OrganizationHistory({ open, onOpenChange, organizationId, organizationName }: OrganizationHistoryProps) {
  const history = getHistoryForOrganization(organizationId);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>Organization History</DialogTitle>
          <DialogDescription>
            Activity history for {organizationName}
          </DialogDescription>
        </DialogHeader>
        
        <ScrollArea className="h-[500px] pr-4">
          {history.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <Clock className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>No history available for this organization</p>
            </div>
          ) : (
            <div className="space-y-4">
              {history.map((entry) => (
                <div key={entry.id} className="border rounded-lg p-4 hover:bg-muted/50 transition-colors">
                  <div className="flex items-start justify-between">
                    <div className="flex items-start gap-3">
                      {getActionIcon(entry.action)}
                      <div>
                        <p className="font-medium">{entry.description}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatTimestamp(entry.timestamp)}
                        </p>
                      </div>
                    </div>
                    <Badge variant="outline">{formatAction(entry.action)}</Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </ScrollArea>
      </DialogContent>
    </Dialog>
  );
}

function getHistoryForOrganization(organizationId: string): HistoryEntry[] {
  const allHistory = localStorage.getItem('organizationHistory');
  if (!allHistory) return [];
  
  const history = JSON.parse(allHistory) as HistoryEntry[];
  return history
    .filter(entry => entry.organizationId === organizationId)
    .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
}

function getActionIcon(action: string) {
  switch (action) {
    case 'organization_created':
      return <Users className="h-5 w-5 text-blue-500" />;
    case 'organization_switched':
      return <GitCommit className="h-5 w-5 text-green-500" />;
    case 'debate_created':
      return <FileText className="h-5 w-5 text-purple-500" />;
    case 'debate_updated':
      return <Activity className="h-5 w-5 text-amber-500" />;
    default:
      return <Clock className="h-5 w-5 text-gray-500" />;
  }
}

function formatAction(action: string): string {
  return action
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
  if (hours < 24) return `${hours} hour${hours > 1 ? 's' : ''} ago`;
  if (days < 7) return `${days} day${days > 1 ? 's' : ''} ago`;
  
  return date.toLocaleDateString();
}