# Zamaz Corporate Design System
## Fortune 100 Enterprise UI/UX Standards

### Version 1.0 | 2025

---

## 1. Brand Identity Foundation

### 1.1 Brand Essence
**Vision**: Empowering intelligent decision-making through advanced debate and analysis systems  
**Mission**: To provide enterprise-grade tools that transform complex information into actionable insights  
**Values**: Innovation, Clarity, Trust, Intelligence, Accessibility

### 1.2 Design Principles

#### **Clarity First**
- Information hierarchy is paramount
- Remove visual noise, embrace purposeful whitespace
- Every element must serve a clear function

#### **Intelligence Embedded**
- Design should reflect sophisticated technology
- Use subtle animations to indicate AI/ML processes
- Data visualization should be insightful, not decorative

#### **Enterprise Trust**
- Consistent, predictable interactions
- Professional aesthetic that conveys reliability
- Security and privacy considerations visible in design

#### **Progressive Enhancement**
- Mobile-first responsive design
- Accessibility as a core feature, not an afterthought
- Performance optimization in every decision

---

## 2. Typography System

### 2.1 Font Stack

#### Primary Font: Inter
- **Headlines**: Inter Display (Variable)
- **Body**: Inter (Variable)
- **Monospace**: JetBrains Mono

```css
:root {
  --font-primary: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  --font-display: 'Inter Display', var(--font-primary);
  --font-mono: 'JetBrains Mono', 'Consolas', 'Monaco', monospace;
}
```

### 2.2 Type Scale (1.250 Major Third)

```css
:root {
  /* Base size: 16px */
  --text-xs: 0.64rem;      /* 10.24px */
  --text-sm: 0.8rem;       /* 12.8px */
  --text-base: 1rem;       /* 16px */
  --text-lg: 1.25rem;      /* 20px */
  --text-xl: 1.563rem;     /* 25px */
  --text-2xl: 1.953rem;    /* 31.25px */
  --text-3xl: 2.441rem;    /* 39px */
  --text-4xl: 3.052rem;    /* 48.8px */
  --text-5xl: 3.815rem;    /* 61px */
}
```

### 2.3 Font Weights

```css
:root {
  --font-light: 300;
  --font-regular: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;
}
```

### 2.4 Line Heights

```css
:root {
  --leading-none: 1;
  --leading-tight: 1.25;
  --leading-snug: 1.375;
  --leading-normal: 1.5;
  --leading-relaxed: 1.625;
  --leading-loose: 2;
}
```

---

## 3. Color System

### 3.1 Brand Colors

```css
:root {
  /* Primary - Deep Intelligence Blue */
  --primary-50: #e6f0ff;
  --primary-100: #cce1ff;
  --primary-200: #99c3ff;
  --primary-300: #66a5ff;
  --primary-400: #3387ff;
  --primary-500: #0066ff; /* Main */
  --primary-600: #0052cc;
  --primary-700: #003d99;
  --primary-800: #002966;
  --primary-900: #001433;
  
  /* Secondary - Trust Green */
  --secondary-50: #e8f5f0;
  --secondary-100: #d1ebe1;
  --secondary-200: #a3d7c3;
  --secondary-300: #75c3a5;
  --secondary-400: #47af87;
  --secondary-500: #1a9b69; /* Main */
  --secondary-600: #157c54;
  --secondary-700: #105d3f;
  --secondary-800: #0a3e2a;
  --secondary-900: #051f15;
  
  /* Accent - Innovation Purple */
  --accent-50: #f3e8ff;
  --accent-100: #e7d1ff;
  --accent-200: #cfa3ff;
  --accent-300: #b775ff;
  --accent-400: #9f47ff;
  --accent-500: #8719ff; /* Main */
  --accent-600: #6c14cc;
  --accent-700: #510f99;
  --accent-800: #360a66;
  --accent-900: #1b0533;
}
```

### 3.2 Neutral Colors

```css
:root {
  /* Gray Scale */
  --gray-50: #f8f9fa;
  --gray-100: #f1f3f5;
  --gray-200: #e9ecef;
  --gray-300: #dee2e6;
  --gray-400: #ced4da;
  --gray-500: #adb5bd;
  --gray-600: #6c757d;
  --gray-700: #495057;
  --gray-800: #343a40;
  --gray-900: #212529;
  --gray-950: #0d0f12;
}
```

### 3.3 Semantic Colors

```css
:root {
  /* Status Colors */
  --success-light: #d1fae5;
  --success: #10b981;
  --success-dark: #065f46;
  
  --warning-light: #fef3c7;
  --warning: #f59e0b;
  --warning-dark: #92400e;
  
  --error-light: #fee2e2;
  --error: #ef4444;
  --error-dark: #991b1b;
  
  --info-light: #dbeafe;
  --info: #3b82f6;
  --info-dark: #1e40af;
}
```

### 3.4 Dark Mode Colors

```css
:root[data-theme="dark"] {
  --bg-primary: var(--gray-950);
  --bg-secondary: var(--gray-900);
  --bg-tertiary: var(--gray-800);
  
  --text-primary: var(--gray-50);
  --text-secondary: var(--gray-300);
  --text-tertiary: var(--gray-400);
  
  --border-primary: var(--gray-700);
  --border-secondary: var(--gray-800);
}
```

---

## 4. Spacing & Grid System

### 4.1 Spacing Scale

```css
:root {
  --space-0: 0;
  --space-1: 0.25rem;   /* 4px */
  --space-2: 0.5rem;    /* 8px */
  --space-3: 0.75rem;   /* 12px */
  --space-4: 1rem;      /* 16px */
  --space-5: 1.25rem;   /* 20px */
  --space-6: 1.5rem;    /* 24px */
  --space-8: 2rem;      /* 32px */
  --space-10: 2.5rem;   /* 40px */
  --space-12: 3rem;     /* 48px */
  --space-16: 4rem;     /* 64px */
  --space-20: 5rem;     /* 80px */
  --space-24: 6rem;     /* 96px */
}
```

### 4.2 Container System

```css
:root {
  --container-xs: 475px;
  --container-sm: 640px;
  --container-md: 768px;
  --container-lg: 1024px;
  --container-xl: 1280px;
  --container-2xl: 1536px;
  --container-fluid: 100%;
}

.container {
  width: 100%;
  margin-left: auto;
  margin-right: auto;
  padding-left: var(--space-4);
  padding-right: var(--space-4);
}

@media (min-width: 640px) {
  .container { max-width: var(--container-sm); }
}

@media (min-width: 768px) {
  .container { 
    max-width: var(--container-md);
    padding-left: var(--space-6);
    padding-right: var(--space-6);
  }
}

@media (min-width: 1024px) {
  .container { max-width: var(--container-lg); }
}

@media (min-width: 1280px) {
  .container { max-width: var(--container-xl); }
}
```

### 4.3 Grid System

```css
.grid {
  display: grid;
  gap: var(--space-4);
}

.grid-cols-12 {
  grid-template-columns: repeat(12, minmax(0, 1fr));
}

@media (min-width: 768px) {
  .md\:grid-cols-12 {
    grid-template-columns: repeat(12, minmax(0, 1fr));
  }
}
```

---

## 5. Component Specifications

### 5.1 Buttons

#### Primary Button
```css
.btn-primary {
  /* Sizing */
  padding: var(--space-3) var(--space-6);
  min-height: 44px; /* Touch target */
  
  /* Typography */
  font-family: var(--font-primary);
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  line-height: var(--leading-tight);
  
  /* Colors */
  background-color: var(--primary-500);
  color: white;
  
  /* Borders */
  border: none;
  border-radius: 8px;
  
  /* Effects */
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);
  
  /* States */
  cursor: pointer;
}

.btn-primary:hover {
  background-color: var(--primary-600);
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  transform: translateY(-1px);
}

.btn-primary:active {
  background-color: var(--primary-700);
  transform: translateY(0);
}

.btn-primary:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
}
```

### 5.2 Input Fields

```css
.input {
  /* Sizing */
  width: 100%;
  padding: var(--space-3) var(--space-4);
  min-height: 44px;
  
  /* Typography */
  font-family: var(--font-primary);
  font-size: var(--text-base);
  line-height: var(--leading-normal);
  
  /* Colors */
  background-color: white;
  color: var(--gray-900);
  
  /* Borders */
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  
  /* Effects */
  transition: all 150ms ease;
}

.input:hover {
  border-color: var(--gray-400);
}

.input:focus {
  outline: none;
  border-color: var(--primary-500);
  box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.1);
}
```

### 5.3 Cards

```css
.card {
  background-color: white;
  border: 1px solid var(--gray-200);
  border-radius: 12px;
  padding: var(--space-6);
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.05);
  transition: all 200ms ease;
}

.card:hover {
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  transform: translateY(-2px);
}
```

---

## 6. Motion & Animation

### 6.1 Timing Functions

```css
:root {
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);
}
```

### 6.2 Duration Scale

```css
:root {
  --duration-75: 75ms;
  --duration-100: 100ms;
  --duration-150: 150ms;
  --duration-200: 200ms;
  --duration-300: 300ms;
  --duration-500: 500ms;
  --duration-700: 700ms;
  --duration-1000: 1000ms;
}
```

### 6.3 Animation Principles

1. **Purposeful**: Every animation must serve a functional purpose
2. **Performance**: Use transform and opacity for 60fps animations
3. **Subtle**: Enterprise applications require refined, not flashy animations
4. **Consistent**: Same easing and duration for similar interactions

---

## 7. Accessibility Standards

### 7.1 WCAG 2.1 Level AA Compliance

- **Color Contrast**: Minimum 4.5:1 for normal text, 3:1 for large text
- **Focus Indicators**: Visible focus states for all interactive elements
- **Keyboard Navigation**: Full keyboard accessibility
- **Screen Reader Support**: Semantic HTML and ARIA labels

### 7.2 Implementation

```css
/* Focus Visible */
:focus-visible {
  outline: 2px solid var(--primary-500);
  outline-offset: 2px;
}

/* Skip to Content */
.skip-to-content {
  position: absolute;
  left: -9999px;
  z-index: 999;
}

.skip-to-content:focus {
  left: 50%;
  transform: translateX(-50%);
  top: var(--space-4);
}
```

---

## 8. Responsive Design

### 8.1 Breakpoints

```css
:root {
  --screen-xs: 475px;
  --screen-sm: 640px;
  --screen-md: 768px;
  --screen-lg: 1024px;
  --screen-xl: 1280px;
  --screen-2xl: 1536px;
}
```

### 8.2 Mobile-First Approach

```css
/* Base styles for mobile */
.element {
  font-size: var(--text-base);
  padding: var(--space-3);
}

/* Tablet and up */
@media (min-width: 768px) {
  .element {
    font-size: var(--text-lg);
    padding: var(--space-4);
  }
}

/* Desktop and up */
@media (min-width: 1024px) {
  .element {
    font-size: var(--text-xl);
    padding: var(--space-6);
  }
}
```

---

## 9. Icons & Imagery

### 9.1 Icon System

- **Primary Icon Set**: Phosphor Icons (Regular weight)
- **Icon Sizes**: 16px, 20px, 24px, 32px, 48px
- **Stroke Width**: 1.5px for consistency

### 9.2 Image Guidelines

- **Aspect Ratios**: 16:9, 4:3, 1:1, 9:16
- **Loading**: Progressive enhancement with blur-up technique
- **Formats**: WebP with JPEG fallback, AVIF for next-gen

---

## 10. Data Visualization

### 10.1 Chart Color Palette

```css
:root {
  --chart-1: #0066ff;
  --chart-2: #1a9b69;
  --chart-3: #8719ff;
  --chart-4: #ff6b6b;
  --chart-5: #f59e0b;
  --chart-6: #10b981;
  --chart-7: #6366f1;
  --chart-8: #ec4899;
}
```

### 10.2 Visualization Principles

1. **Data-Ink Ratio**: Maximize data, minimize decoration
2. **Color Accessibility**: Consider colorblind users
3. **Interactive Tooltips**: Provide context on hover/tap
4. **Responsive Charts**: Adapt to viewport changes

---

## 11. Implementation Guidelines

### 11.1 CSS Architecture

- **Methodology**: CUBE CSS (Composition, Utility, Block, Exception)
- **Naming Convention**: BEM for components, functional utilities
- **Organization**: Layered approach (Settings, Tools, Generic, Elements, Objects, Components, Utilities)

### 11.2 Component Development

1. Start with semantic HTML
2. Layer on base styles
3. Add interactive states
4. Ensure accessibility
5. Test across devices
6. Document usage

### 11.3 Performance Budget

- **First Contentful Paint**: < 1.2s
- **Time to Interactive**: < 3.5s
- **Cumulative Layout Shift**: < 0.1
- **Total CSS Size**: < 50KB (minified + gzipped)

---

## 12. Design Tokens

### 12.1 Token Structure

```json
{
  "color": {
    "primary": {
      "value": "#0066ff",
      "type": "color",
      "description": "Main brand color"
    }
  },
  "spacing": {
    "base": {
      "value": "16px",
      "type": "dimension",
      "description": "Base spacing unit"
    }
  }
}
```

### 12.2 Platform Exports

- **CSS**: Custom properties
- **JavaScript/TypeScript**: ES modules
- **iOS**: Swift extensions
- **Android**: XML resources

---

## 13. Voice & Tone

### 13.1 Content Principles

- **Clear**: Use plain language, avoid jargon
- **Concise**: Get to the point quickly
- **Confident**: Professional without being cold
- **Helpful**: Guide users to success

### 13.2 Error Messages

```
Structure: [What happened] + [Why] + [How to fix]

Example:
"Unable to save your changes. 
The connection was interrupted. 
Please check your internet connection and try again."
```

---

## 14. Future Considerations (2025 Trends)

### 14.1 AI-Driven Interfaces
- Predictive UI elements
- Contextual assistance
- Personalized layouts

### 14.2 Advanced Interactions
- Voice UI integration
- Gesture controls
- Haptic feedback

### 14.3 Sustainability
- Dark mode by default
- Reduced animation options
- Efficient asset loading

---

## Appendix A: Quick Reference

### CSS Custom Properties Setup

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300..700&display=swap');

:root {
  /* Typography */
  --font-primary: 'Inter', system-ui, sans-serif;
  --text-base: 1rem;
  --leading-normal: 1.5;
  
  /* Colors */
  --primary-500: #0066ff;
  --gray-900: #212529;
  --bg-primary: #ffffff;
  
  /* Spacing */
  --space-4: 1rem;
  
  /* Borders */
  --radius-base: 8px;
  
  /* Shadows */
  --shadow-sm: 0 1px 3px 0 rgb(0 0 0 / 0.1);
  
  /* Animation */
  --duration-base: 150ms;
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
}
```

### Component Example

```html
<button class="btn btn-primary">
  <svg class="icon" width="20" height="20"><!-- icon --></svg>
  <span>Get Started</span>
</button>
```

---

This design system provides a comprehensive foundation for building consistent, accessible, and professional enterprise applications that meet Fortune 100 standards while embracing 2025 design trends.