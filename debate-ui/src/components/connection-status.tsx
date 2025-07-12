'use client';

import { useDebateWebSocket } from '@/hooks/use-websocket';
import { cn } from '@/lib/utils';
import { Wifi, WifiOff, RefreshCw } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

export function ConnectionStatus() {
  const { isConnected, isReconnecting } = useDebateWebSocket();

  const getStatusColor = () => {
    if (isConnected) return 'text-green-600 dark:text-green-400';
    if (isReconnecting) return 'text-amber-600 dark:text-amber-400';
    return 'text-red-600 dark:text-red-400';
  };

  const getStatusIcon = () => {
    if (isConnected) return <Wifi className="h-4 w-4" />;
    if (isReconnecting) return <RefreshCw className="h-4 w-4 animate-spin" />;
    return <WifiOff className="h-4 w-4" />;
  };

  const getStatusText = () => {
    if (isConnected) return 'Connected';
    if (isReconnecting) return 'Reconnecting...';
    return 'Disconnected';
  };

  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-gray-100 dark:bg-gray-800 transition-colors">
            <div className="relative">
              <div className={cn(
                "absolute inset-0 rounded-full blur-sm",
                isConnected && "bg-green-500/20",
                isReconnecting && "bg-amber-500/20 animate-pulse",
                !isConnected && !isReconnecting && "bg-red-500/20"
              )} />
              <div className={cn("relative", getStatusColor())}>
                {getStatusIcon()}
              </div>
            </div>
            <span className={cn("text-xs font-medium", getStatusColor())}>
              {getStatusText()}
            </span>
          </div>
        </TooltipTrigger>
        <TooltipContent>
          <p className="text-sm">
            {isConnected && 'Real-time updates active'}
            {isReconnecting && 'Attempting to restore connection'}
            {!isConnected && !isReconnecting && 'Real-time updates unavailable'}
          </p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}