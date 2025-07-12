'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Brain, Building2, MessageSquare, ArrowRight, Check, Rocket } from 'lucide-react';

interface OnboardingStep {
  id: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  action?: () => void;
  completed?: boolean;
}

interface OnboardingWizardProps {
  onComplete: () => void;
  onCreateOrganization: () => void;
  onCreateDebate: () => void;
}

export function OnboardingWizard({ onComplete, onCreateOrganization, onCreateDebate }: OnboardingWizardProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [showWizard, setShowWizard] = useState(false);
  const [completedSteps, setCompletedSteps] = useState<Set<string>>(new Set());

  useEffect(() => {
    // Check if user has completed onboarding
    const hasCompletedOnboarding = localStorage.getItem('hasCompletedOnboarding');
    const hasOrganizations = localStorage.getItem('organizations');
    
    if (!hasCompletedOnboarding && !hasOrganizations) {
      setShowWizard(true);
    }
  }, []);

  const steps: OnboardingStep[] = [
    {
      id: 'welcome',
      title: 'Welcome to AI Debate System',
      description: 'Let\'s get you started with your first AI-powered debate. This quick setup will have you running debates in minutes!',
      icon: <Brain className="h-8 w-8 text-blue-500" />
    },
    {
      id: 'organization',
      title: 'Create Your Organization',
      description: 'Organizations help you manage different debate contexts and keep your debates organized.',
      icon: <Building2 className="h-8 w-8 text-purple-500" />,
      action: () => {
        onCreateOrganization();
        markStepCompleted('organization');
      }
    },
    {
      id: 'debate',
      title: 'Start Your First Debate',
      description: 'Create an AI debate with multiple participants. Choose from templates or create your own topic.',
      icon: <MessageSquare className="h-8 w-8 text-green-500" />,
      action: () => {
        onCreateDebate();
        markStepCompleted('debate');
      }
    },
    {
      id: 'complete',
      title: 'You\'re All Set!',
      description: 'Congratulations! You\'re ready to explore the full power of AI debates. Happy debating!',
      icon: <Rocket className="h-8 w-8 text-amber-500" />
    }
  ];

  const markStepCompleted = (stepId: string) => {
    setCompletedSteps(prev => new Set(prev).add(stepId));
  };

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      completeOnboarding();
    }
  };

  const handleSkip = () => {
    completeOnboarding();
  };

  const completeOnboarding = () => {
    localStorage.setItem('hasCompletedOnboarding', 'true');
    setShowWizard(false);
    onComplete();
  };

  const progress = ((currentStep + 1) / steps.length) * 100;
  // Validate currentStep index before accessing array
  // eslint-disable-next-line security/detect-object-injection
  const currentStepData = (currentStep >= 0 && currentStep < steps.length) ? steps[currentStep] : steps[0];
  const isActionStep = currentStepData.action !== undefined;
  const isStepCompleted = completedSteps.has(currentStepData.id);

  if (!showWizard) return null;

  return (
    <Dialog open={showWizard} onOpenChange={setShowWizard}>
      <DialogContent className="max-w-2xl">
        <DialogHeader className="pb-4">
          <div className="flex items-center justify-between mb-4">
            <DialogTitle className="text-2xl">Getting Started</DialogTitle>
            <Button variant="ghost" size="sm" onClick={handleSkip}>
              Skip Tour
            </Button>
          </div>
          <Progress value={progress} className="h-2" />
        </DialogHeader>

        <div className="py-8 text-center">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-gradient-to-br from-blue-50 to-purple-50 dark:from-blue-950 dark:to-purple-950 mb-6">
            {currentStepData.icon}
          </div>
          
          <h3 className="text-2xl font-bold mb-4">{currentStepData.title}</h3>
          <DialogDescription className="text-lg mb-8 max-w-md mx-auto">
            {currentStepData.description}
          </DialogDescription>

          {isActionStep && (
            <div className="mb-6">
              {isStepCompleted ? (
                <div className="inline-flex items-center gap-2 text-green-600 dark:text-green-400">
                  <Check className="h-5 w-5" />
                  <span className="font-medium">Completed!</span>
                </div>
              ) : (
                <Button
                  size="lg"
                  onClick={currentStepData.action}
                  className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white"
                >
                  {currentStepData.id === 'organization' ? 'Create Organization' : 'Create Debate'}
                </Button>
              )}
            </div>
          )}

          <div className="flex justify-center gap-4">
            {currentStep > 0 && (
              <Button
                variant="outline"
                onClick={() => setCurrentStep(currentStep - 1)}
              >
                Back
              </Button>
            )}
            <Button
              onClick={handleNext}
              disabled={isActionStep && !isStepCompleted}
              className={!isActionStep || isStepCompleted ? 'bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white' : ''}
            >
              {currentStep === steps.length - 1 ? 'Get Started' : 'Next'}
              <ArrowRight className="h-4 w-4 ml-2" />
            </Button>
          </div>
        </div>

        <div className="flex justify-center gap-2 pb-4">
          {steps.map((_, index) => (
            <div
              key={index}
              className={`h-2 w-2 rounded-full transition-colors ${
                index === currentStep
                  ? 'bg-blue-600'
                  : index < currentStep
                  ? 'bg-blue-300'
                  : 'bg-gray-300'
              }`}
            />
          ))}
        </div>
      </DialogContent>
    </Dialog>
  );
}