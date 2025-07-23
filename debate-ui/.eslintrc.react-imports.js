module.exports = {
  extends: ['./../.linting/frontend/.eslintrc.js'],
  rules: {
    // React import validation rules
    'import/no-duplicates': ['error', { 'prefer-inline': true }],
    'import/first': 'error',
    'import/newline-after-import': 'error',
    'import/no-webpack-loader-syntax': 'error',
    
    // Ensure React is imported correctly
    'react/jsx-uses-react': 'error',
    'react/jsx-uses-vars': 'error',
    
    // Prevent common React import mistakes
    'no-restricted-imports': [
      'error',
      {
        paths: [
          {
            name: 'react',
            importNames: ['default'],
            message: 'Import React as: import React from "react" or use named imports',
          },
        ],
        patterns: [
          {
            group: ['react/index', 'react/index.js'],
            message: 'Import from "react" directly, not from index files',
          },
          {
            group: ['**/node_modules/react/**', '!react', '!react-dom', '!react-router-dom', '!react-redux', '!react-hook-form'],
            message: 'Do not import React internals',
          },
        ],
      },
    ],
    
    // Ensure consistent import style
    'import/consistent-type-specifier-style': ['error', 'prefer-inline'],
    'import/no-anonymous-default-export': 'error',
    
    // Check for multiple React versions
    'import/no-extraneous-dependencies': [
      'error',
      {
        devDependencies: ['**/*.test.tsx', '**/*.test.ts', '**/*.spec.tsx', '**/*.spec.ts', 'vite.config.js'],
        peerDependencies: false,
      },
    ],
    
    // Warn about potential React version conflicts
    'import/no-unresolved': [
      'error',
      {
        caseSensitive: true,
        commonjs: true,
        ignore: ['^@/', '^~/', '\\.svg$'],
      },
    ],
    
    // Custom pattern checks for common issues from CLAUDE.md
    'no-restricted-syntax': [
      'error',
      {
        selector: 'ImportDeclaration[source.value=/^@zamaz\\/ui/]',
        message: 'Do not use @zamaz/ui - use Ant Design (antd) instead. See CLAUDE.md for migration guide.',
      },
      {
        selector: 'ImportDeclaration[source.value=/^lucide-react/]',
        message: 'Use @ant-design/icons instead of lucide-react for consistency.',
      },
    ],
  },
  overrides: [
    {
      files: ['*.tsx', '*.jsx'],
      rules: {
        // Ensure React is in scope for JSX files (for older React versions)
        'react/react-in-jsx-scope': 'off', // React 17+ doesn't need this
      },
    },
  ],
};