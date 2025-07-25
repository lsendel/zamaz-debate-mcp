/**
 * Design System Exports
 * Central export for all design tokens and utilities
 */

export * from './typography';
export * from './colors';
export * from './spacing';

// Combined theme object for Ant Design
import { typography } from './typography';
import { colors } from './colors';
import { spacing, borderRadius } from './spacing';

export const theme = {
  typography,
  colors,
  spacing,
  borderRadius,
};

// Ant Design theme configuration
export const antdTheme = {
  token: {
    // Typography
    fontSize: 16,
    fontFamily: typography.fontFamily.sans,
    fontFamilyCode: typography.fontFamily.mono,
    fontSizeHeading1: parseInt(typography.fontSize['4xl']),
    fontSizeHeading2: parseInt(typography.fontSize['3xl']),
    fontSizeHeading3: parseInt(typography.fontSize['2xl']),
    fontSizeHeading4: parseInt(typography.fontSize.xl),
    fontSizeHeading5: parseInt(typography.fontSize.lg),
    lineHeight: typography.lineHeight.normal,
    lineHeightHeading1: typography.lineHeight.tight,
    lineHeightHeading2: typography.lineHeight.snug,
    fontWeightStrong: typography.fontWeight.semibold,

    // Colors
    colorPrimary: colors.primary[500],
    colorSuccess: colors.semantic.success,
    colorWarning: colors.semantic.warning,
    colorError: colors.semantic.error,
    colorInfo: colors.semantic.info,
    colorText: colors.text.primary,
    colorTextSecondary: colors.text.secondary,
    colorTextTertiary: colors.text.tertiary,
    colorTextDisabled: colors.text.disabled,
    colorBorder: colors.border.default,
    colorBorderSecondary: colors.border.light,
    colorBgContainer: colors.background.primary,
    colorBgElevated: colors.background.elevated,
    colorBgLayout: colors.background.secondary,
    colorBgTextHover: colors.background.tertiary,
    colorLink: colors.text.link,
    colorLinkHover: colors.text.linkHover,

    // Spacing
    marginXXS: spacing[1],
    marginXS: spacing[2],
    marginSM: spacing[3],
    marginMD: spacing[4],
    marginLG: spacing[5],
    marginXL: spacing[6],
    marginXXL: spacing[8],
    paddingXXS: spacing[1],
    paddingXS: spacing[2],
    paddingSM: spacing[3],
    paddingMD: spacing[4],
    paddingLG: spacing[5],
    paddingXL: spacing[6],
    paddingXXL: spacing[8],

    // Border Radius
    borderRadius: borderRadius.md,
    borderRadiusSM: borderRadius.sm,
    borderRadiusLG: borderRadius.lg,

    // Control sizes
    controlHeight: 40,
    controlHeightSM: 32,
    controlHeightLG: 48,

    // Shadows
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.08)',
    boxShadowSecondary: '0 4px 12px rgba(0, 0, 0, 0.12)',
  },

  components: {
    Button: {
      fontSize: parseInt(typography.fontSize.base),
      fontWeight: typography.fontWeight.medium,
      paddingContentHorizontal: spacing[4],
      borderRadius: borderRadius.button,
    },
    Input: {
      fontSize: parseInt(typography.fontSize.base),
      paddingBlock: spacing[2],
      paddingInline: spacing[3],
      borderRadius: borderRadius.input,
    },
    Card: {
      paddingLG: spacing[6],
      borderRadiusLG: borderRadius.card,
    },
    Table: {
      fontSize: parseInt(typography.fontSize.base),
      cellPaddingBlock: spacing[3],
      cellPaddingInline: spacing[4],
      headerBg: colors.background.secondary,
      headerColor: colors.text.primary,
    },
    Modal: {
      borderRadiusLG: borderRadius.modal,
      paddingContentHorizontal: spacing[6],
      paddingContentVertical: spacing[6],
    },
    Menu: {
      itemHeight: 48,
      fontSize: parseInt(typography.fontSize.base),
      itemPaddingInline: spacing[4],
    },
    Form: {
      labelFontSize: parseInt(typography.fontSize.sm),
      verticalLabelPadding: `0 0 ${spacing[2]}px`,
      itemMarginBottom: spacing[5],
    },
    Typography: {
      titleMarginBottom: '0.5em',
      titleMarginTop: 0,
    },
    Select: {
      controlHeight: 40,
      fontSize: parseInt(typography.fontSize.base),
    },
    Badge: {
      fontSize: parseInt(typography.fontSize.xs),
    },
    Tag: {
      fontSize: parseInt(typography.fontSize.sm),
      borderRadius: borderRadius.tag,
    },
    Notification: {
      fontSize: parseInt(typography.fontSize.base),
    },
  },
};
