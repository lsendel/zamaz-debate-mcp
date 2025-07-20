const sharedConfig = require('../../../packages/zamaz-ui/tailwind.config.js');

/** @type {import('tailwindcss').Config} */
module.exports = {
  ...sharedConfig,
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../../../packages/zamaz-ui/src/**/*.{js,ts,jsx,tsx}",
  ],
};