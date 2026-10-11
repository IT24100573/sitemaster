/**
 * Colours point at the CSS variables in src/styles/theme.css (stored as RGB
 * channels), so the palette is defined once and opacity modifiers such as
 * bg-primary/10 still work.
 */
const channel = (name) => `rgb(var(${name}) / <alpha-value>)`;

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: channel('--color-primary'),
          dark: channel('--color-primary-dark'),
          light: channel('--color-primary-light'),
        },
        surface: channel('--color-surface'),
        ink: channel('--color-text'),
        muted: channel('--color-text-muted'),
        line: channel('--color-border'),
      },
      borderRadius: {
        card: 'var(--radius-card)',
      },
      boxShadow: {
        card: 'var(--shadow-card)',
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
