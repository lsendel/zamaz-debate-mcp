import React from 'react';
import { Typography } from 'antd';
import { typographyPresets, colors } from '../../styles';

const { Title, Text, Paragraph } = Typography;

interface TypographyProps {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

// Page Title Component (H1)
export const PageTitle: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Title
    level={1}
    className={className}
    style={{
      ...typographyPresets.pageTitle,
      color: colors.text.primary,
      ...style,
    }}
  >
    {children}
  </Title>
);

// Section Title Component (H2)
export const SectionTitle: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Title
    level={2}
    className={className}
    style={{
      ...typographyPresets.sectionTitle,
      color: colors.text.primary,
      ...style,
    }}
  >
    {children}
  </Title>
);

// Card Title Component (H3)
export const CardTitle: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Title
    level={3}
    className={className}
    style={{
      ...typographyPresets.cardTitle,
      color: colors.text.primary,
      ...style,
    }}
  >
    {children}
  </Title>
);

// Subsection Title Component (H4)
export const SubsectionTitle: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Title
    level={4}
    className={className}
    style={{
      fontSize: typographyPresets.body.fontSize,
      fontWeight: typographyPresets.cardTitle.fontWeight,
      lineHeight: typographyPresets.body.lineHeight,
      marginBottom: 8,
      color: colors.text.primary,
      ...style,
    }}
  >
    {children}
  </Title>
);

// Body Text Component
interface BodyTextProps extends TypographyProps {
  secondary?: boolean;
  tertiary?: boolean;
  size?: 'small' | 'default' | 'large';
}

export const BodyText: React.FC<BodyTextProps> = ({
  children,
  secondary,
  tertiary,
  size = 'default',
  className,
  style,
}) => {
  const preset =
    size === 'small'
      ? typographyPresets.bodySmall
      : size === 'large'
        ? typographyPresets.bodyLarge
        : typographyPresets.body;

  const textColor = tertiary
    ? colors.text.tertiary
    : secondary
      ? colors.text.secondary
      : colors.text.primary;

  return (
    <Text
      className={className}
      style={{
        ...preset,
        color: textColor,
        ...style,
      }}
    >
      {children}
    </Text>
  );
};

// Body Paragraph Component
export const BodyParagraph: React.FC<BodyTextProps> = ({
  children,
  secondary,
  tertiary,
  size = 'default',
  className,
  style,
}) => {
  const preset =
    size === 'small'
      ? typographyPresets.bodySmall
      : size === 'large'
        ? typographyPresets.bodyLarge
        : typographyPresets.body;

  const textColor = tertiary
    ? colors.text.tertiary
    : secondary
      ? colors.text.secondary
      : colors.text.primary;

  return (
    <Paragraph
      className={className}
      style={{
        ...preset,
        color: textColor,
        marginBottom: 16,
        ...style,
      }}
    >
      {children}
    </Paragraph>
  );
};

// Caption Component
export const Caption: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Text
    className={className}
    style={{
      ...typographyPresets.caption,
      color: colors.text.tertiary,
      ...style,
    }}
  >
    {children}
  </Text>
);

// Label Component
export const Label: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Text
    className={className}
    style={{
      ...typographyPresets.label,
      color: colors.text.secondary,
      display: 'block',
      marginBottom: 4,
      ...style,
    }}
  >
    {children}
  </Text>
);

// Link Component
interface LinkProps extends TypographyProps {
  href?: string;
  onClick?: () => void;
}

export const Link: React.FC<LinkProps> = ({ children, href, onClick, className, style }) => (
  <Typography.Link
    href={href}
    onClick={onClick}
    className={className}
    style={{
      fontSize: typographyPresets.body.fontSize,
      color: colors.text.link,
      ...style,
    }}
  >
    {children}
  </Typography.Link>
);

// Error Text Component
export const ErrorText: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Text
    className={className}
    style={{
      ...typographyPresets.bodySmall,
      color: colors.semantic.error,
      ...style,
    }}
  >
    {children}
  </Text>
);

// Success Text Component
export const SuccessText: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Text
    className={className}
    style={{
      ...typographyPresets.bodySmall,
      color: colors.semantic.success,
      ...style,
    }}
  >
    {children}
  </Text>
);

// Code/Monospace Component
export const Code: React.FC<TypographyProps> = ({ children, className, style }) => (
  <Text
    code
    className={className}
    style={{
      fontSize: typographyPresets.bodySmall.fontSize,
      ...style,
    }}
  >
    {children}
  </Text>
);
