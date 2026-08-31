import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig(({ command }) => {
  if (command === 'build' && !process.env.VITE_TICKET_API_BASE_URL) {
    throw new Error(
      'VITE_TICKET_API_BASE_URL must be set for production builds; refusing to default to localhost.',
    );
  }

  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
    },
    test: {
      environment: 'jsdom',
      environmentOptions: {
        jsdom: {
          url: 'http://localhost:5173/',
        },
      },
      globals: true,
      include: ['tests/unit/**/*.test.ts', 'tests/unit/**/*.test.tsx'],
      setupFiles: './tests/support/setup.ts',
    },
  };
});
