import type { Meta, StoryObj } from '@storybook/react';
import { Button } from './Button';
import { Plus, ArrowRight, Download, Trash2 } from 'lucide-react';

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
      options: ['primary', 'secondary', 'ghost', 'danger'],
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
    children: 'Primary Button',
  },
};

export const Secondary: Story = {
  args: {
    variant: 'secondary',
    children: 'Secondary Button',
  },
};

export const Ghost: Story = {
  args: {
    variant: 'ghost',
    children: 'Ghost Button',
  },
};

export const Danger: Story = {
  args: {
    variant: 'danger',
    children: 'Delete',
  },
};

export const Small: Story = {
  args: {
    size: 'sm',
    children: 'Small Button',
  },
};

export const Large: Story = {
  args: {
    size: 'lg',
    children: 'Large Button',
  },
};

export const WithLeftIcon: Story = {
  args: {
    leftIcon: <Plus className="h-4 w-4" />,
    children: 'Add Item',
  },
};

export const WithRightIcon: Story = {
  args: {
    rightIcon: <ArrowRight className="h-4 w-4" />,
    children: 'Next Step',
  },
};

export const Loading: Story = {
  args: {
    loading: true,
    children: 'Loading...',
  },
};

export const Disabled: Story = {
  args: {
    disabled: true,
    children: 'Disabled',
  },
};

export const FullWidth: Story = {
  args: {
    fullWidth: true,
    children: 'Full Width Button',
  },
  parameters: {
    layout: 'padded',
  },
};

export const ButtonGroup: Story = {
  render: () => (
    <div className="flex gap-4">
      <Button variant="secondary">Cancel</Button>
      <Button>Save Changes</Button>
    </div>
  ),
};

export const IconButtons: Story = {
  render: () => (
    <div className="flex gap-4">
      <Button size="sm" variant="secondary" leftIcon={<Download className="h-4 w-4" />}>
        Download
      </Button>
      <Button size="sm" variant="primary" leftIcon={<Plus className="h-4 w-4" />}>
        Create New
      </Button>
      <Button size="sm" variant="danger" leftIcon={<Trash2 className="h-4 w-4" />}>
        Delete
      </Button>
    </div>
  ),
};

export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <div className="flex gap-4">
        <Button variant="primary">Primary</Button>
        <Button variant="secondary">Secondary</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="danger">Danger</Button>
      </div>
      <div className="flex gap-4">
        <Button variant="primary" disabled>Primary</Button>
        <Button variant="secondary" disabled>Secondary</Button>
        <Button variant="ghost" disabled>Ghost</Button>
        <Button variant="danger" disabled>Danger</Button>
      </div>
      <div className="flex gap-4">
        <Button variant="primary" loading>Loading</Button>
        <Button variant="secondary" loading>Loading</Button>
        <Button variant="ghost" loading>Loading</Button>
        <Button variant="danger" loading>Loading</Button>
      </div>
    </div>
  ),
};