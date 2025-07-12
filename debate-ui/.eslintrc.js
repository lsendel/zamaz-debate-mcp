module.exports = {
  extends: [
    "next/core-web-vitals"
  ],
  plugins: ["security"],
  rules: {
    // Security rules
    "security/detect-object-injection": "warn", // Changed to warn for development
    "security/detect-non-literal-regexp": "error",
    "security/detect-unsafe-regex": "error",
    "security/detect-buffer-noassert": "error",
    "security/detect-child-process": "error",
    "security/detect-disable-mustache-escape": "error",
    "security/detect-eval-with-expression": "error",
    "security/detect-no-csrf-before-method-override": "error",
    "security/detect-non-literal-fs-filename": "error",
    "security/detect-non-literal-require": "error",
    "security/detect-possible-timing-attacks": "error",
    "security/detect-pseudoRandomBytes": "error",
    
    // Built-in React security rules
    "react/no-danger": "warn", // flags dangerouslySetInnerHTML
    "react/jsx-no-target-blank": "error",
    "react/jsx-no-script-url": "error",
    "react/no-unsafe": "error",
    
    // Additional security-related rules
    "no-eval": "error",
    "no-implied-eval": "error",
    "no-script-url": "error",
    "no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
    
    // Prevent console.log in production builds
    "no-console": process.env.NODE_ENV === "production" ? "error" : "warn"
  },
  env: {
    browser: true,
    es6: true,
    node: true
  },
  parserOptions: {
    ecmaVersion: 2021,
    sourceType: "module",
    ecmaFeatures: {
      jsx: true
    }
  },
  settings: {
    react: {
      version: "detect"
    }
  }
};