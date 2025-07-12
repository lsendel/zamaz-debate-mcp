# CSS Troubleshooting Guide

## Common CSS Issues & Fixes

### 1. Z-Index Conflicts

**Problem:** Dropdowns appearing behind modals, tooltips hidden by headers

**Solution:**
```css
/* Establish z-index hierarchy */
:root {
  --z-base: 0;
  --z-dropdown: 10;
  --z-sticky: 20;
  --z-fixed: 30;
  --z-modal-backdrop: 40;
  --z-modal: 50;
  --z-notification: 60;
  --z-tooltip: 70;
}

/* Apply consistently */
.header { z-index: var(--z-sticky); }
.dropdown-menu { z-index: var(--z-dropdown); }
.modal-overlay { z-index: var(--z-modal-backdrop); }
.modal-content { z-index: var(--z-modal); }
.tooltip { z-index: var(--z-tooltip); }
```

### 2. Flexbox Layout Issues

**Problem:** Items not aligning properly, unexpected wrapping

**Debugging Checklist:**
```css
/* Add visual debugging */
.debug * {
  outline: 1px solid red;
}

/* Common fixes */
.flex-container {
  display: flex;
  flex-wrap: wrap; /* or nowrap */
  align-items: flex-start; /* not stretch */
  gap: 1rem; /* instead of margins */
}

.flex-item {
  flex: 0 1 auto; /* don't grow, can shrink, auto basis */
  min-width: 0; /* prevent overflow */
}
```

### 3. Overflow & Scrolling

**Problem:** Content cut off, horizontal scroll on mobile

**Solutions:**
```css
/* Prevent horizontal scroll */
html, body {
  overflow-x: hidden;
  max-width: 100%;
}

/* Safe text overflow */
.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Scrollable containers */
.scrollable {
  overflow-y: auto;
  max-height: 100vh;
  -webkit-overflow-scrolling: touch; /* smooth iOS scrolling */
  
  /* Firefox scrollbar styling */
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
}

/* Chrome/Safari scrollbar styling */
.scrollable::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.scrollable::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}
```

### 4. CSS Grid Issues

**Problem:** Grid items not placing correctly

**Debugging:**
```css
/* Visual grid debugging */
.grid-debug {
  background-image: 
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 1rem,
      rgba(255, 0, 0, 0.1) 1rem,
      rgba(255, 0, 0, 0.1) calc(1rem + 1px)
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 1rem,
      rgba(255, 0, 0, 0.1) 1rem,
      rgba(255, 0, 0, 0.1) calc(1rem + 1px)
    );
}

/* Common grid patterns */
.auto-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.responsive-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 1rem;
}

.grid-item-span-6 {
  grid-column: span 6;
}

@media (max-width: 768px) {
  .grid-item-span-6 {
    grid-column: span 12;
  }
}
```

### 5. Dark Mode Color Issues

**Problem:** Poor contrast, invisible borders in dark mode

**Solution:**
```css
/* Use CSS variables for theming */
:root {
  --bg-primary: #ffffff;
  --bg-secondary: #f5f5f5;
  --text-primary: #1a1a1a;
  --text-secondary: #666666;
  --border-color: #e5e5e5;
}

.dark {
  --bg-primary: #1a1a1a;
  --bg-secondary: #2d2d2d;
  --text-primary: #ffffff;
  --text-secondary: #a0a0a0;
  --border-color: #404040;
}

/* Use semantic color variables */
.card {
  background: var(--bg-primary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

/* Ensure sufficient contrast */
.dark .subtle-border {
  border-color: rgba(255, 255, 255, 0.1);
}
```

### 6. Animation Performance

**Problem:** Janky animations, layout thrashing

**Optimizations:**
```css
/* Use transform instead of position */
.slide-in {
  transform: translateX(-100%);
  transition: transform 0.3s ease-out;
}

.slide-in.active {
  transform: translateX(0);
}

/* Optimize expensive properties */
.smooth-animation {
  will-change: transform; /* hint to browser */
  transform: translateZ(0); /* force GPU acceleration */
}

/* Reduce paint areas */
.animated-element {
  contain: layout style paint;
}

/* Disable animations for reduced motion */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 7. Mobile-Specific Issues

**Problem:** Tiny text, unclickable buttons, viewport issues

**Solutions:**
```css
/* Ensure readable text */
html {
  -webkit-text-size-adjust: 100%;
  font-size: 16px; /* prevent zoom on iOS */
}

/* Minimum touch target size */
button, 
a,
[role="button"] {
  min-height: 44px;
  min-width: 44px;
  position: relative;
}

/* Fix viewport on mobile */
.mobile-full-height {
  height: 100vh;
  height: 100dvh; /* dynamic viewport height */
}

/* Safe area insets for notched devices */
.safe-padding {
  padding: env(safe-area-inset-top) 
           env(safe-area-inset-right) 
           env(safe-area-inset-bottom) 
           env(safe-area-inset-left);
}
```

### 8. Focus Styles

**Problem:** No visible focus indicator, poor accessibility

**Solution:**
```css
/* Custom focus styles */
*:focus {
  outline: none;
}

*:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* Different focus for different elements */
button:focus-visible {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.5);
}

input:focus-visible,
textarea:focus-visible {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* Skip to main content link */
.skip-link {
  position: absolute;
  left: -10000px;
  top: auto;
  width: 1px;
  height: 1px;
  overflow: hidden;
}

.skip-link:focus {
  position: fixed;
  top: 1rem;
  left: 1rem;
  width: auto;
  height: auto;
  z-index: 999;
}
```

## CSS Debugging Tools & Techniques

### Browser DevTools Tips

1. **Computed Styles Panel**
   - Shows final computed values
   - Indicates which rules are overridden
   - Shows inheritance chain

2. **Layout Debugging**
   ```javascript
   // Paste in console to highlight all elements
   document.querySelectorAll('*').forEach(el => {
     el.style.outline = '1px solid red';
   });
   ```

3. **Check Specificity**
   ```javascript
   // Get specificity of selectors
   function getSpecificity(selector) {
     const a = (selector.match(/#/g) || []).length;
     const b = (selector.match(/\./g) || []).length;
     const c = (selector.match(/[a-zA-Z]+/g) || []).length;
     return [a, b, c];
   }
   ```

### Performance Profiling

1. **Check for Layout Thrashing**
   ```javascript
   // Bad: Forces multiple reflows
   elements.forEach(el => {
     el.style.left = el.offsetLeft + 10 + 'px';
   });
   
   // Good: Batch reads then writes
   const positions = elements.map(el => el.offsetLeft);
   elements.forEach((el, i) => {
     el.style.left = positions[i] + 10 + 'px';
   });
   ```

2. **Monitor Paint Events**
   - Chrome DevTools > Rendering > Paint flashing
   - Shows which areas are being repainted

### CSS Validation Checklist

- [ ] No horizontal scroll on mobile
- [ ] All interactive elements have focus states
- [ ] Text remains readable in all color modes
- [ ] Animations respect prefers-reduced-motion
- [ ] Z-index hierarchy is documented
- [ ] No layout shift during loading
- [ ] Touch targets are at least 44x44px
- [ ] Scrollable areas have visible scrollbars
- [ ] Print styles are defined if needed
- [ ] Critical CSS is inlined or prioritized

## Quick Fixes Reference

```css
/* Reset problematic defaults */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

/* Prevent common issues */
img, video {
  max-width: 100%;
  height: auto;
}

/* Fix button styling inconsistencies */
button {
  font: inherit;
  color: inherit;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
}

/* Ensure form elements inherit font */
input, textarea, select {
  font: inherit;
}

/* Prevent text selection on UI elements */
.no-select {
  user-select: none;
  -webkit-user-select: none;
}
```