# Technical Migration Guide - Zamaz Design System

## Quick Start Migration Path

### Step 1: Install Dependencies

```bash
# For debate-ui (already using Vite)
cd debate-ui
npm install -D tailwindcss postcss autoprefixer @tailwindcss/forms
npm install @radix-ui/react-select @radix-ui/react-dialog @radix-ui/react-dropdown-menu
npm install clsx class-variance-authority
npm install @tanstack/react-query

# For workflow-editor (using CRA - needs migration to Vite)
cd workflow-editor
npm install -D vite @vitejs/plugin-react
# Then install same dependencies as above
```

### Step 2: Configuration Files

#### Tailwind Configuration
```javascript
// tailwind.config.js
import designTokens from '../ui-specs/design-tokens.json';

export default {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: Object.entries(designTokens.colors).reduce((acc, [key, shades]) => {
        if (typeof shades === 'object' && !shades.value) {
          acc[key] = Object.entries(shades).reduce((shadeAcc, [shade, config]) => {
            shadeAcc[shade] = config.value || config.default?.value;
            return shadeAcc;
          }, {});
        }
        return acc;
      }, {}),
      fontFamily: {
        primary: designTokens.typography.fontFamily.primary.value.split(','),
        display: designTokens.typography.fontFamily.display.value.split(','),
        mono: designTokens.typography.fontFamily.mono.value.split(','),
      },
      fontSize: Object.entries(designTokens.typography.fontSize).reduce((acc, [key, config]) => {
        acc[key] = [config.value, { lineHeight: '1.5' }];
        return acc;
      }, {}),
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
```

#### PostCSS Configuration
```javascript
// postcss.config.js
export default {
  plugins: {
    'tailwindcss/nesting': {},
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

#### Vite Configuration for Workflow-Editor
```javascript
// vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@zamaz/ui': path.resolve(__dirname, '../ui-specs'),
    },
  },
  server: {
    port: 3001,
    proxy: {
      '/api': 'http://localhost:5000',
    },
  },
});
```

---

## Component Migration Examples

### 1. Button Migration (MUI → Zamaz)

#### Before (Material-UI):
```tsx
// debate-ui/src/components/CreateDebateDialog.tsx
import { Button } from '@mui/material';

<Button 
  variant="contained" 
  color="primary"
  startIcon={<AddIcon />}
  disabled={loading}
  onClick={handleSubmit}
  sx={{ mt: 2 }}
>
  Create Debate
</Button>
```

#### After (Zamaz Design System):
```tsx
// debate-ui/src/components/CreateDebateDialog.tsx
import { Button } from '@/components/ui/Button';
import { Plus } from 'lucide-react';

<Button 
  variant="primary"
  leftIcon={<Plus className="w-5 h-5" />}
  disabled={loading}
  loading={loading}
  onClick={handleSubmit}
  className="mt-8"
>
  Create Debate
</Button>
```

#### New Button Component:
```tsx
// shared/components/ui/Button.tsx
import { forwardRef } from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { clsx } from 'clsx';
import { Loader2 } from 'lucide-react';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-md font-medium transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60',
  {
    variants: {
      variant: {
        primary: 'bg-primary-500 text-white hover:bg-primary-600 focus-visible:ring-primary-500 shadow-sm hover:shadow-md',
        secondary: 'bg-white text-gray-900 border border-gray-300 hover:bg-gray-50 focus-visible:ring-gray-500',
        ghost: 'hover:bg-gray-100 text-gray-900 focus-visible:ring-gray-500',
        danger: 'bg-red-500 text-white hover:bg-red-600 focus-visible:ring-red-500',
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
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, leftIcon, rightIcon, loading, children, disabled, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={clsx(buttonVariants({ variant, size }), className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? (
          <Loader2 className="h-4 w-4 animate-spin" />
        ) : leftIcon}
        {children}
        {!loading && rightIcon}
      </button>
    );
  }
);

Button.displayName = 'Button';
```

---

### 2. Form Input Migration

#### Before (Material-UI):
```tsx
// debate-ui/src/components/CreateDebateDialog.tsx
import { TextField } from '@mui/material';

<TextField
  fullWidth
  label="Debate Title"
  value={title}
  onChange={(e) => setTitle(e.target.value)}
  error={!!errors.title}
  helperText={errors.title}
  margin="normal"
  required
/>
```

#### After (Zamaz Design System):
```tsx
// debate-ui/src/components/CreateDebateDialog.tsx
import { Input } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';

<FormField
  label="Debate Title"
  required
  error={errors.title}
>
  <Input
    value={title}
    onChange={(e) => setTitle(e.target.value)}
    placeholder="Enter debate title"
  />
</FormField>
```

#### New Form Components:
```tsx
// shared/components/ui/Input.tsx
import { forwardRef } from 'react';
import { clsx } from 'clsx';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, error, ...props }, ref) => {
    return (
      <input
        ref={ref}
        className={clsx(
          'flex h-11 w-full rounded-md border bg-white px-3 py-2 text-base',
          'placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2',
          'disabled:cursor-not-allowed disabled:opacity-50',
          'transition-all duration-150',
          error
            ? 'border-red-500 focus:ring-red-500'
            : 'border-gray-300 hover:border-gray-400 focus:border-primary-500 focus:ring-primary-500',
          className
        )}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';

// shared/components/ui/FormField.tsx
interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  helperText?: string;
  children: React.ReactNode;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  required,
  error,
  helperText,
  children,
}) => {
  return (
    <div className="mb-6">
      <label className="mb-2 block text-sm font-medium text-gray-900">
        {label}
        {required && <span className="ml-1 text-red-500">*</span>}
      </label>
      {children}
      {(error || helperText) && (
        <p className={clsx('mt-1 text-sm', error ? 'text-red-500' : 'text-gray-600')}>
          {error || helperText}
        </p>
      )}
    </div>
  );
};
```

---

### 3. Navigation Migration

#### Before (Material-UI Drawer):
```tsx
// debate-ui/src/components/Layout.tsx
import { Drawer, List, ListItem, ListItemIcon, ListItemText } from '@mui/material';

<Drawer variant="permanent" sx={{ width: 240 }}>
  <List>
    <ListItem button onClick={() => navigate('/debates')}>
      <ListItemIcon><ForumIcon /></ListItemIcon>
      <ListItemText primary="Debates" />
    </ListItem>
  </List>
</Drawer>
```

#### After (Zamaz Design System):
```tsx
// debate-ui/src/components/Layout.tsx
import { Navigation } from '@/components/ui/Navigation';
import { MessageSquare, BarChart, Settings } from 'lucide-react';

const navItems = [
  { label: 'Debates', href: '/debates', icon: MessageSquare },
  { label: 'Analytics', href: '/analytics', icon: BarChart },
  { label: 'Settings', href: '/settings', icon: Settings },
];

<Navigation items={navItems} />
```

#### New Navigation Component:
```tsx
// shared/components/ui/Navigation.tsx
import { NavLink } from 'react-router-dom';
import { clsx } from 'clsx';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

interface NavigationProps {
  items: NavItem[];
}

export const Navigation: React.FC<NavigationProps> = ({ items }) => {
  return (
    <nav className="w-64 bg-white border-r border-gray-200 h-full">
      <div className="p-4">
        <h1 className="text-2xl font-bold text-primary-500">Zamaz</h1>
      </div>
      <ul className="space-y-1 p-2">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <li key={item.href}>
              <NavLink
                to={item.href}
                className={({ isActive }) =>
                  clsx(
                    'flex items-center gap-3 px-3 py-2 rounded-md transition-colors',
                    'hover:bg-gray-100',
                    isActive
                      ? 'bg-primary-50 text-primary-700 font-medium'
                      : 'text-gray-700'
                  )
                }
              >
                <Icon className="h-5 w-5" />
                <span>{item.label}</span>
              </NavLink>
            </li>
          );
        })}
      </ul>
    </nav>
  );
};
```

---

### 4. Card Migration (Workflow Editor)

#### Before (Inline Styles):
```tsx
// workflow-editor/src/samples/StamfordGeospatialSample.tsx
<div style={{
  backgroundColor: 'white',
  borderRadius: '8px',
  padding: '20px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  marginBottom: '20px'
}}>
  <h3 style={{ marginBottom: '10px' }}>Query Results</h3>
  <p>Found {results.length} locations</p>
</div>
```

#### After (Zamaz Design System):
```tsx
// workflow-editor/src/samples/StamfordGeospatialSample.tsx
import { Card, CardHeader, CardBody } from '@/components/ui/Card';

<Card variant="elevated" className="mb-5">
  <CardHeader>
    <h3 className="text-lg font-semibold">Query Results</h3>
  </CardHeader>
  <CardBody>
    <p className="text-gray-600">Found {results.length} locations</p>
  </CardBody>
</Card>
```

---

## State Management Migration

### Redux to Zustand Migration Pattern

#### Before (Redux Toolkit):
```tsx
// debate-ui/src/store/slices/debateSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

export const fetchDebates = createAsyncThunk(
  'debates/fetch',
  async () => {
    const response = await api.get('/debates');
    return response.data;
  }
);

const debateSlice = createSlice({
  name: 'debates',
  initialState: { list: [], loading: false },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchDebates.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchDebates.fulfilled, (state, action) => {
        state.list = action.payload;
        state.loading = false;
      });
  },
});
```

#### After (Zustand + React Query):
```tsx
// shared/stores/debateStore.ts
import { create } from 'zustand';
import { useQuery } from '@tanstack/react-query';

// Zustand for UI state
export const useDebateUIStore = create((set) => ({
  selectedDebateId: null,
  filterOptions: {},
  setSelectedDebate: (id) => set({ selectedDebateId: id }),
  setFilterOptions: (options) => set({ filterOptions: options }),
}));

// React Query for server state
export const useDebates = () => {
  return useQuery({
    queryKey: ['debates'],
    queryFn: async () => {
      const response = await api.get('/debates');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};
```

---

## Theme Provider Setup

### Creating a Unified Theme Provider

```tsx
// shared/providers/ThemeProvider.tsx
import { createContext, useContext, useEffect, useState } from 'react';

type Theme = 'light' | 'dark';

interface ThemeContextType {
  theme: Theme;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [theme, setTheme] = useState<Theme>(() => {
    const saved = localStorage.getItem('theme');
    return (saved as Theme) || 'light';
  });

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
};
```

---

## Performance Optimization

### Code Splitting Strategy

```tsx
// App.tsx - Lazy load routes
import { lazy, Suspense } from 'react';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const DebatesPage = lazy(() => import('./pages/DebatesPage'));
const AnalyticsPage = lazy(() => import('./pages/AnalyticsPage'));
const WorkflowEditor = lazy(() => import('./pages/WorkflowEditor'));

function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        <Route path="/debates" element={<DebatesPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/workflow" element={<WorkflowEditor />} />
      </Routes>
    </Suspense>
  );
}
```

### Bundle Optimization

```javascript
// vite.config.js
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['@radix-ui/react-select', '@radix-ui/react-dialog'],
          'chart-vendor': ['recharts', 'd3'],
          'map-vendor': ['maplibre-gl'],
        },
      },
    },
  },
});
```

---

## Testing Migration

### Component Testing Setup

```tsx
// Button.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe } from '@axe-core/react';
import { Button } from './Button';

describe('Button Component', () => {
  it('renders with correct variant styles', () => {
    render(<Button variant="primary">Click me</Button>);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('bg-primary-500');
  });

  it('handles loading state', () => {
    render(<Button loading>Loading</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('meets accessibility standards', async () => {
    const { container } = render(<Button>Accessible Button</Button>);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
```

---

## Migration Checklist

### Phase 1: Setup ✓
- [ ] Install Tailwind CSS and dependencies
- [ ] Configure PostCSS
- [ ] Set up design tokens import
- [ ] Create shared component directory
- [ ] Configure path aliases

### Phase 2: Core Components
- [ ] Button component
- [ ] Input/Form components
- [ ] Card component
- [ ] Navigation component
- [ ] Modal/Dialog component

### Phase 3: Layout Components
- [ ] Page layout wrapper
- [ ] Grid system
- [ ] Responsive containers
- [ ] Header/Footer

### Phase 4: Complex Components
- [ ] Data tables
- [ ] Charts (migrate from Recharts)
- [ ] Maps (theme MapLibre)
- [ ] Workflow nodes

### Phase 5: Application Integration
- [ ] Update routing structure
- [ ] Migrate state management
- [ ] Update API integration
- [ ] Theme provider setup

### Phase 6: Testing & Documentation
- [ ] Unit tests for all components
- [ ] E2E tests for critical flows
- [ ] Storybook documentation
- [ ] Migration guide for team

---

## Common Pitfalls & Solutions

### 1. CSS Specificity Issues
```css
/* Problem: MUI styles override Tailwind */
/* Solution: Use important modifier or increase specificity */
.btn-primary {
  @apply !bg-primary-500 hover:!bg-primary-600;
}
```

### 2. TypeScript Types
```typescript
// Create proper type exports
export type { ButtonProps } from './Button';
export type { InputProps } from './Input';

// Use in components
import type { ButtonProps } from '@/components/ui';
```

### 3. Dark Mode Support
```css
/* Ensure all colors support dark mode */
.card {
  @apply bg-white dark:bg-gray-800;
  @apply text-gray-900 dark:text-gray-100;
  @apply border-gray-200 dark:border-gray-700;
}
```

This technical guide provides concrete examples and patterns for migrating from the current implementations to the new design system.