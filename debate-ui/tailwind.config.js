/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "../packages/zamaz-ui/src/**/*.{js,jsx,ts,tsx}",
  ],
  darkMode: ['class', '[data-theme="dark"]'],
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
        },
        gray: {
          50: '#f8f9fa',
          100: '#f1f3f5',
          200: '#e9ecef',
          300: '#dee2e6',
          400: '#ced4da',
          500: '#adb5bd',
          600: '#6c757d',
          700: '#495057',
          800: '#343a40',
          900: '#212529',
          950: '#0d0f12',
        },
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms')({
      strategy: 'class',
    }),
  ],
}
