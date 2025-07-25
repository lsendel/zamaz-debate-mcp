/**
 * Spacing Design System
 * Consistent spacing scale based on 4px units
 */

export const spacing = {
  0: 0,
  1: 4, // 0.25rem
  2: 8, // 0.5rem
  3: 12, // 0.75rem
  4: 16, // 1rem
  5: 20, // 1.25rem
  6: 24, // 1.5rem
  7: 28, // 1.75rem
  8: 32, // 2rem
  9: 36, // 2.25rem
  10: 40, // 2.5rem
  12: 48, // 3rem
  14: 56, // 3.5rem
  16: 64, // 4rem
  20: 80, // 5rem
  24: 96, // 6rem
  32: 128, // 8rem
  40: 160, // 10rem
  48: 192, // 12rem
  56: 224, // 14rem
  64: 256, // 16rem
} as const;

// Component-specific spacing presets
export const componentSpacing = {
  // Page layout
  pageMargin: spacing[6],
  pagePadding: spacing[6],
  sectionGap: spacing[10],

  // Card spacing
  cardPadding: spacing[6],
  cardGap: spacing[4],
  cardHeaderPadding: `${spacing[4]}px ${spacing[6]}px`,

  // Form spacing
  formItemGap: spacing[5],
  formGroupGap: spacing[8],
  inputPadding: `${spacing[2]}px ${spacing[3]}px`,

  // Table spacing
  tableCellPadding: `${spacing[3]}px ${spacing[4]}px`,
  tableHeaderPadding: `${spacing[3]}px ${spacing[4]}px`,

  // List spacing
  listItemGap: spacing[3],
  listItemPadding: spacing[4],

  // Button spacing
  buttonPadding: {
    small: `${spacing[1]}px ${spacing[3]}px`,
    medium: `${spacing[2]}px ${spacing[4]}px`,
    large: `${spacing[3]}px ${spacing[5]}px`,
  },
  buttonGroupGap: spacing[2],

  // Navigation
  navItemPadding: `${spacing[3]}px ${spacing[4]}px`,
  sidebarWidth: 280,
  headerHeight: 64,

  // Modal spacing
  modalPadding: spacing[6],
  modalHeaderPadding: `${spacing[4]}px ${spacing[6]}px`,

  // Gaps between elements
  inlineGap: spacing[2],
  stackGap: spacing[3],
  sectionHeaderGap: spacing[4],
};

// Border radius scale
export const borderRadius = {
  none: 0,
  sm: 4,
  md: 8,
  lg: 12,
  xl: 16,
  full: 9999,

  // Component specific
  button: 6,
  card: 8,
  modal: 8,
  input: 6,
  badge: 4,
  tag: 4,
} as const;

// Z-index scale for layering
export const zIndex = {
  dropdown: 1050,
  sticky: 1020,
  fixed: 1030,
  modalBackdrop: 1040,
  modal: 1050,
  popover: 1060,
  tooltip: 1070,
  notification: 1080,
} as const;

// Responsive breakpoints (matching Ant Design)
export const breakpoints = {
  xs: 480, // Mobile
  sm: 576, // Small tablet
  md: 768, // Tablet
  lg: 992, // Desktop
  xl: 1200, // Large desktop
  xxl: 1600, // Extra large
} as const;

// Helper functions
export const getSpacing = (...values: (keyof typeof spacing)[]): string => {
  return values.map(v => `${spacing[v]}px`).join(' ');
};

export const getResponsiveSpacing = (
  mobile: keyof typeof spacing,
  tablet: keyof typeof spacing,
  desktop: keyof typeof spacing,
): { xs: number; md: number; lg: number } => {
  return {
    xs: spacing[mobile],
    md: spacing[tablet],
    lg: spacing[desktop],
  };
};
