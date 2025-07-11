'use client';

import { useState, useEffect } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { CreateDebateDialog } from '@/components/create-debate-dialog';
import { DebateView } from '@/components/debate-view';
import { OllamaSetup } from '@/components/ollama-setup';
import { MCPClient } from '@/lib/mcp-client';
import { Debate } from '@/types/debate';
import { OrganizationSwitcher } from '@/components/organization-switcher';
import { useOrganization } from '@/hooks/use-organization';
import { Users, MessageSquare, Brain, Settings, Plus, Clock, Activity, Zap, Globe, Cpu } from 'lucide-react';
import { OnboardingWizard } from '@/components/onboarding-wizard';
import { QuickActions } from '@/components/quick-actions';
import { DebateTemplates } from '@/components/debate-templates';
import { KeyboardShortcutsDialog } from '@/components/keyboard-shortcuts-dialog';
import { useKeyboardShortcuts, DEFAULT_SHORTCUTS } from '@/hooks/use-keyboard-shortcuts';
import { useRouter } from 'next/navigation';

export default function HomePage() {
  const [debates, setDebates] = useState<Debate[]>([]);
  const [selectedDebate, setSelectedDebate] = useState<Debate | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [showTemplates, setShowTemplates] = useState(false);
  const { currentOrg, addHistoryEntry } = useOrganization();
  const router = useRouter();

  const debateClient = new MCPClient('debate');

  // Set up keyboard shortcuts
  useKeyboardShortcuts([
    ...DEFAULT_SHORTCUTS,
    {
      key: 'n',
      ctrl: true,
      description: 'Create new debate',
      handler: () => setIsCreateOpen(true)
    }
  ]);

  useEffect(() => {
    if (currentOrg) {
      loadDebates();
    }
  }, [currentOrg]);

  const loadDebates = async () => {
    try {
      setIsLoading(true);
      const response = await debateClient.readResource('debate://debates');
      setDebates(response.debates || []);
    } catch (error) {
      console.error('Failed to load debates:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateDebate = async (debate: any) => {
    try {
      const response = await debateClient.callTool('create_debate', debate);
      await loadDebates();
      setIsCreateOpen(false);
      
      // Add history entry
      if (currentOrg) {
        addHistoryEntry({
          organizationId: currentOrg.id,
          action: 'debate_created',
          description: `Created debate: ${debate.name}`,
          metadata: { debateId: response.debateId, topic: debate.topic }
        });
      }
    } catch (error) {
      console.error('Failed to create debate:', error);
      alert('Failed to create debate. Please check the services are running.');
    }
  };

  const handleTemplateSelect = (template: any) => {
    setIsCreateOpen(true);
    setShowTemplates(false);
    // TODO: Pre-fill the create debate dialog with template data
  };

  const handleViewHistory = () => {
    const orgSwitcher = document.querySelector('[aria-expanded]');
    if (orgSwitcher) {
      (orgSwitcher as HTMLElement).click();
      setTimeout(() => {
        const historyButton = Array.from(document.querySelectorAll('button')).find(
          btn => btn.textContent?.includes('View History')
        );
        if (historyButton) historyButton.click();
      }, 100);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'active': return 'bg-emerald-500 text-white';
      case 'paused': return 'bg-amber-500 text-white';
      case 'completed': return 'bg-blue-500 text-white';
      case 'draft': return 'bg-gray-500 text-white';
      default: return 'bg-gray-400 text-white';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'active': return <Activity className="h-3 w-3" />;
      case 'paused': return <Clock className="h-3 w-3" />;
      case 'completed': return <MessageSquare className="h-3 w-3" />;
      default: return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-gray-50 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950">
      {/* Header */}
      <header className="border-b bg-white/50 backdrop-blur-md dark:bg-gray-900/50 sticky top-0 z-40 header-overlay">
        <div className="container mx-auto px-4 py-6">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-xl bg-gradient-to-br from-blue-600 to-purple-600 flex items-center justify-center shadow-lg">
                <Brain className="h-7 w-7 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold bg-gradient-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-400 bg-clip-text text-transparent">
                  AI Debate System
                </h1>
                <p className="text-sm text-muted-foreground flex items-center gap-2 mt-1">
                  <Cpu className="h-3 w-3" />
                  Powered by MCP & Ollama
                </p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <OrganizationSwitcher />
              <Button 
                onClick={() => setIsCreateOpen(true)} 
                size="lg"
                className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white shadow-lg"
              >
                <Plus className="h-5 w-5 mr-2" />
                New Debate
              </Button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-8 main-content relative z-10">
        {/* Onboarding */}
        <OnboardingWizard
          onComplete={() => {
            loadDebates();
          }}
          onCreateOrganization={() => {
            const orgSwitcher = document.querySelector('[aria-expanded]');
            if (orgSwitcher) {
              (orgSwitcher as HTMLElement).click();
              setTimeout(() => {
                const createButton = Array.from(document.querySelectorAll('button')).find(
                  btn => btn.textContent?.includes('Create Organization')
                );
                if (createButton) createButton.click();
              }, 100);
            }
          }}
          onCreateDebate={() => setIsCreateOpen(true)}
        />

        {/* Quick Actions */}
        <QuickActions
          onNewDebate={() => setIsCreateOpen(true)}
          onViewHistory={handleViewHistory}
          onManageOrganizations={() => {
            const orgSwitcher = document.querySelector('[aria-expanded]');
            if (orgSwitcher) (orgSwitcher as HTMLElement).click();
          }}
          debateCount={debates.length}
          activeDebates={debates.filter(d => d.status === 'active').length}
        />
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <Card className="border-0 shadow-md bg-gradient-to-br from-blue-50 to-blue-100 dark:from-blue-950 dark:to-blue-900">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-blue-600 dark:text-blue-400">Total Debates</p>
                  <p className="text-3xl font-bold text-blue-900 dark:text-blue-100">{debates.length}</p>
                </div>
                <Globe className="h-8 w-8 text-blue-500 opacity-50" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md bg-gradient-to-br from-emerald-50 to-emerald-100 dark:from-emerald-950 dark:to-emerald-900">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-emerald-600 dark:text-emerald-400">Active</p>
                  <p className="text-3xl font-bold text-emerald-900 dark:text-emerald-100">
                    {debates.filter(d => d.status === 'active').length}
                  </p>
                </div>
                <Activity className="h-8 w-8 text-emerald-500 opacity-50" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md bg-gradient-to-br from-purple-50 to-purple-100 dark:from-purple-950 dark:to-purple-900">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-purple-600 dark:text-purple-400">Completed</p>
                  <p className="text-3xl font-bold text-purple-900 dark:text-purple-100">
                    {debates.filter(d => d.status === 'completed').length}
                  </p>
                </div>
                <MessageSquare className="h-8 w-8 text-purple-500 opacity-50" />
              </div>
            </CardContent>
          </Card>

          <Card className="border-0 shadow-md bg-gradient-to-br from-amber-50 to-amber-100 dark:from-amber-950 dark:to-amber-900">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-amber-600 dark:text-amber-400">AI Models</p>
                  <p className="text-3xl font-bold text-amber-900 dark:text-amber-100">4</p>
                </div>
                <Zap className="h-8 w-8 text-amber-500 opacity-50" />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="debates" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4 lg:w-[600px] bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm">
            <TabsTrigger value="debates" className="data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800">
              <MessageSquare className="h-4 w-4 mr-2" />
              Debates
            </TabsTrigger>
            <TabsTrigger value="templates" className="data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800">
              <Brain className="h-4 w-4 mr-2" />
              Templates
            </TabsTrigger>
            <TabsTrigger value="active" className="data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800">
              <Activity className="h-4 w-4 mr-2" />
              Active
            </TabsTrigger>
            <TabsTrigger value="ollama" className="data-[state=active]:bg-white dark:data-[state=active]:bg-gray-800">
              <Settings className="h-4 w-4 mr-2" />
              Setup
            </TabsTrigger>
          </TabsList>

          <TabsContent value="debates" className="space-y-4">
            {isLoading ? (
              <Card className="border-0 shadow-lg">
                <CardContent className="py-12 text-center">
                  <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
                    <div className="h-6 w-6 border-2 border-gray-300 dark:border-gray-600 border-t-transparent rounded-full animate-spin" />
                  </div>
                  <p className="text-muted-foreground">Loading debates...</p>
                </CardContent>
              </Card>
            ) : debates.length === 0 ? (
              <Card className="border-0 shadow-lg bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800">
                <CardContent className="py-16 text-center">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-200 dark:bg-gray-700 mb-4">
                    <MessageSquare className="h-8 w-8 text-gray-400" />
                  </div>
                  <h3 className="text-xl font-semibold mb-2">No debates yet</h3>
                  <p className="text-muted-foreground mb-6">Create your first AI-powered debate</p>
                  <Button 
                    onClick={() => setIsCreateOpen(true)}
                    className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white"
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Create Debate
                  </Button>
                </CardContent>
              </Card>
            ) : (
              <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                {debates.map((debate) => (
                  <Card
                    key={debate.id}
                    className="border-0 shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1 bg-white dark:bg-gray-800"
                    onClick={() => setSelectedDebate(debate)}
                  >
                    <CardHeader className="pb-4">
                      <div className="flex justify-between items-start mb-2">
                        <Badge className={cn("flex items-center gap-1", getStatusColor(debate.status))}>
                          {getStatusIcon(debate.status)}
                          {debate.status}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {new Date(debate.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      <CardTitle className="text-xl">{debate.name}</CardTitle>
                      <CardDescription className="line-clamp-2">{debate.topic}</CardDescription>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-center gap-4 text-sm text-muted-foreground mb-4">
                        <div className="flex items-center gap-1.5">
                          <Users className="h-4 w-4" />
                          <span>{debate.participants.length} participants</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <MessageSquare className="h-4 w-4" />
                          <span>Round {debate.currentRound || 1}</span>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {debate.participants.map((p, i) => (
                          <Badge 
                            key={i} 
                            variant="outline"
                            className="bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-700 border-gray-200 dark:border-gray-600"
                          >
                            <Cpu className="h-3 w-3 mr-1" />
                            {p.name}
                          </Badge>
                        ))}
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="active">
            {selectedDebate ? (
              <DebateView
                debate={selectedDebate}
                onUpdate={loadDebates}
              />
            ) : (
              <Card className="border-0 shadow-lg">
                <CardContent className="py-16 text-center">
                  <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gray-100 dark:bg-gray-800 mb-4">
                    <Activity className="h-8 w-8 text-gray-400" />
                  </div>
                  <h3 className="text-xl font-semibold mb-2">No active debate</h3>
                  <p className="text-muted-foreground">
                    Select a debate from the Debates tab to view it here
                  </p>
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="templates">
            <DebateTemplates onSelectTemplate={handleTemplateSelect} />
          </TabsContent>

          <TabsContent value="ollama">
            <OllamaSetup />
          </TabsContent>
        </Tabs>

        <CreateDebateDialog
          open={isCreateOpen}
          onOpenChange={setIsCreateOpen}
          onSubmit={handleCreateDebate}
        />

        <KeyboardShortcutsDialog />
      </main>
    </div>
  );
}

function cn(...classes: string[]) {
  return classes.filter(Boolean).join(' ');
}