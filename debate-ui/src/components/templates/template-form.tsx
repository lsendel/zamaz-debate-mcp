'use client';

import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Plus, X } from 'lucide-react';
import { Template, TemplateFormData, TemplateVariable } from '@/hooks/use-templates';

interface TemplateFormProps {
  open: boolean;
  onOpenChange: (_open: boolean) => void;
  onSubmit: (_data: TemplateFormData) => Promise<{ success: boolean }>;
  template?: Template | null;
  mode: 'create' | 'edit';
}

const DEFAULT_FORM_DATA: TemplateFormData = {
  name: '',
  description: '',
  category: '',
  subcategory: '',
  content: '',
  variables: [],
  tags: []
};

const CATEGORIES = [
  'Educational',
  'Business',
  'Philosophy',
  'Science',
  'Politics',
  'Ethics',
  'Technology',
  'Other'
];

export function TemplateForm({
  open,
  onOpenChange,
  onSubmit,
  template,
  mode
}: TemplateFormProps) {
  const [formData, setFormData] = useState<TemplateFormData>(DEFAULT_FORM_DATA);
  const [newTag, setNewTag] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Initialize form data when template changes
  useEffect(() => {
    if (template && mode === 'edit') {
      setFormData({
        name: template.name,
        description: template.description || '',
        category: template.category,
        subcategory: template.subcategory || '',
        content: template.content,
        variables: template.variables,
        tags: template.tags
      });
    } else {
      setFormData(DEFAULT_FORM_DATA);
    }
  }, [template, mode]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.content.trim()) return;

    setIsSubmitting(true);
    try {
      const result = await onSubmit(formData);
      if (result.success) {
        setFormData(DEFAULT_FORM_DATA);
        onOpenChange(false);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const addTag = () => {
    if (newTag.trim() && !formData.tags.includes(newTag.trim())) {
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, newTag.trim()]
      }));
      setNewTag('');
    }
  };

  const removeTag = (tagToRemove: string) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags.filter(tag => tag !== tagToRemove)
    }));
  };

  const addVariable = () => {
    const newVariable: TemplateVariable = {
      name: '',
      type: 'string',
      description: '',
      required: false
    };
    setFormData(prev => ({
      ...prev,
      variables: [...prev.variables, newVariable]
    }));
  };

  const updateVariable = (index: number, field: keyof TemplateVariable, value: any) => {
    setFormData(prev => ({
      ...prev,
      variables: prev.variables.map((variable, i) =>
        i === index ? { ...variable, [field]: value } : variable
      )
    }));
  };

  const removeVariable = (index: number) => {
    setFormData(prev => ({
      ...prev,
      variables: prev.variables.filter((_, i) => i !== index)
    }));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {mode === 'create' ? 'Create New Template' : 'Edit Template'}
          </DialogTitle>
          <DialogDescription>
            {mode === 'create' 
              ? 'Create a reusable template for future debates'
              : 'Update the template details and content'
            }
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">Template Name *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Enter template name"
                required
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="category">Category *</Label>
              <Select
                value={formData.category}
                onValueChange={(value) => setFormData(prev => ({ ...prev, category: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((category) => (
                    <SelectItem key={category} value={category}>
                      {category}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
              placeholder="Describe what this template is for"
              rows={3}
            />
          </div>

          {/* Tags */}
          <div className="space-y-2">
            <Label>Tags</Label>
            <div className="flex gap-2 mb-2">
              <Input
                value={newTag}
                onChange={(e) => setNewTag(e.target.value)}
                placeholder="Add a tag"
                onKeyPress={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())}
              />
              <Button type="button" onClick={addTag} size="sm">
                <Plus className="w-4 h-4" />
              </Button>
            </div>
            <div className="flex flex-wrap gap-1">
              {formData.tags.map((tag) => (
                <Badge key={tag} variant="secondary" className="gap-1">
                  {tag}
                  <button
                    type="button"
                    onClick={() => removeTag(tag)}
                    className="hover:text-destructive"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </Badge>
              ))}
            </div>
          </div>

          {/* Content */}
          <div className="space-y-2">
            <Label htmlFor="content">Template Content *</Label>
            <Textarea
              id="content"
              value={formData.content}
              onChange={(e) => setFormData(prev => ({ ...prev, content: e.target.value }))}
              placeholder="Enter the template content with variables like {{variable_name}}"
              rows={8}
              required
            />
          </div>

          {/* Variables */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Template Variables</Label>
              <Button type="button" onClick={addVariable} size="sm" variant="outline">
                <Plus className="w-4 h-4 mr-1" />
                Add Variable
              </Button>
            </div>
            
            {formData.variables.map((variable, index) => (
              <div key={index} className="grid grid-cols-6 gap-2 items-end p-3 border rounded">
                <div className="space-y-1">
                  <Label className="text-xs">Name</Label>
                  <Input
                    value={variable.name}
                    onChange={(e) => updateVariable(index, 'name', e.target.value)}
                    placeholder="variable_name"
                    className="h-8"
                  />
                </div>
                
                <div className="space-y-1">
                  <Label className="text-xs">Type</Label>
                  <Select
                    value={variable.type}
                    onValueChange={(value) => updateVariable(index, 'type', value)}
                  >
                    <SelectTrigger className="h-8">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="string">String</SelectItem>
                      <SelectItem value="number">Number</SelectItem>
                      <SelectItem value="boolean">Boolean</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                
                <div className="col-span-3 space-y-1">
                  <Label className="text-xs">Description</Label>
                  <Input
                    value={variable.description}
                    onChange={(e) => updateVariable(index, 'description', e.target.value)}
                    placeholder="Describe this variable"
                    className="h-8"
                  />
                </div>
                
                <Button
                  type="button"
                  onClick={() => removeVariable(index)}
                  size="sm"
                  variant="outline"
                  className="text-destructive"
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            ))}
          </div>
        </form>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create Template' : 'Update Template'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}