/**
 * nombre: playwright.config.js
 * descripcion: Configuración de Playwright: levanta API (perfil H2) y front-end compilado.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
import { defineConfig, devices } from '@playwright/test';

const BASE = process.env.E2E_BASE_URL ?? 'http://localhost:5173';
const externo = Boolean(process.env.E2E_BASE_URL);

export default defineConfig({
  testDir: './playwright',
  timeout: 30_000,
  retries: 0,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: { baseURL: BASE, trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'] } },
    { name: 'movil', use: { ...devices['Pixel 7'] }, testMatch: /responsive\.spec\.js/ },
  ],
  webServer: externo ? undefined : [
    {
      command: 'java -jar ../../backend/target/inventario-backend-1.0.0.jar --spring.profiles.active=h2',
      url: 'http://localhost:8080/actuator/health',
      timeout: 120_000,
      reuseExistingServer: true,
    },
    {
      command: 'npx vite preview --port 5173 --strictPort',
      cwd: '../../frontend',
      url: 'http://localhost:5173',
      timeout: 60_000,
      reuseExistingServer: true,
    },
  ],
});
