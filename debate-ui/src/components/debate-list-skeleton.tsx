'use client';

import { Card } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

export function DebateListSkeleton() {
  return (
    <div className="space-y-4">
      {[...Array(3)].map((_, i) => (
        <Card key={i} className="p-6">
          <div className="space-y-3">
            {/* Title skeleton */}
            <Skeleton className="h-6 w-3/4" />
            
            {/* Topic skeleton */}
            <Skeleton className="h-4 w-1/2" />
            
            {/* Participants and status */}
            <div className="flex items-center justify-between mt-4">
              <div className="flex gap-2">
                {/* Participant avatars */}
                <Skeleton className="h-8 w-8 rounded-full" />
                <Skeleton className="h-8 w-8 rounded-full" />
                <Skeleton className="h-8 w-8 rounded-full" />
              </div>
              
              {/* Status badge */}
              <Skeleton className="h-6 w-20 rounded-full" />
            </div>
            
            {/* Bottom info */}
            <div className="flex items-center justify-between text-sm text-muted-foreground pt-2">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-32" />
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
}

export function DebateDetailSkeleton() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="space-y-2">
        <Skeleton className="h-8 w-2/3" />
        <Skeleton className="h-5 w-1/2" />
      </div>
      
      {/* Participants */}
      <div className="flex gap-4">
        {[...Array(3)].map((_, i) => (
          <Card key={i} className="p-4 flex-1">
            <div className="flex items-center gap-3">
              <Skeleton className="h-10 w-10 rounded-full" />
              <div className="space-y-2 flex-1">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-3 w-32" />
              </div>
            </div>
          </Card>
        ))}
      </div>
      
      {/* Turns */}
      <div className="space-y-4">
        {[...Array(2)].map((_, i) => (
          <Card key={i} className="p-6">
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <Skeleton className="h-8 w-8 rounded-full" />
                <div className="space-y-1">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-3 w-24" />
                </div>
              </div>
              <div className="space-y-2 mt-4">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}