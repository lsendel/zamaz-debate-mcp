'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Building2, Check, Plus, ChevronDown, History } from 'lucide-react';
import { useOrganization } from '@/hooks/use-organization';
import { logger } from '@/lib/logger';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { OrganizationHistory } from './organization-history';

interface Organization {
  id: string;
  name: string;
  createdAt: string;
  debateCount?: number;
  lastActive?: string;
}

export function OrganizationSwitcher() {
  const { currentOrg, setCurrentOrg, addHistoryEntry } = useOrganization();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [newOrgName, setNewOrgName] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const loadOrganizations = useCallback(async () => {
    try {
      setIsLoading(true);
      // Load from localStorage for now - in production this would be an API call
      const savedOrgs = localStorage.getItem('organizations');
      if (savedOrgs) {
        setOrganizations(JSON.parse(savedOrgs));
      } else {
        // Create default organization
        const defaultOrg: Organization = {
          id: 'default-org',
          name: 'Default Organization',
          createdAt: new Date().toISOString()
        };
        setOrganizations([defaultOrg]);
        localStorage.setItem('organizations', JSON.stringify([defaultOrg]));
        if (!currentOrg) {
          setCurrentOrg(defaultOrg);
        }
      }
    } catch (error) {
      logger.error('Failed to load organizations', error as Error);
    } finally {
      setIsLoading(false);
    }
  }, [currentOrg, setCurrentOrg]);

  useEffect(() => {
    loadOrganizations();
  }, [loadOrganizations]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const createOrganization = async () => {
    if (!newOrgName.trim()) return;

    const newOrg: Organization = {
      id: `org-${Date.now()}`,
      name: newOrgName,
      createdAt: new Date().toISOString(),
      debateCount: 0
    };

    const updatedOrgs = [...organizations, newOrg];
    setOrganizations(updatedOrgs);
    localStorage.setItem('organizations', JSON.stringify(updatedOrgs));
    
    // Add history entry for creation
    addHistoryEntry({
      organizationId: newOrg.id,
      action: 'organization_created',
      description: `Created organization: ${newOrg.name}`
    });
    
    // Switch to new organization
    setCurrentOrg(newOrg);
    setIsCreateOpen(false);
    setNewOrgName('');
  };

  const switchOrganization = (org: Organization) => {
    setCurrentOrg(org);
    // Update last active
    const updatedOrgs = organizations.map(o => 
      o.id === org.id ? { ...o, lastActive: new Date().toISOString() } : o
    );
    setOrganizations(updatedOrgs);
    localStorage.setItem('organizations', JSON.stringify(updatedOrgs));
  };

  const formatLastActive = (date?: string) => {
    if (!date) return 'Never';
    const diff = Date.now() - new Date(date).getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (days === 0) return 'Today';
    if (days === 1) return 'Yesterday';
    return `${days} days ago`;
  };

  if (isLoading || !currentOrg) {
    return (
      <div className="h-10 w-48 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
    );
  }

  return (
    <>
      <div className="relative" ref={dropdownRef}>
        <Button 
          variant="outline" 
          className="w-[240px] justify-between focus:bg-accent focus:text-accent-foreground hover:bg-accent hover:text-accent-foreground relative z-50"
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          aria-expanded={isDropdownOpen}
          aria-haspopup="true"
        >
          <div className="flex items-center gap-2">
            <Building2 className="h-4 w-4" />
            <span className="truncate">{currentOrg.name}</span>
          </div>
          <ChevronDown className={`h-4 w-4 opacity-50 transition-transform ${isDropdownOpen ? 'rotate-180' : ''}`} />
        </Button>
        
        {isDropdownOpen && (
          <div className="absolute top-full left-0 mt-2 w-[240px] bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-md shadow-lg z-[100] py-1">
            <div className="flex items-center justify-between px-3 py-2 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-semibold">Organizations</span>
              <Badge variant="secondary" className="ml-2">
                {organizations.length}
              </Badge>
            </div>
            
            <div className="max-h-64 overflow-y-auto">
              {organizations.map((org) => (
                <button
                  key={org.id}
                  onClick={() => {
                    switchOrganization(org);
                    setIsDropdownOpen(false);
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-800 focus:bg-gray-100 dark:focus:bg-gray-800 focus:outline-none"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Building2 className="h-4 w-4" />
                      <div>
                        <div className="font-medium text-sm">{org.name}</div>
                        <div className="text-xs text-muted-foreground">
                          {org.debateCount || 0} debates • {formatLastActive(org.lastActive)}
                        </div>
                      </div>
                    </div>
                    {currentOrg.id === org.id && (
                      <Check className="h-4 w-4 text-blue-600" />
                    )}
                  </div>
                </button>
              ))}
            </div>
            
            <div className="border-t border-gray-200 dark:border-gray-700 mt-1">
              <button
                onClick={() => {
                  setIsCreateOpen(true);
                  setIsDropdownOpen(false);
                }}
                className="w-full px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-800 focus:bg-gray-100 dark:focus:bg-gray-800 focus:outline-none flex items-center gap-2 text-sm"
              >
                <Plus className="h-4 w-4" />
                Create Organization
              </button>
              
              <button
                onClick={() => {
                  setIsHistoryOpen(true);
                  setIsDropdownOpen(false);
                }}
                className="w-full px-3 py-2 text-left hover:bg-gray-100 dark:hover:bg-gray-800 focus:bg-gray-100 dark:focus:bg-gray-800 focus:outline-none flex items-center gap-2 text-sm"
              >
                <History className="h-4 w-4" />
                View History
              </button>
            </div>
          </div>
        )}
      </div>

      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create New Organization</DialogTitle>
            <DialogDescription>
              Create a new organization to manage separate debate contexts and histories.
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div>
              <Label htmlFor="org-name">Organization Name</Label>
              <Input
                id="org-name"
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
                placeholder="e.g., Research Team Alpha"
                className="mt-1"
              />
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button onClick={createOrganization}>
              Create Organization
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {currentOrg && (
        <OrganizationHistory
          open={isHistoryOpen}
          onOpenChange={setIsHistoryOpen}
          organizationId={currentOrg.id}
          organizationName={currentOrg.name}
        />
      )}
    </>
  );
}