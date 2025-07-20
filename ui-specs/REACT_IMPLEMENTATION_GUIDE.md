# React Implementation Guide - Zamaz Design System

## Recommended Tech Stack for Enterprise React Applications (2025)

### Core Framework Architecture

```typescript
// Recommended stack for Fortune 100 enterprise standards
{
  "ui-framework": "React 18+ with TypeScript",
  "styling": "CSS Modules + Tailwind CSS + CSS-in-JS",
  "components": "Radix UI (headless) + Custom Components",
  "state": "Zustand / Redux Toolkit",
  "forms": "React Hook Form + Zod",
  "routing": "React Router v6",
  "data-fetching": "TanStack Query (React Query)",
  "testing": "Vitest + React Testing Library + Playwright"
}
```

### 1. Styling Solution: Hybrid Approach

#### A. Tailwind CSS Configuration
```javascript
// tailwind.config.js
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#e6f0ff',
          100: '#cce1ff',
          200: '#99c3ff',
          300: '#66a5ff',
          400: '#3387ff',
          500: '#0066ff',
          600: '#0052cc',
          700: '#003d99',
          800: '#002966',
          900: '#001433',
        },
        secondary: {
          50: '#e8f5f0',
          100: '#d1ebe1',
          200: '#a3d7c3',
          300: '#75c3a5',
          400: '#47af87',
          500: '#1a9b69',
          600: '#157c54',
          700: '#105d3f',
          800: '#0a3e2a',
          900: '#051f15',
        },
        accent: {
          50: '#f3e8ff',
          100: '#e7d1ff',
          200: '#cfa3ff',
          300: '#b775ff',
          400: '#9f47ff',
          500: '#8719ff',
          600: '#6c14cc',
          700: '#510f99',
          800: '#360a66',
          900: '#1b0533',
        }
      },
      fontFamily: {
        'primary': ['Inter', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        'display': ['Inter Display', 'Inter', 'sans-serif'],
        'mono': ['JetBrains Mono', 'Consolas', 'monospace'],
      },
      fontSize: {
        'xs': '0.64rem',
        'sm': '0.8rem',
        'base': '1rem',
        'lg': '1.25rem',
        'xl': '1.563rem',
        '2xl': '1.953rem',
        '3xl': '2.441rem',
        '4xl': '3.052rem',
        '5xl': '3.815rem',
      },
      spacing: {
        '0': '0',
        '1': '0.25rem',
        '2': '0.5rem',
        '3': '0.75rem',
        '4': '1rem',
        '5': '1.25rem',
        '6': '1.5rem',
        '8': '2rem',
        '10': '2.5rem',
        '12': '3rem',
        '16': '4rem',
        '20': '5rem',
        '24': '6rem',
      }
    }
  },
  plugins: [
    require('@tailwindcss/forms'),
    require('@tailwindcss/typography'),
    require('@tailwindcss/container-queries'),
  ]
}
```

#### B. CSS-in-JS with Emotion
```typescript
// theme/theme.ts
import { Theme } from '@emotion/react';

export const theme: Theme = {
  colors: {
    primary: {
      50: '#e6f0ff',
      500: '#0066ff',
      600: '#0052cc',
      700: '#003d99',
    },
    // ... rest of color palette
  },
  typography: {
    fontFamily: {
      primary: "'Inter', -apple-system, sans-serif",
      display: "'Inter Display', 'Inter', sans-serif",
      mono: "'JetBrains Mono', monospace",
    }
  },
  spacing: (factor: number) => `${factor * 0.25}rem`,
  transitions: {
    default: 'all 150ms cubic-bezier(0, 0, 0.2, 1)',
  }
};
```

### 2. Component Library Architecture

#### A. Base Component Structure
```typescript
// components/Button/Button.tsx
import { forwardRef } from 'react';
import { clsx } from 'clsx';
import { VariantProps, cva } from 'class-variance-authority';

const buttonVariants = cva(
  // Base styles
  'inline-flex items-center justify-center gap-2 font-medium rounded-md transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:opacity-60 disabled:cursor-not-allowed',
  {
    variants: {
      variant: {
        primary: 'bg-primary-500 text-white hover:bg-primary-600 focus-visible:ring-primary-500',
        secondary: 'bg-white text-gray-900 border border-gray-300 hover:bg-gray-50',
        ghost: 'hover:bg-gray-100 text-gray-900',
      },
      size: {
        sm: 'h-9 px-3 text-sm',
        md: 'h-11 px-6 text-base',
        lg: 'h-13 px-8 text-lg',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, loading, children, disabled, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={clsx(buttonVariants({ variant, size }), className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading && <Spinner className="mr-2" />}
        {children}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

#### B. Using Radix UI for Complex Components
```typescript
// components/Select/Select.tsx
import * as React from 'react';
import * as SelectPrimitive from '@radix-ui/react-select';
import { ChevronDown, Check } from 'lucide-react';
import { clsx } from 'clsx';

export const Select = SelectPrimitive.Root;
export const SelectGroup = SelectPrimitive.Group;
export const SelectValue = SelectPrimitive.Value;

export const SelectTrigger = React.forwardRef<
  React.ElementRef<typeof SelectPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof SelectPrimitive.Trigger>
>(({ className, children, ...props }, ref) => (
  <SelectPrimitive.Trigger
    ref={ref}
    className={clsx(
      'flex h-11 w-full items-center justify-between rounded-md border border-gray-300 bg-white px-3 py-2 text-base',
      'placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2',
      'disabled:cursor-not-allowed disabled:opacity-50',
      className
    )}
    {...props}
  >
    {children}
    <SelectPrimitive.Icon asChild>
      <ChevronDown className="h-4 w-4 opacity-50" />
    </SelectPrimitive.Icon>
  </SelectPrimitive.Trigger>
));

SelectTrigger.displayName = SelectPrimitive.Trigger.displayName;

// ... rest of Select components
```

### 3. Form Management with React Hook Form

```typescript
// components/Form/Form.tsx
import { useForm, FormProvider } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

// Example form schema
const userFormSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  role: z.enum(['admin', 'user', 'guest']),
});

type UserFormData = z.infer<typeof userFormSchema>;

export function UserForm() {
  const methods = useForm<UserFormData>({
    resolver: zodResolver(userFormSchema),
    defaultValues: {
      email: '',
      password: '',
      role: 'user',
    },
  });

  const onSubmit = async (data: UserFormData) => {
    // Handle form submission
    console.log(data);
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onSubmit)} className="space-y-6">
        <FormField
          name="email"
          label="Email Address"
          type="email"
          placeholder="user@example.com"
        />
        <FormField
          name="password"
          label="Password"
          type="password"
        />
        <FormSelect
          name="role"
          label="User Role"
          options={[
            { value: 'admin', label: 'Administrator' },
            { value: 'user', label: 'User' },
            { value: 'guest', label: 'Guest' },
          ]}
        />
        <Button type="submit" loading={methods.formState.isSubmitting}>
          Create User
        </Button>
      </form>
    </FormProvider>
  );
}
```

### 4. State Management with Zustand

```typescript
// stores/uiStore.ts
import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';

interface UIState {
  theme: 'light' | 'dark';
  sidebarOpen: boolean;
  toggleTheme: () => void;
  toggleSidebar: () => void;
}

export const useUIStore = create<UIState>()(
  devtools(
    persist(
      (set) => ({
        theme: 'light',
        sidebarOpen: true,
        toggleTheme: () => set((state) => ({ 
          theme: state.theme === 'light' ? 'dark' : 'light' 
        })),
        toggleSidebar: () => set((state) => ({ 
          sidebarOpen: !state.sidebarOpen 
        })),
      }),
      {
        name: 'ui-storage',
      }
    )
  )
);
```

### 5. Data Fetching with TanStack Query

```typescript
// hooks/useUsers.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

export function useUsers() {
  return useQuery({
    queryKey: ['users'],
    queryFn: () => api.get('/users').then(res => res.data),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useCreateUser() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (newUser: CreateUserDto) => api.post('/users', newUser),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });
}
```

### 6. Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── index.ts
│   ├── Form/
│   ├── Layout/
│   └── ...
├── features/           # Feature-based modules
│   ├── auth/
│   ├── dashboard/
│   └── reports/
├── hooks/              # Custom React hooks
├── lib/                # Utilities and helpers
├── stores/             # Zustand stores
├── styles/             # Global styles
├── theme/              # Theme configuration
└── types/              # TypeScript types
```

### 7. Component Documentation with Storybook

```typescript
// Button.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { Button } from './Button';

const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: { type: 'select' },
      options: ['primary', 'secondary', 'ghost'],
    },
    size: {
      control: { type: 'select' },
      options: ['sm', 'md', 'lg'],
    },
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const Primary: Story = {
  args: {
    variant: 'primary',
    children: 'Button',
  },
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    children: 'Button',
  },
};

export const Loading: Story = {
  args: {
    loading: true,
    children: 'Loading...',
  },
};
```

### 8. Testing Strategy

```typescript
// Button.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from './Button';

describe('Button', () => {
  it('renders with correct text', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByRole('button')).toHaveTextContent('Click me');
  });

  it('handles click events', async () => {
    const handleClick = vi.fn();
    const user = userEvent.setup();
    
    render(<Button onClick={handleClick}>Click me</Button>);
    await user.click(screen.getByRole('button'));
    
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when loading', () => {
    render(<Button loading>Loading</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });
});
```

### 9. Performance Optimization

```typescript
// components/VirtualizedList.tsx
import { useVirtualizer } from '@tanstack/react-virtual';
import { useRef } from 'react';

export function VirtualizedList({ items }: { items: any[] }) {
  const parentRef = useRef<HTMLDivElement>(null);

  const virtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 50,
    overscan: 5,
  });

  return (
    <div ref={parentRef} className="h-[600px] overflow-auto">
      <div
        style={{
          height: `${virtualizer.getTotalSize()}px`,
          width: '100%',
          position: 'relative',
        }}
      >
        {virtualizer.getVirtualItems().map((virtualItem) => (
          <div
            key={virtualItem.key}
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: `${virtualItem.size}px`,
              transform: `translateY(${virtualItem.start}px)`,
            }}
          >
            {items[virtualItem.index]}
          </div>
        ))}
      </div>
    </div>
  );
}
```

### 10. Accessibility Implementation

```typescript
// hooks/useAccessibility.ts
export function useAccessibility() {
  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Skip to main content
      if (e.key === '1' && e.altKey) {
        document.getElementById('main-content')?.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Announce page changes to screen readers
  const announce = (message: string) => {
    const announcement = document.createElement('div');
    announcement.setAttribute('role', 'status');
    announcement.setAttribute('aria-live', 'polite');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    document.body.appendChild(announcement);
    setTimeout(() => document.body.removeChild(announcement), 1000);
  };

  return { announce };
}
```

## Implementation Checklist

- [ ] Set up React with TypeScript and Vite
- [ ] Install and configure Tailwind CSS
- [ ] Set up Emotion for CSS-in-JS
- [ ] Install Radix UI primitives
- [ ] Configure React Hook Form with Zod
- [ ] Set up Zustand for state management
- [ ] Configure TanStack Query
- [ ] Set up Storybook for component documentation
- [ ] Configure testing with Vitest and React Testing Library
- [ ] Implement accessibility hooks and utilities
- [ ] Set up ESLint and Prettier with enterprise rules
- [ ] Configure path aliases for clean imports

## Package.json Dependencies

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "@radix-ui/react-select": "^2.0.0",
    "@radix-ui/react-dialog": "^1.0.5",
    "@radix-ui/react-dropdown-menu": "^2.0.6",
    "@tanstack/react-query": "^5.17.0",
    "@tanstack/react-virtual": "^3.0.1",
    "react-hook-form": "^7.48.0",
    "@hookform/resolvers": "^3.3.4",
    "zod": "^3.22.4",
    "zustand": "^4.4.7",
    "clsx": "^2.1.0",
    "class-variance-authority": "^0.7.0",
    "@emotion/react": "^11.11.3",
    "@emotion/styled": "^11.11.0",
    "lucide-react": "^0.309.0",
    "react-router-dom": "^6.21.1"
  },
  "devDependencies": {
    "@types/react": "^18.2.45",
    "@types/react-dom": "^18.2.18",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.3.3",
    "vite": "^5.0.10",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32",
    "@storybook/react": "^7.6.6",
    "@storybook/react-vite": "^7.6.6",
    "vitest": "^1.1.0",
    "@testing-library/react": "^14.1.2",
    "@testing-library/user-event": "^14.5.2",
    "@testing-library/jest-dom": "^6.1.6",
    "eslint": "^8.56.0",
    "prettier": "^3.1.1"
  }
}
```

This implementation guide provides a modern, scalable foundation for building enterprise React applications that adhere to Fortune 100 standards while leveraging the best practices and tools available in 2025.