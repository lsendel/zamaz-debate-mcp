'use client';

import { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Plus } from 'lucide-react';
import { useTemplates, Template } from '@/hooks/use-templates';
import { TemplateFilters } from '@/components/templates/template-filters';
import { TemplateList } from '@/components/templates/template-list';
import { TemplateForm } from '@/components/templates/template-form';
import { TemplatePreview } from '@/components/templates/template-preview';

interface TemplateManagerProps {
  onSelectTemplate?: (_template: Template) => void;
  onClose?: () => void;
}

export function TemplateManager({ onSelectTemplate, onClose }: TemplateManagerProps) {
  const {
    templates,
    isLoading,
    loadTemplates,
    createTemplate,
    updateTemplate,
    deleteTemplate,
    previewTemplate
  } = useTemplates();

  // UI state
  const [selectedTemplate, setSelectedTemplate] = useState<Template | null>(null);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);
  const [previewContent, setPreviewContent] = useState('');
  
  // Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');

  // Load templates on mount
  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  // Memoized filtered templates
  const filteredTemplates = useMemo(() => {
    return templates.filter(template => {
      const matchesSearch = !searchQuery || 
        template.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        template.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        template.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()));
      
      const matchesCategory = selectedCategory === 'all' || template.category === selectedCategory;
      
      return matchesSearch && matchesCategory;
    });
  }, [templates, searchQuery, selectedCategory]);

  // Memoized categories
  const categories = useMemo(() => {
    const cats = new Set(templates.map(t => t.category));
    return Array.from(cats).sort();
  }, [templates]);

  // Event handlers
  const handleEdit = (template: Template) => {
    setSelectedTemplate(template);
    setIsEditOpen(true);
  };

  const handleDelete = async (template: Template) => {
    if (!confirm(`Are you sure you want to delete "${template.name}"?`)) return;
    
    const result = await deleteTemplate(template.id);
    if (!result.success) {
      alert('Failed to delete template. Please try again.');
    }
  };

  const handlePreview = async (template: Template) => {
    setSelectedTemplate(template);
    setIsPreviewOpen(true);
    
    const result = await previewTemplate(template);
    if (result.success) {
      setPreviewContent(result.content);
    }
  };

  const handleCreateSubmit = async (formData: any) => {
    const result = await createTemplate(formData);
    return result;
  };

  const handleEditSubmit = async (formData: any) => {
    if (!selectedTemplate) return { success: false };
    const result = await updateTemplate(selectedTemplate.id, formData);
    return result;
  };

  const handleSelectTemplate = (template: Template) => {
    onSelectTemplate?.(template);
    onClose?.();
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold">Template Manager</h2>
          <p className="text-muted-foreground">
            Create and manage reusable debate templates
          </p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          New Template
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Filter Templates</CardTitle>
        </CardHeader>
        <CardContent>
          <TemplateFilters
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            selectedCategory={selectedCategory}
            onCategoryChange={setSelectedCategory}
            categories={categories}
          />
        </CardContent>
      </Card>

      {/* Results count */}
      {!isLoading && (
        <div className="text-sm text-muted-foreground">
          Showing {filteredTemplates.length} of {templates.length} templates
        </div>
      )}

      {/* Template List */}
      <TemplateList
        templates={filteredTemplates}
        isLoading={isLoading}
        onEdit={handleEdit}
        onDelete={handleDelete}
        onPreview={handlePreview}
      />

      {/* Create Template Dialog */}
      <TemplateForm
        open={isCreateOpen}
        onOpenChange={setIsCreateOpen}
        onSubmit={handleCreateSubmit}
        mode="create"
      />

      {/* Edit Template Dialog */}
      <TemplateForm
        open={isEditOpen}
        onOpenChange={setIsEditOpen}
        onSubmit={handleEditSubmit}
        template={selectedTemplate}
        mode="edit"
      />

      {/* Preview Template Dialog */}
      <TemplatePreview
        open={isPreviewOpen}
        onOpenChange={setIsPreviewOpen}
        template={selectedTemplate}
        previewContent={previewContent}
        onSelectTemplate={onSelectTemplate ? handleSelectTemplate : undefined}
      />
    </div>
  );
}