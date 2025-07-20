import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { clsx } from 'clsx';

const badgeVariants = cva(
  'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2',
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-gray-100 text-gray-900 hover:bg-gray-200',
        primary:
          'border-transparent bg-primary-100 text-primary-700 hover:bg-primary-200',
        secondary:
          'border-transparent bg-secondary-100 text-secondary-700 hover:bg-secondary-200',
        success:
          'border-transparent bg-green-100 text-green-700 hover:bg-green-200',
        warning:
          'border-transparent bg-yellow-100 text-yellow-700 hover:bg-yellow-200',
        error:
          'border-transparent bg-red-100 text-red-700 hover:bg-red-200',
        danger:
          'border-transparent bg-red-100 text-red-700 hover:bg-red-200',
        outline:
          'text-gray-950 border border-gray-200',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={clsx(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };