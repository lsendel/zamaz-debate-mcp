'use client';

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface Organization {
  id: string;
  name: string;
  createdAt: string;
  debateCount?: number;
  lastActive?: string;
}

interface OrganizationContextType {
  currentOrg: Organization | null;
  setCurrentOrg: (org: Organization) => void;
  organizationHistory: OrganizationHistoryEntry[];
  addHistoryEntry: (entry: Omit<OrganizationHistoryEntry, 'id' | 'timestamp'>) => void;
}

interface OrganizationHistoryEntry {
  id: string;
  organizationId: string;
  action: string;
  description: string;
  timestamp: string;
  userId?: string;
  metadata?: Record<string, any>;
}

const OrganizationContext = createContext<OrganizationContextType | undefined>(undefined);

export function OrganizationProvider({ children }: { children: ReactNode }) {
  const [currentOrg, setCurrentOrgState] = useState<Organization | null>(null);
  const [organizationHistory, setOrganizationHistory] = useState<OrganizationHistoryEntry[]>([]);

  useEffect(() => {
    // Load current organization from localStorage
    const savedOrgId = localStorage.getItem('currentOrganizationId');
    const savedOrgs = localStorage.getItem('organizations');
    
    if (savedOrgId && savedOrgs) {
      const orgs = JSON.parse(savedOrgs);
      const org = orgs.find((o: Organization) => o.id === savedOrgId);
      if (org) {
        setCurrentOrgState(org);
      }
    }

    // Load history
    const savedHistory = localStorage.getItem('organizationHistory');
    if (savedHistory) {
      setOrganizationHistory(JSON.parse(savedHistory));
    }
  }, []);

  const setCurrentOrg = (org: Organization) => {
    setCurrentOrgState(org);
    localStorage.setItem('currentOrganizationId', org.id);
    
    // Add history entry for organization switch
    addHistoryEntry({
      organizationId: org.id,
      action: 'organization_switched',
      description: `Switched to organization: ${org.name}`
    });
  };

  const addHistoryEntry = (entry: Omit<OrganizationHistoryEntry, 'id' | 'timestamp'>) => {
    const newEntry: OrganizationHistoryEntry = {
      ...entry,
      id: `history-${Date.now()}`,
      timestamp: new Date().toISOString()
    };
    
    const updatedHistory = [...organizationHistory, newEntry];
    setOrganizationHistory(updatedHistory);
    
    // Keep only last 100 entries per organization
    const orgHistory = updatedHistory.filter(h => h.organizationId === entry.organizationId);
    if (orgHistory.length > 100) {
      const trimmedHistory = updatedHistory.filter(h => 
        h.organizationId !== entry.organizationId || 
        orgHistory.slice(-100).includes(h)
      );
      localStorage.setItem('organizationHistory', JSON.stringify(trimmedHistory));
    } else {
      localStorage.setItem('organizationHistory', JSON.stringify(updatedHistory));
    }
  };

  return (
    <OrganizationContext.Provider value={{ 
      currentOrg, 
      setCurrentOrg, 
      organizationHistory,
      addHistoryEntry 
    }}>
      {children}
    </OrganizationContext.Provider>
  );
}

export function useOrganization() {
  const context = useContext(OrganizationContext);
  if (context === undefined) {
    throw new Error('useOrganization must be used within an OrganizationProvider');
  }
  return context;
}