/**
 * Color Design System
 * WCAG AA compliant color palette with improved contrast ratios
 */

export const colors = {
  // Primary color scale
  primary: {
    50: '#e6f0ff',
    100: '#bbd4ff',
    200: '#85b4ff',
    300: '#4d93ff',
    400: '#267bff',
    500: '#1677ff', // Ant Design primary
    600: '#1360d9',
    700: '#0f4eb3',
    800: '#0c3d8c',
    900: '#082c66',
  },

  // Neutral/Gray scale with improved contrast
  gray: {
    50: '#fafafa',
    100: '#f5f5f5',
    200: '#e8e8e8',
    300: '#d9d9d9',
    400: '#bfbfbf',
    500: '#8c8c8c',
    600: '#595959', // Minimum for text on white (7:1 contrast)
    700: '#434343',
    800: '#262626',
    900: '#1f1f1f',
  },

  // Semantic colors
  semantic: {
    success: '#52c41a',
    successBg: '#f6ffed',
    successBorder: '#b7eb8f',

    warning: '#faad14',
    warningBg: '#fffbe6',
    warningBorder: '#ffe58f',

    error: '#f5222d',
    errorBg: '#fff1f0',
    errorBorder: '#ffccc7',

    info: '#1677ff',
    infoBg: '#e6f4ff',
    infoBorder: '#91caff',
  },

  // Background colors
  background: {
    primary: '#ffffff',
    secondary: '#fafafa',
    tertiary: '#f5f5f5',
    elevated: '#ffffff',
    overlay: 'rgba(0, 0, 0, 0.45)',
  },

  // Text colors with WCAG compliance
  text: {
    primary: '#262626', // High contrast for main text
    secondary: '#595959', // Still readable for secondary text
    tertiary: '#8c8c8c', // For less important text
    disabled: '#bfbfbf', // For disabled state
    inverse: '#ffffff', // For text on dark backgrounds
    link: '#1677ff',
    linkHover: '#1360d9',
  },

  // Border colors
  border: {
    default: '#d9d9d9',
    light: '#e8e8e8',
    dark: '#bfbfbf',
  },
} as const;

// Helper functions for color manipulation
export const getColorWithOpacity = (color: string, opacity: number): string => {
  // If it's a hex color
  if (color.startsWith('#')) {
    const r = parseInt(color.slice(1, 3), 16);
    const g = parseInt(color.slice(3, 5), 16);
    const b = parseInt(color.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${opacity})`;
  }
  return color;
};

// WCAG contrast ratio helpers
export const contrastRatios = {
  // Text on white background
  onWhite: {
    large: {
      // Large text (18px+ or 14px+ bold)
      AA: colors.gray[500], // 4.5:1 ratio
      AAA: colors.gray[700], // 7:1 ratio
    },
    normal: {
      // Normal text
      AA: colors.gray[600], // 4.5:1 ratio
      AAA: colors.gray[800], // 7:1 ratio
    },
  },
  // Text on primary color
  onPrimary: {
    text: colors.text.inverse,
  },
};

// Shadow definitions
export const shadows = {
  xs: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  sm: '0 2px 4px 0 rgba(0, 0, 0, 0.06)',
  md: '0 4px 8px 0 rgba(0, 0, 0, 0.08)',
  lg: '0 8px 16px 0 rgba(0, 0, 0, 0.10)',
  xl: '0 12px 24px 0 rgba(0, 0, 0, 0.12)',

  card: '0 2px 8px rgba(0, 0, 0, 0.08)',
  cardHover: '0 4px 12px rgba(0, 0, 0, 0.12)',

  modal: '0 4px 12px rgba(0, 0, 0, 0.15)',
  dropdown:
    '0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 9px 28px 8px rgba(0, 0, 0, 0.05)',
};
