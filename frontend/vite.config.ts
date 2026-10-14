import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

const configuredApi = process.env.VITE_API_URL ?? 'http://localhost:8080'
const apiOrigin = configuredApi.replace(/\/api\/v1\/?$/, '')

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    proxy: {
      '/api': apiOrigin,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test-setup.ts',
    css: true,
  },
})
