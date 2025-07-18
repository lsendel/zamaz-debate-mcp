// Service-specific ESLint overrides for Debate UI
module.exports = {
  // Extend the global configuration
  extends: ['../../frontend/.eslintrc.js'],

  // Debate UI specific settings
  settings: {
    react: {
      version: 'detect',
    },
    'import/resolver': {
      typescript: {
        alwaysTryTypes: true,
        project: './tsconfig.json',
      },
    },
  },

  // Debate UI specific rules
  rules: {
    // Allow longer lines for JSX components
    'max-len': [
      'error',
      {
        code: 120,
        ignoreUrls: true,
        ignoreStrings: true,
        ignoreTemplateLiterals: true,
        ignoreRegExpLiterals: true,
        ignoreComments: true,
        ignorePattern: '^\\s*<.*>.*</.*>$', // Ignore JSX lines
      },
    ],

    // Allow more props for complex debate components
    'react/jsx-max-props-per-line': ['error', { maximum: 3 }],

    // Relax prop validation for debate-specific components
    'react/prop-types': 'off', // Using TypeScript instead

    // Allow console.log in development for debugging debates
    'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',

    // Allow any type for complex debate data structures
    '@typescript-eslint/no-explicit-any': 'warn',

    // Allow non-null assertions for debate state management
    '@typescript-eslint/no-non-null-assertion': 'warn',

    // Relax complexity rules for debate logic
    'sonarjs/cognitive-complexity': ['error', 20],
    'sonarjs/no-duplicate-string': ['error', 5],

    // Allow empty functions for event handlers
    '@typescript-eslint/no-empty-function': [
      'error',
      {
        allow: ['arrowFunctions', 'functions', 'methods'],
      },
    ],

    // Custom rules for debate UI patterns
    'react/jsx-no-bind': [
      'error',
      {
        allowArrowFunctions: true, // Allow arrow functions in JSX for debate handlers
        allowBind: false,
        allowFunctions: false,
      },
    ],

    // Allow unused vars for debate event parameters
    '@typescript-eslint/no-unused-vars': [
      'error',
      {
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        ignoreRestSiblings: true,
      },
    ],

    // Debate-specific import rules
    'import/order': [
      'error',
      {
        groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index', 'type'],
        pathGroups: [
          {
            pattern: 'react',
            group: 'external',
            position: 'before',
          },
          {
            pattern: '@mui/**',
            group: 'external',
            position: 'after',
          },
          {
            pattern: '../**',
            group: 'parent',
            position: 'before',
          },
        ],
        pathGroupsExcludedImportTypes: ['react'],
        'newlines-between': 'always',
        alphabetize: {
          order: 'asc',
          caseInsensitive: true,
        },
      },
    ],

    // Allow specific naming patterns for debate components
    'react/jsx-pascal-case': [
      'error',
      {
        allowAllCaps: true,
        allowNamespace: true,
        allowLeadingUnderscore: true,
      },
    ],

    // Accessibility rules specific to debate interface
    'jsx-a11y/click-events-have-key-events': 'warn', // Debate interactions might be mouse-only
    'jsx-a11y/no-static-element-interactions': 'warn',

    // Performance rules for debate rendering
    'react/jsx-no-constructed-context-values': 'error',
    'react/no-unstable-nested-components': 'error',
  },

  // Environment-specific overrides
  overrides: [
    {
      // Test files
      files: ['**/*.test.ts', '**/*.test.tsx', '**/*.spec.ts', '**/*.spec.tsx'],
      env: {
        jest: true,
      },
      rules: {
        '@typescript-eslint/no-explicit-any': 'off',
        '@typescript-eslint/no-non-null-assertion': 'off',
        'sonarjs/no-duplicate-string': 'off',
        'max-len': 'off',
      },
    },
    {
      // Story files (Storybook)
      files: ['**/*.stories.ts', '**/*.stories.tsx'],
      rules: {
        'import/no-extraneous-dependencies': 'off',
        '@typescript-eslint/no-explicit-any': 'off',
      },
    },
    {
      // Configuration files
      files: ['**/*.config.ts', '**/*.config.js'],
      rules: {
        'import/no-extraneous-dependencies': 'off',
        '@typescript-eslint/no-var-requires': 'off',
      },
    },
    {
      // Debate-specific component files
      files: ['**/components/Debate*.tsx', '**/pages/Debate*.tsx'],
      rules: {
        'sonarjs/cognitive-complexity': ['error', 25], // Allow higher complexity for debate components
        'react/jsx-max-depth': ['error', { max: 6 }], // Allow deeper nesting for debate UI
        'max-lines-per-function': ['error', { max: 100 }], // Allow longer functions for debate logic
      },
    },
    {
      // Redux/State management files
      files: ['**/store/**/*.ts', '**/slices/**/*.ts'],
      rules: {
        '@typescript-eslint/no-explicit-any': 'warn',
        'sonarjs/no-duplicate-string': 'off',
        'no-param-reassign': [
          'error',
          {
            props: true,
            ignorePropertyModificationsFor: ['state'], // Allow state mutations in Redux Toolkit
          },
        ],
      },
    },
  ],
};
