'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Brain, Cpu, Globe, Shield, Heart, Briefcase, Sparkles, ArrowRight } from 'lucide-react';

interface DebateTemplate {
  id: string;
  title: string;
  description: string;
  topic: string;
  category: string;
  icon: React.ReactNode;
  participants: {
    name: string;
    position: string;
    model: string;
  }[];
  difficulty: 'beginner' | 'intermediate' | 'advanced';
  estimatedTime: string;
}

interface DebateTemplatesProps {
  onSelectTemplate: (_template: DebateTemplate) => void;
}

export function DebateTemplates({ onSelectTemplate }: DebateTemplatesProps) {
  const templates: DebateTemplate[] = [
    {
      id: 'ai-ethics',
      title: 'AI Ethics & Society',
      description: 'Explore the ethical implications of artificial intelligence on society',
      topic: 'Should AI development be regulated by international standards?',
      category: 'Technology',
      icon: <Brain className="h-5 w-5" />,
      participants: [
        { name: 'Tech Optimist', position: 'Pro-Innovation', model: 'llama3' },
        { name: 'Ethics Guardian', position: 'Pro-Regulation', model: 'mistral' }
      ],
      difficulty: 'beginner',
      estimatedTime: '20-30 min'
    },
    {
      id: 'climate-tech',
      title: 'Climate & Technology',
      description: 'Debate the role of technology in solving climate change',
      topic: 'Can technology alone solve the climate crisis?',
      category: 'Environment',
      icon: <Globe className="h-5 w-5" />,
      participants: [
        { name: 'Tech Solutionist', position: 'Technology-First', model: 'llama3' },
        { name: 'Systemic Thinker', position: 'Holistic Approach', model: 'codellama' }
      ],
      difficulty: 'intermediate',
      estimatedTime: '30-40 min'
    },
    {
      id: 'privacy-security',
      title: 'Privacy vs Security',
      description: 'Balance between personal privacy and collective security',
      topic: 'Should governments have backdoor access to encrypted communications?',
      category: 'Security',
      icon: <Shield className="h-5 w-5" />,
      participants: [
        { name: 'Security Expert', position: 'Pro-Access', model: 'mistral' },
        { name: 'Privacy Advocate', position: 'Pro-Encryption', model: 'llama3' }
      ],
      difficulty: 'advanced',
      estimatedTime: '40-50 min'
    },
    {
      id: 'healthcare-ai',
      title: 'AI in Healthcare',
      description: 'The future of AI-driven medical diagnosis and treatment',
      topic: 'Should AI be allowed to make autonomous medical decisions?',
      category: 'Healthcare',
      icon: <Heart className="h-5 w-5" />,
      participants: [
        { name: 'Medical Innovator', position: 'Pro-AI Healthcare', model: 'llama3' },
        { name: 'Medical Ethicist', position: 'Human-Centered Care', model: 'mistral' }
      ],
      difficulty: 'intermediate',
      estimatedTime: '25-35 min'
    },
    {
      id: 'remote-work',
      title: 'Future of Work',
      description: 'Remote work versus traditional office environments',
      topic: 'Is remote work the future of professional collaboration?',
      category: 'Business',
      icon: <Briefcase className="h-5 w-5" />,
      participants: [
        { name: 'Remote Advocate', position: 'Pro-Remote', model: 'llama3' },
        { name: 'Office Traditionalist', position: 'Pro-Office', model: 'codellama' }
      ],
      difficulty: 'beginner',
      estimatedTime: '20-30 min'
    }
  ];

  const getCategoryColor = (category: string) => {
    const colors: Record<string, string> = {
      Technology: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200',
      Environment: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
      Security: 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200',
      Healthcare: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
      Business: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200'
    };
    // Use Object.hasOwn to safely check property existence
    // eslint-disable-next-line security/detect-object-injection
    return Object.hasOwn(colors, category) ? colors[category] : 'bg-gray-100 text-gray-800';
  };

  const getDifficultyColor = (difficulty: string) => {
    const colors: Record<string, string> = {
      beginner: 'bg-emerald-500',
      intermediate: 'bg-amber-500',
      advanced: 'bg-red-500'
    };
    // Use Object.hasOwn to safely check property existence
    // eslint-disable-next-line security/detect-object-injection
    return Object.hasOwn(colors, difficulty) ? colors[difficulty] : 'bg-gray-500';
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold">Debate Templates</h2>
          <p className="text-muted-foreground">Start with a pre-configured debate on popular topics</p>
        </div>
        <Badge variant="secondary" className="flex items-center gap-1">
          <Sparkles className="h-3 w-3" />
          {templates.length} templates
        </Badge>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {templates.map((template) => (
          <Card
            key={template.id}
            className="border-0 shadow-lg hover:shadow-xl transition-all duration-300 cursor-pointer transform hover:-translate-y-1"
          >
            <CardHeader>
              <div className="flex items-start justify-between mb-3">
                <div className={`inline-flex items-center justify-center w-10 h-10 rounded-lg bg-gradient-to-br ${
                  getCategoryColor(template.category).includes('blue') ? 'from-blue-500 to-blue-600' :
                  getCategoryColor(template.category).includes('green') ? 'from-green-500 to-green-600' :
                  getCategoryColor(template.category).includes('purple') ? 'from-purple-500 to-purple-600' :
                  getCategoryColor(template.category).includes('red') ? 'from-red-500 to-red-600' :
                  'from-amber-500 to-amber-600'
                } text-white`}>
                  {template.icon}
                </div>
                <div className="flex gap-2">
                  <Badge variant="outline" className={getCategoryColor(template.category)}>
                    {template.category}
                  </Badge>
                  <Badge className={`${getDifficultyColor(template.difficulty)} text-white`}>
                    {template.difficulty}
                  </Badge>
                </div>
              </div>
              <CardTitle className="text-xl mb-2">{template.title}</CardTitle>
              <CardDescription className="mb-3">{template.description}</CardDescription>
              <p className="text-sm font-medium mb-3">&ldquo;{template.topic}&rdquo;</p>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Cpu className="h-4 w-4" />
                  <span>{template.participants.length} AI participants</span>
                  <span className="mx-2">•</span>
                  <span>{template.estimatedTime}</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  {template.participants.map((p, i) => (
                    <Badge key={i} variant="secondary" className="text-xs">
                      {p.name} ({p.model})
                    </Badge>
                  ))}
                </div>
                <Button 
                  className="w-full mt-4"
                  variant="outline"
                  onClick={() => onSelectTemplate(template)}
                >
                  Use Template
                  <ArrowRight className="h-4 w-4 ml-2" />
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}