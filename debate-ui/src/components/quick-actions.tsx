'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus, History, Zap, Users, TrendingUp } from 'lucide-react';

interface QuickActionsProps {
  onNewDebate: () => void;
  onViewHistory: () => void;
  onManageOrganizations: () => void;
  debateCount: number;
  activeDebates: number;
}

export function QuickActions({ 
  onNewDebate, 
  onViewHistory, 
  onManageOrganizations,
  debateCount,
  activeDebates 
}: QuickActionsProps) {
  const actions = [
    {
      title: 'Start New Debate',
      description: 'Launch a new AI-powered debate',
      icon: <Plus className="h-6 w-6" />,
      color: 'from-blue-500 to-purple-500',
      onClick: onNewDebate,
      primary: true
    },
    {
      title: 'Continue Active',
      description: `${activeDebates} debate${activeDebates !== 1 ? 's' : ''} in progress`,
      icon: <TrendingUp className="h-6 w-6" />,
      color: 'from-green-500 to-emerald-500',
      onClick: () => {},
      disabled: activeDebates === 0
    },
    {
      title: 'View History',
      description: `Review ${debateCount} past debates`,
      icon: <History className="h-6 w-6" />,
      color: 'from-amber-500 to-orange-500',
      onClick: onViewHistory
    },
    {
      title: 'Manage Teams',
      description: 'Organizations & settings',
      icon: <Users className="h-6 w-6" />,
      color: 'from-purple-500 to-pink-500',
      onClick: onManageOrganizations
    }
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
      {actions.map((action, index) => (
        <Card 
          key={index}
          className={`border-0 shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 ${
            action.disabled ? 'opacity-60' : ''
          }`}
          onClick={!action.disabled ? action.onClick : undefined}
        >
          <CardHeader className="pb-3">
            <div className={`inline-flex items-center justify-center w-12 h-12 rounded-xl bg-gradient-to-br ${action.color} text-white mb-3`}>
              {action.icon}
            </div>
            <CardTitle className="text-lg">{action.title}</CardTitle>
            <CardDescription className="text-sm">
              {action.description}
            </CardDescription>
          </CardHeader>
          {action.primary && (
            <CardContent className="pt-0">
              <Button 
                className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white"
                onClick={(e) => {
                  e.stopPropagation();
                  action.onClick();
                }}
              >
                <Zap className="h-4 w-4 mr-2" />
                Quick Start
              </Button>
            </CardContent>
          )}
        </Card>
      ))}
    </div>
  );
}