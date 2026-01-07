import { test, expect } from '@playwright/test';

let authToken: string;
const baseURL = 'http://localhost:8080';
const testUsername = 'test2345';
const testPassword = 'Qwpo1209!@';

test.describe('장바구니 API 테스트 (Authentication 기반)', () => {
  // 모든 테스트 전에 로그인하여 JWT 토큰 획득
  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${baseURL}/members/sign-in`, {
      data: {
        username: testUsername,
        password: testPassword
      }
    });

    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    authToken = loginData.accessToken;
    console.log('로그인 성공, JWT 토큰 획득');
  });

  test('1. 장바구니 조회 - Authentication 기반 (memberId 파라미터 제거)', async ({ request }) => {
    // When: Authorization 헤더와 함께 장바구니 조회 (memberId 파라미터 없음)
    const response = await request.get(`${baseURL}/carts`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    // Then: 성공 응답 확인
    expect(response.ok()).toBeTruthy();
    const cartData = await response.json();
    console.log('Cart data:', cartData);

    // CartDto에 productId 포함 여부 확인
    if (cartData.length > 0) {
      expect(cartData[0]).toHaveProperty('productId');
      console.log('✅ CartDto에 productId 포함 확인');
    }
  });

  test('2. 장바구니 추가 - Authentication 기반 (요청 바디에서 memberId 제거)', async ({ request }) => {
    // Given: 추가할 상품 정보 (memberId 없음)
    const addCartRequest = {
      productId: 1,
      quantity: 2
    };

    // When: Authorization 헤더와 함께 장바구니 추가
    const response = await request.post(`${baseURL}/carts`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: addCartRequest
    });

    // Then: 성공 응답 확인
    expect(response.ok()).toBeTruthy();
    const responseData = await response.json();
    console.log('장바구니 추가 결과:', responseData);
    expect(responseData).toHaveProperty('cartId');
    expect(responseData.message).toBe('장바구니에 정상적으로 추가되었습니다.');
  });

  test('3. 장바구니 조회 후 productId 포함 확인', async ({ request }) => {
    // When: 장바구니 조회
    const response = await request.get(`${baseURL}/carts`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    // Then: CartDto에 productId 필드 확인
    expect(response.ok()).toBeTruthy();
    const cartData = await response.json();

    if (cartData.length > 0) {
      expect(cartData[0]).toHaveProperty('productId');
      expect(cartData[0]).toHaveProperty('productName');
      expect(cartData[0]).toHaveProperty('price');
      expect(cartData[0]).toHaveProperty('quantity');
      console.log('✅ CartDto 구조 확인 완료:', cartData[0]);
    }
  });

  test('4. 장바구니 전체 삭제 - Authentication 기반 (memberId 파라미터 제거)', async ({ request }) => {
    // When: Authorization 헤더와 함께 장바구니 전체 삭제 (memberId 파라미터 없음)
    const response = await request.delete(`${baseURL}/carts-all`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    // Then: 성공 응답 확인
    expect(response.status()).toBe(202); // ACCEPTED
    console.log('✅ 장바구니 전체 삭제 성공 (202 ACCEPTED)');
  });

  test('5. 인증 없이 장바구니 조회 시 401/403 Unauthorized', async ({ request }) => {
    // When: Authorization 헤더 없이 장바구니 조회
    const response = await request.get(`${baseURL}/carts`);

    // Then: 401 또는 403 응답 확인 (Spring Security는 403을 반환할 수 있음)
    expect([401, 403]).toContain(response.status());
    console.log('✅ 인증 없이 접근 시 401/403 반환 확인');
  });

  test('6. 인증 없이 장바구니 추가 시 401/403 Unauthorized', async ({ request }) => {
    // When: Authorization 헤더 없이 장바구니 추가
    const response = await request.post(`${baseURL}/carts`, {
      headers: {
        'Content-Type': 'application/json'
      },
      data: {
        productId: 1,
        quantity: 1
      }
    });

    // Then: 401 또는 403 응답 확인
    expect([401, 403]).toContain(response.status());
    console.log('✅ 인증 없이 추가 시 401/403 반환 확인');
  });

  test('7. 인증 없이 장바구니 전체 삭제 시 401/403 Unauthorized', async ({ request }) => {
    // When: Authorization 헤더 없이 장바구니 전체 삭제
    const response = await request.delete(`${baseURL}/carts-all`);

    // Then: 401 또는 403 응답 확인
    expect([401, 403]).toContain(response.status());
    console.log('✅ 인증 없이 삭제 시 401/403 반환 확인');
  });
});

test.describe('주문 API 테스트 (Authentication 기반)', () => {
  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${baseURL}/members/sign-in`, {
      data: {
        username: testUsername,
        password: testPassword
      }
    });

    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    authToken = loginData.accessToken;
  });

  test('8. 내 주문 조회 - Authentication 기반 (memberId 파라미터 제거)', async ({ request }) => {
    // When: Authorization 헤더와 함께 내 주문 조회 (memberId 파라미터 없음)
    const response = await request.get(`${baseURL}/orders/my-order`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    // Then: 성공 응답 확인
    expect(response.ok()).toBeTruthy();
    const orders = await response.json();
    console.log('내 주문 목록:', orders);
  });

  test('9. 인증 없이 내 주문 조회 시 401/403 Unauthorized', async ({ request }) => {
    // When: Authorization 헤더 없이 내 주문 조회
    const response = await request.get(`${baseURL}/orders/my-order`);

    // Then: 401 또는 403 응답 확인
    expect([401, 403]).toContain(response.status());
    console.log('✅ 인증 없이 주문 조회 시 401/403 반환 확인');
  });
});
