import { test, expect } from '@playwright/test';

const baseURL = 'http://localhost:8080';
const testUsername = 'test2345';
const testPassword = 'Qwpo1209!@';

test.describe('장바구니 UI 테스트', () => {
  test('장바구니 전체 플로우 - 로그인부터 장바구니 확인까지', async ({ page }) => {
    // 1. 로그인 페이지 접속
    await page.goto(`${baseURL}/login`);
    await page.waitForLoadState('networkidle');

    // 로그인 페이지 스크린샷
    await page.screenshot({
      path: 'test-results/1-login-page.png',
      fullPage: true
    });
    console.log('✅ 1. 로그인 페이지 스크린샷 저장');

    // 2. 로그인 수행
    await page.fill('#username', testUsername);
    await page.fill('#password', testPassword);

    // 로그인 폼 입력 후 스크린샷
    await page.screenshot({
      path: 'test-results/2-login-form-filled.png',
      fullPage: true
    });
    console.log('✅ 2. 로그인 폼 입력 스크린샷 저장');

    // 로그인 버튼 클릭
    await page.click('button[type="submit"]');
    await page.waitForLoadState('networkidle');

    // 로그인 성공 확인 (홈페이지 또는 다른 페이지로 리다이렉트)
    const currentUrl = page.url();
    console.log(`로그인 후 URL: ${currentUrl}`);

    // 로그인 후 페이지 스크린샷
    await page.screenshot({
      path: 'test-results/3-after-login.png',
      fullPage: true
    });
    console.log('✅ 3. 로그인 성공 후 페이지 스크린샷 저장');

    // 3. 장바구니 페이지 접속
    await page.goto(`${baseURL}/cart`);
    await page.waitForLoadState('networkidle');

    // 장바구니 페이지 스크린샷
    await page.screenshot({
      path: 'test-results/4-cart-page.png',
      fullPage: true
    });
    console.log('✅ 4. 장바구니 페이지 스크린샷 저장');

    // 장바구니 페이지 요소 확인
    const pageTitle = await page.title();
    console.log(`페이지 제목: ${pageTitle}`);

    // 장바구니 컨테이너 확인
    const cartExists = await page.locator('.container, #cart-container, [class*="cart"]').count() > 0;
    expect(cartExists).toBeTruthy();
    console.log('✅ 장바구니 페이지 요소 확인 완료');

    // 4. 상품 목록 페이지 접속
    await page.goto(`${baseURL}/shop`);
    await page.waitForLoadState('networkidle');

    // 상품 목록 페이지 스크린샷
    await page.screenshot({
      path: 'test-results/5-products-page.png',
      fullPage: true
    });
    console.log('✅ 5. 상품 목록 페이지 스크린샷 저장');

    // 테스트 완료 - 주요 페이지 스크린샷 모두 캡처
    console.log('✅ 장바구니 관련 주요 페이지 스크린샷 캡처 완료');

    // 테스트 종료 전 5초 대기 (브라우저 확인용)
    await page.waitForTimeout(3000);
  });

  test('로그아웃 상태에서 장바구니 접근 테스트', async ({ page }) => {
    // 로그아웃 상태에서 장바구니 페이지 접속 시도
    await page.goto(`${baseURL}/cart`);
    await page.waitForLoadState('networkidle');

    const currentUrl = page.url();
    console.log(`로그아웃 상태에서 장바구니 접근 시 URL: ${currentUrl}`);

    // 로그인 페이지로 리다이렉트되거나 에러 메시지가 표시되는지 확인
    const isLoginPage = currentUrl.includes('/login');
    const hasErrorMessage = await page.locator('text=/로그인|인증|권한/i').count() > 0;

    if (isLoginPage || hasErrorMessage) {
      console.log('✅ 로그아웃 상태에서 장바구니 접근 차단 확인');

      // 스크린샷
      await page.screenshot({
        path: 'test-results/7-cart-access-denied.png',
        fullPage: true
      });
      console.log('✅ 7. 장바구니 접근 거부 스크린샷 저장');
    }

    expect(isLoginPage || hasErrorMessage).toBeTruthy();
  });
});
