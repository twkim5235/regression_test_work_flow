import { test as base, expect } from '@playwright/test';

// 테스트 계정 정보
export const TEST_USER = {
  username: 'test2345',
  password: 'Qwpo1209!@'
};

// API Base URL
export const API_BASE_URL = process.env.API_URL || 'http://localhost:8080';
export const FRONTEND_URL = process.env.FRONTEND_URL || 'http://localhost:3000';

// 인증된 테스트 픽스처
type AuthFixture = {
  authToken: string;
};

export const test = base.extend<AuthFixture>({
  authToken: async ({ request }, use) => {
    const response = await request.post(`${API_BASE_URL}/members/sign-in`, {
      data: {
        username: TEST_USER.username,
        password: TEST_USER.password
      }
    });
    const data = await response.json();
    await use(data.accessToken);
  }
});

export { expect };
