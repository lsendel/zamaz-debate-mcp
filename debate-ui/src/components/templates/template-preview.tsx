'use client';

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Template } from '@/hooks/use-templates';
import { FileText, Hash, Clock, Users } from 'lucide-react';

interface TemplatePreviewProps {
  open: boolean;
  onOpenChange: (_open: boolean) => void;
  template: Template | null;
  previewContent: string;
  onSelectTemplate?: (_template: Template) => void;
}

export function TemplatePreview({
  open,
  onOpenChange,
  template,
  previewContent,
  onSelectTemplate
}: TemplatePreviewProps) {
  if (!template) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-4xl max-h-[90vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="w-5 h-5" />
            {template.name}
          </DialogTitle>
          <DialogDescription>
            Preview template content and structure
          </DialogDescription>
        </DialogHeader>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[600px]">
          {/* Template Info */}
          <div className="space-y-4">
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">Template Details</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <div className="text-sm font-medium mb-1">Category</div>
                  <Badge variant="outline">{template.category}</Badge>
                </div>

                {template.subcategory && (
                  <div>
                    <div className="text-sm font-medium mb-1">Subcategory</div>
                    <Badge variant="secondary">{template.subcategory}</Badge>
                  </div>
                )}

                <div>
                  <div className="text-sm font-medium mb-1">Description</div>
                  <p className="text-sm text-muted-foreground">
                    {template.description || 'No description provided'}
                  </p>
                </div>

                {template.tags.length > 0 && (
                  <div>
                    <div className="text-sm font-medium mb-2">Tags</div>
                    <div className="flex flex-wrap gap-1">
                      {template.tags.map((tag) => (
                        <Badge key={tag} variant="secondary" className="text-xs">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                <div className="pt-3 space-y-2 text-xs text-muted-foreground">
                  <div className="flex items-center gap-2">
                    <Hash className="w-3 h-3" />
                    <span>Used {template.usage_count} times</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Clock className="w-3 h-3" />
                    <span>Updated {new Date(template.updated_at).toLocaleDateString()}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Users className="w-3 h-3" />
                    <span>{template.variables.length} variables</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Variables */}
            {template.variables.length > 0 && (
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-base">Template Variables</CardTitle>
                  <CardDescription className="text-xs">
                    These variables can be customized when using the template
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <ScrollArea className="h-48">
                    <div className="space-y-3">
                      {template.variables.map((variable, index) => (
                        <div key={index} className="p-2 border rounded-sm">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-sm font-mono text-blue-600">
                              {`{{${variable.name}}}`}
                            </span>
                            <Badge variant="outline" className="text-xs">
                              {variable.type}
                            </Badge>
                            {variable.required && (
                              <Badge variant="destructive" className="text-xs">
                                required
                              </Badge>
                            )}
                          </div>
                          <p className="text-xs text-muted-foreground">
                            {variable.description}
                          </p>
                          {variable.default_value && (
                            <p className="text-xs text-muted-foreground mt-1">
                              Default: {variable.default_value}
                            </p>
                          )}
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </CardContent>
              </Card>
            )}
          </div>

          {/* Template Content */}
          <div className="lg:col-span-2">
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="text-base">Template Content</CardTitle>
                <CardDescription>
                  Raw template with variable placeholders
                </CardDescription>
              </CardHeader>
              <CardContent>
                <ScrollArea className="h-[500px]">
                  <div className="space-y-4">
                    {/* Raw Content */}
                    <div>
                      <div className="text-sm font-medium mb-2">Raw Template</div>
                      <div className="p-3 bg-muted rounded-md">
                        <pre className="text-sm whitespace-pre-wrap font-mono">
                          {template.content}
                        </pre>
                      </div>
                    </div>

                    {/* Preview Content */}
                    {previewContent && (
                      <div>
                        <div className="text-sm font-medium mb-2">Preview (with sample data)</div>
                        <div className="p-3 border rounded-md">
                          <div className="text-sm whitespace-pre-wrap">
                            {previewContent}
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </ScrollArea>
              </CardContent>
            </Card>
          </div>
        </div>

        {onSelectTemplate && (
          <div className="flex justify-end pt-4 border-t">
            <button
              onClick={() => onSelectTemplate(template)}
              className="px-4 py-2 bg-primary text-primary-foreground rounded-md hover:bg-primary/90 transition-colors"
            >
              Use This Template
            </button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}