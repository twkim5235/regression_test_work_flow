import { Page, APIRequestContext } from '@playwright/test';
import { API_BASE_URL, TEST_USER } from './fixtures';

// 로그인 헬퍼
export async function login(request: APIRequestContext): Promise<string> {
  const response = await request.post(`${API_BASE_URL}/members/sign-in`, {
    data: {
      username: TEST_USER.username,
      password: TEST_USER.password
    }
  });
  const data = await response.json();
  return data.accessToken;
}

// 인증 헤더 생성
export function authHeader(token: string) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}

// 장바구니 전체 삭제
export async function clearCart(request: APIRequestContext, token: string) {
  await request.delete(`${API_BASE_URL}/carts-all`, {
    headers: authHeader(token)
  });
}

// 스크린샷 저장 (PR 결과용)
export async function saveScreenshot(page: Page, prFolder: string, name: string) {
  await page.screenshot({
    path: `../../pr-results/${prFolder}/screenshots/${name}.png`,
    fullPage: true
  });
}
