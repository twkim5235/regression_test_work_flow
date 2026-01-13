import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: '../coverage-reports/latest' }],
    ['json', { outputFile: '../coverage-reports/latest/results.json' }],
    ['list']
  ],
  timeout: 60000,
  expect: {
    timeout: 10000
  },
  use: {
    baseURL: process.env.API_URL || 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'on',
    video: 'retain-on-failure',
    headless: process.env.CI ? true : false,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
