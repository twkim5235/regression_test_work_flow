import { test, expect } from '@playwright/test';
import { API_BASE_URL } from '../common/fixtures';
import { authHeader, login } from '../common/helpers';

let authToken: string;

test.describe('@pr-1 주문 API 테스트 (Authentication 기반)', () => {
  test.beforeAll(async ({ request }) => {
    authToken = await login(request);
  });

  test('@regression 내 주문 조회 - Authentication 기반', async ({ request }) => {
    // When: Authorization 헤더와 함께 내 주문 조회 (memberId 파라미터 없음)
    const response = await request.get(`${API_BASE_URL}/orders/my-order`, {
      headers: authHeader(authToken)
    });

    // Then: 성공 응답 확인
    expect(response.ok()).toBeTruthy();
    const orders = await response.json();
    console.log('내 주문 목록:', orders);
  });

  test('@smoke @regression 인증 없이 내 주문 조회 시 401/403 Unauthorized', async ({ request }) => {
    // When: Authorization 헤더 없이 내 주문 조회
    const response = await request.get(`${API_BASE_URL}/orders/my-order`);

    // Then: 401 또는 403 응답 확인
    expect([401, 403]).toContain(response.status());
    console.log('인증 없이 주문 조회 시 401/403 반환 확인');
  });
});
