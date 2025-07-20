import * as React from 'react';
import { clsx } from 'clsx';

export interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  helperText?: string;
  children: React.ReactNode;
  className?: string;
}

const FormField: React.FC<FormFieldProps> = ({
  label,
  required,
  error,
  helperText,
  children,
  className,
}) => {
  return (
    <div className={clsx('mb-6', className)}>
      <label className="mb-2 block text-sm font-medium text-gray-900 dark:text-gray-100">
        {label}
        {required && <span className="ml-1 text-red-500">*</span>}
      </label>
      {children}
      {(error || helperText) && (
        <p className={clsx('mt-1 text-sm', error ? 'text-red-500' : 'text-gray-600 dark:text-gray-400')}>
          {error || helperText}
        </p>
      )}
    </div>
  );
};

export { FormField };