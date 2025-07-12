'use client';

import { useState, useCallback } from 'react';
import { useOrganization } from '@/hooks/use-organization';
import { logger } from '@/lib/logger';

export interface Template {
  id: string;
  name: string;
  description?: string;
  category: string;
  subcategory?: string;
  template_type: string;
  content: string;
  variables: TemplateVariable[];
  tags: string[];
  status: string;
  usage_count: number;
  created_at: string;
  updated_at: string;
}

export interface TemplateVariable {
  name: string;
  type: string;
  description: string;
  required: boolean;
  default_value?: any;
}

export interface TemplateFormData {
  name: string;
  description: string;
  category: string;
  subcategory: string;
  content: string;
  variables: TemplateVariable[];
  tags: string[];
}

export function useTemplates() {
  const { currentOrg } = useOrganization();
  const [templates, setTemplates] = useState<Template[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadTemplates = useCallback(async () => {
    try {
      setIsLoading(true);
      const response = await fetch('/api/template/list', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          organization_id: currentOrg?.id || 'system'
        })
      });

      if (response.ok) {
        const data = await response.json();
        setTemplates(data.templates || []);
      }
    } catch (error) {
      logger.error('Failed to load templates', error as Error);
    } finally {
      setIsLoading(false);
    }
  }, [currentOrg?.id]);

  const createTemplate = useCallback(async (formData: TemplateFormData) => {
    try {
      const response = await fetch('/api/template/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          organization_id: currentOrg?.id || 'system',
          ...formData
        })
      });

      if (response.ok) {
        await loadTemplates();
        return { success: true };
      }
      return { success: false };
    } catch (error) {
      logger.error('Failed to create template', error as Error);
      return { success: false };
    }
  }, [currentOrg?.id, loadTemplates]);

  const updateTemplate = useCallback(async (templateId: string, formData: TemplateFormData) => {
    try {
      const response = await fetch('/api/template/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: templateId,
          organization_id: currentOrg?.id || 'system',
          ...formData
        })
      });

      if (response.ok) {
        await loadTemplates();
        return { success: true };
      }
      return { success: false };
    } catch (error) {
      logger.error('Failed to update template', error as Error);
      return { success: false };
    }
  }, [currentOrg?.id, loadTemplates]);

  const deleteTemplate = useCallback(async (templateId: string) => {
    try {
      const response = await fetch('/api/template/delete', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: templateId,
          organization_id: currentOrg?.id || 'system'
        })
      });

      if (response.ok) {
        await loadTemplates();
        return { success: true };
      }
      return { success: false };
    } catch (error) {
      logger.error('Failed to delete template', error as Error);
      return { success: false };
    }
  }, [currentOrg?.id, loadTemplates]);

  const previewTemplate = useCallback(async (template: Template) => {
    try {
      const response = await fetch('/api/template/preview', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          template_id: template.id,
          organization_id: currentOrg?.id || 'system'
        })
      });

      if (response.ok) {
        const data = await response.json();
        return { success: true, content: data.content };
      }
      return { success: false, content: '' };
    } catch (error) {
      logger.error('Failed to preview template', error as Error);
      return { success: false, content: '' };
    }
  }, [currentOrg?.id]);

  return {
    templates,
    isLoading,
    loadTemplates,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    previewTemplate
  };
}