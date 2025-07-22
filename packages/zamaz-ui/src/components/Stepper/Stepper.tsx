import React from 'react';
import { cn } from '../../utils/cn';
import { Check } from 'lucide-react';

export interface StepperProps {
  steps: {
    label: string;
    description?: string;
    status?: 'completed' | 'active' | 'pending';
    icon?: React.ReactNode;
  }[];
  orientation?: 'horizontal' | 'vertical';
  activeStep?: number;
  className?: string;
}

export const Stepper: React.FC<StepperProps> = ({
  steps,
  orientation = 'vertical',
  activeStep = 0,
  className,
}) => {
  const isHorizontal = orientation === 'horizontal';

  return (
    <div
      className={cn(
        'relative',
        isHorizontal ? 'flex items-center' : 'space-y-6',
        className
      )}
    >
      {steps.map((step, index) => {
        const isCompleted = step.status === 'completed' || index < activeStep;
        const isActive = step.status === 'active' || index === activeStep;
//         const isPending = step.status === 'pending' || index > activeStep; // SonarCloud: removed useless assignment

        return (
          <div
            key={index}
            className={cn(
              'relative',
              isHorizontal ? 'flex items-center flex-1' : 'flex'
            )}
          >
            {/* Connector Line */}
            {index < steps.length - 1 && (
              <div
                className={cn(
                  'absolute',
                  isHorizontal
                    ? 'top-5 left-10 right-0 h-0.5'
                    : 'top-10 left-5 bottom-0 w-0.5',
                  isCompleted ? 'bg-primary-500' : 'bg-gray-200'
                )}
              />
            )}

            {/* Step */}
            <div className={cn('relative z-10', isHorizontal ? '' : 'flex')}>
              {/* Step Icon */}
              <div
                className={cn(
                  'w-10 h-10 rounded-full flex items-center justify-center font-medium transition-all',
                  isCompleted
                    ? 'bg-primary-500 text-white'
                    : isActive
                    ? 'bg-primary-100 text-primary-700 border-2 border-primary-500'
                    : 'bg-gray-100 text-gray-400 border-2 border-gray-200'
                )}
              >
                {step.icon ? (
                  step.icon
                ) : isCompleted ? (
                  <Check className="w-5 h-5" />
                ) : (
                  <span>{index + 1}</span>
                )}
              </div>

              {/* Content */}
              <div
                className={cn(
                  isHorizontal
                    ? 'absolute top-12 left-1/2 -translate-x-1/2 text-center'
                    : 'ml-4 flex-1'
                )}
              >
                <div
                  className={cn(
                    'font-medium',
                    isActive ? 'text-gray-900' : 'text-gray-500'
                  )}
                >
                  {step.label}
                </div>
                {step.description && (
                  <div className="text-sm text-gray-500 mt-1">
                    {step.description}
                  </div>
                )}
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export interface StepProps {
  children: React.ReactNode;
  completed?: boolean;
  active?: boolean;
}

export const Step: React.FC<StepProps> = ({ children, completed, active }) => {
  return <div>{children}</div>;
};

export interface StepLabelProps {
  children: React.ReactNode;
  optional?: React.ReactNode;
  StepIconComponent?: React.ComponentType<{ completed?: boolean; active?: boolean }>;
}

export const StepLabel: React.FC<StepLabelProps> = ({ 
  children, 
  optional,
  StepIconComponent 
}) => {
  return (
    <div>
      <div className="font-medium">{children}</div>
      {optional && <div className="text-sm text-gray-500">{optional}</div>}
    </div>
  );
};

export interface StepContentProps {
  children: React.ReactNode;
}

export const StepContent: React.FC<StepContentProps> = ({ children }) => {
  return <div className="ml-10 pb-6">{children}</div>;
};