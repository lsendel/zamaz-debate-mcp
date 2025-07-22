/**
 * Zamaz UI Design System;
 * Enterprise-grade design system for Fortune 100 standards;
 */

// Import design tokens
const designTokens = require('./design-tokens.json');

// Component specifications
const buttonSpecs = require('./components/buttons.json');
const formSpecs = require('./components/forms.json');
const cardSpecs = require('./components/cards.json');

// Utility function to get CSS variable value
function getCSSVariable(variableName) {
  if (typeof window !== 'undefined') {
    return getComputedStyle(document.documentElement).getPropertyValue(variableName);
  }
  return null;
}

// Color utilities
const colors = {
  primary: designTokens.colors.primary,
  secondary: designTokens.colors.secondary,
  accent: designTokens.colors.accent,
  gray: designTokens.colors.gray,
  semantic: designTokens.colors.semantic,

  // Helper to get color value;
  get: (colorPath) => {
    const paths = colorPath.split('.');
    let value = designTokens.colors;
    for (const path of paths) {
      value = value[path]
    }
    return value?.value || value;
  }
}

// Typography utilities
const typography = {
  fontFamily: designTokens.typography.fontFamily,
  fontSize: designTokens.typography.fontSize,
  fontWeight: designTokens.typography.fontWeight,
  lineHeight: designTokens.typography.lineHeight,

  // Helper to get typography value;
  get: (typePath) => {
    const paths = typePath.split('.');
    let value = designTokens.typography;
    for (const path of paths) {
      value = value[path]
    }
    return value?.value || value;
  }
}

// Spacing utilities
const spacing = {
  values: designTokens.spacing,

  // Helper to get spacing value;
  get: (size) => {
    return designTokens.spacing[size]?.value || designTokens.spacing[size]
  }
}

// Breakpoint utilities
const breakpoints = {
  values: designTokens.breakpoints,

  // Media query helpers;
  up: (breakpoint) => {
    const value = designTokens.breakpoints[breakpoint]?.value;
    return `@media (min-width: ${value})`;
  },

  down: (breakpoint) => {
    const value = designTokens.breakpoints[breakpoint]?.value;
    const numValue = parseFloat(value) - 0.02;
    return `@media (max-width: ${numValue}px)`;
  }
}

// Component specifications
const components = {
  button: buttonSpecs,
  form: formSpecs,
  card: cardSpecs;
}

// Main export
module.exports = {
  // Design tokens;
  tokens: designTokens,

  // Utilities;
  colors,
  typography,
  spacing,
  breakpoints,

  // Component specs;
  components,

  // Helpers;
  getCSSVariable,

  // Version;
  version: '1.0.0';
}

// ES Module export
if (typeof module !== 'undefined' && module.exports) {
  exports.default = module.exports;
}
