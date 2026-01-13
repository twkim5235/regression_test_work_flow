# PR 리그레션 테스트 워크플로우

## 📋 목차
1. [개요](#개요)
2. [폴더 구조](#폴더-구조)
3. [사전 준비사항](#사전-준비사항)
4. [워크플로우 단계](#워크플로우-단계)
5. [문서 작성 가이드](#문서-작성-가이드)
6. [Playwright 테스트 작성 가이드](#playwright-테스트-작성-가이드)
7. [테스트 실행 및 결과 문서화](#테스트-실행-및-결과-문서화)
8. [PR 코멘트 자동화](#pr-코멘트-자동화)
9. [PR 승인 전 체크리스트](#pr-승인-전-체크리스트)
10. [트러블슈팅](#트러블슈팅)

---

## 개요

### 목적
Store 프로젝트의 PR에서 구현하거나 변경된 기능에 대해 Playwright를 사용한 웹 E2E 테스트를 통해 리그레션(회귀) 테스트를 수행하고, 그 결과를 체계적으로 문서화합니다.

### 범위
- 백엔드 API 변경사항에 대한 웹 UI 테스트
- 신규 기능 추가 시 E2E 테스트
- 기존 기능 수정 시 영향도 검증
- 테스트 결과의 시각적 문서화 (스크린샷 포함)

### 통합 테스트 스위트와의 관계

| 구분 | PR 리그레션 테스트 (이 문서) | 통합 테스트 스위트 |
|------|---------------------------|-------------------|
| **목적** | PR 변경사항 검증 | 전체 기능 리그레션 |
| **실행 시점** | PR 올라올 때마다 | 언제든지 (배포 전, 정기적으로) |
| **PR 필요** | O (PR 분석, 코멘트) | X (독립 실행) |
| **테스트 범위** | PR 관련 기능만 | 전체 기능 |
| **참고 문서** | 이 문서 | [통합-테스트-스위트-가이드.md](./통합-테스트-스위트-가이드.md) |

**워크플로우**:
```
PR 테스트 작성 → 검증 완료 → PR 코멘트 → 통합 스위트에 병합 → 커버리지 누적
```

---

## 폴더 구조

```
store/
├── PR-리그레션-테스트-워크플로우.md     (이 문서)
├── 통합-테스트-스위트-가이드.md         (통합 테스트 가이드)
│
└── regression-tests/                    (리그레션 테스트 루트)
    │
    ├── playwright-tests/                (통합 테스트 스위트 - 누적)
    │   ├── tests/                       (기능별 테스트 파일)
    │   │   ├── auth/                    (인증 테스트)
    │   │   ├── cart/                    (장바구니 테스트)
    │   │   ├── order/                   (주문 테스트)
    │   │   └── common/                  (공통 유틸리티)
    │   ├── playwright.config.ts
    │   └── package.json
    │
    ├── pr-results/                      (PR별 테스트 결과 보관)
    │   ├── {PR-이름-1}/                 (예: 장바구니-리팩토링)
    │   │   ├── PR-분석.md               (PR 변경사항 분석 문서)
    │   │   ├── 테스트-결과.md           (테스트 결과 문서)
    │   │   ├── pr-comment.md            (PR 코멘트용 문서)
    │   │   └── screenshots/             (테스트 결과 스크린샷)
    │   │       ├── 1-login-page.png
    │   │       ├── 2-cart-add.png
    │   │       └── ...
    │   └── {PR-이름-2}/                 (예: 주문-할인-로직-수정)
    │       └── ...
    │
    └── coverage-reports/                (커버리지 리포트)
        ├── latest/
        └── 2026-01-09/
```

### 폴더 구조 설명
| 폴더 | 설명 |
|------|------|
| `playwright-tests/` | 모든 테스트가 누적되는 통합 스위트 (PR 테스트 완료 후 병합) |
| `pr-results/{PR-이름}/` | PR별 테스트 결과 문서 및 스크린샷 보관 |
| `coverage-reports/` | 전체 테스트 실행 결과 리포트 |

### 핵심 개념
- **테스트 코드**: `playwright-tests/`에 통합 관리 (누적)
- **테스트 결과**: `pr-results/{PR-이름}/`에 PR별로 보관
- PR 테스트 완료 후, 재사용 가능한 테스트는 통합 스위트에 병합

### 폴더 명명 규칙
- PR 폴더명: `{기능명}-{작업유형}` 형식 (예: `회원가입-기능-추가`, `주문-취소-버그수정`)
- 한글 사용, 공백 대신 하이픈(-) 사용
- 간결하고 명확한 이름 사용

---

## 사전 준비사항

### 1. 백엔드 환경 설정
```bash
# MySQL 데이터베이스 실행 확인
# localhost:3306에 'ddd_start' 데이터베이스 필요

# .env 파일 설정 확인 (store 디렉토리 내)
MYSQL_ROOT_PASSWORD=your_password
JWT_SECRET=your_secret_key
JWT_EXPIRES=86400000
```

### 2. Playwright 설치
```bash
# Playwright 프로젝트 초기화
npm init -y
npm install -D @playwright/test typescript @types/node

# 브라우저 설치
npx playwright install

# 필요한 경우 의존성 설치
npx playwright install-deps
```

**playwright.config.ts 설정 파일 생성**:
```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  timeout: 60000,
  expect: {
    timeout: 10000
  },
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'on',  // 모든 테스트에서 스크린샷 캡처
    video: 'retain-on-failure',  // 실패 시 비디오 저장
    headless: false,  // UI 표시 (브라우저 화면 보기)
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
```

**package.json scripts 설정**:
```json
{
  "scripts": {
    "test": "playwright test --headed",
    "test:headless": "playwright test",
    "test:ui": "playwright test --ui",
    "test:report": "playwright show-report"
  }
}
```

### 3. 프론트엔드 환경
- 프론트엔드 애플리케이션 URL 확인 (예: `http://localhost:3000`)
- 테스트용 계정 준비


### 4. 테스트 계정
- id: test2345
- password: Qwpo1209!@
---

## 워크플로우 단계

### 1단계: PR 분석 및 폴더 생성

#### 1.1 PR 체크아웃
```bash
# PR 브랜치로 체크아웃
cd store
git fetch origin
git checkout {pr-branch-name}
```

#### 1.2 PR 결과 폴더 생성
```bash
cd regression-tests/pr-results
mkdir {PR-이름}
cd {PR-이름}

# 스크린샷 폴더 생성
mkdir screenshots
```

> **참고**: 테스트 코드는 `regression-tests/playwright-tests/`의 통합 스위트에 작성합니다.
> PR별 폴더에는 결과 문서와 스크린샷만 보관합니다.

#### 1.3 PR 분석 문서 작성
`PR-분석.md` 파일을 생성하고 다음 내용을 작성합니다:
- PR 개요 (제목, 번호, 작성자)
- 변경된 기능 목록
- 영향을 받는 API 엔드포인트
- 테스트 범위 정의
- 예상되는 UI 변화

### 2단계: 백엔드 빌드 및 실행

```bash
cd ../../store

# 빌드
./gradlew clean build -x test

# 실행
./gradlew bootRun

# 또는 JAR 실행
java -jar build/libs/*.jar
```

백엔드가 정상 실행되는지 확인:
```bash
# Health check
curl http://localhost:8080/actuator/health
```

### 3단계: Playwright 테스트 작성

#### 3.1 통합 테스트 스위트에서 작업
```bash
cd regression-tests/playwright-tests
```

#### 3.2 테스트 코드 작성
변경된 기능에 대한 E2E 테스트 시나리오를 통합 스위트에 작성합니다.

- **새 기능**: `tests/{기능}/` 폴더에 새 테스트 파일 생성
- **기존 기능 수정**: 기존 테스트 파일에 테스트 케이스 추가
- **PR 태그**: `@pr-{PR번호}` 태그를 추가하여 PR 관련 테스트 식별

```typescript
// 예: tests/cart/cart-api.spec.ts
test('@pr-123 @regression 장바구니 동일 상품 수량 증가', async ({ request }) => {
  // 테스트 코드
});
```

(자세한 내용은 "Playwright 테스트 작성 가이드" 및 [통합-테스트-스위트-가이드.md](./통합-테스트-스위트-가이드.md) 참고)

### 4단계: 테스트 실행

```bash
cd regression-tests/playwright-tests
```

**PR 관련 테스트만 실행**:
```bash
# PR 태그로 필터링하여 실행
npx playwright test --grep @pr-123 --headed

# 또는 특정 기능 테스트 실행
npx playwright test tests/cart/ --headed
```

**전체 테스트 실행**:
```bash
# 모든 테스트 실행 (브라우저 UI 표시)
npm test

# 헤드리스 모드로 실행 (CI/CD용)
npm run test:headless

# UI 모드로 실행 (디버깅용, 인터랙티브)
npx playwright test --ui

# HTML 리포트 생성 및 보기
npx playwright test --reporter=html
npm run report
```

**스크린샷 확인**:
- 테스트 실행 중: `playwright-tests/test-results/` 디렉토리에 자동 저장
- 파일명: `test-finished-1.png`, `test-finished-2.png` 등
- 실패한 테스트: 추가로 `test-failed-*.png` 및 비디오 저장
- **문서화용**: 주요 스크린샷을 선별하여 `pr-results/{PR-이름}/screenshots/`로 복사 정리 (5단계 참조)

### 5단계: 스크린샷 정리 및 테스트 결과 문서화

#### 5.1 스크린샷 정리
```bash
# 통합 스위트에서 PR 결과 폴더로 스크린샷 복사
cd regression-tests

# 주요 스크린샷을 PR 결과 폴더로 복사
cp playwright-tests/test-results/{테스트명}/test-finished-*.png pr-results/{PR-이름}/screenshots/

# 파일명을 의미있게 변경
cd pr-results/{PR-이름}/screenshots
mv test-finished-1.png 1-login-page.png
mv test-finished-2.png 2-cart-add.png
```

#### 5.2 테스트-결과.md 작성
`pr-results/{PR-이름}/테스트-결과.md` 파일을 생성하고 테스트 결과를 기록합니다.
스크린샷은 `screenshots/` 폴더의 이미지를 참조합니다.
(자세한 내용은 "테스트 실행 및 결과 문서화" 참고)

### 6단계: PR에 테스트 결과 코멘트 작성

테스트 완료 후, GitHub CLI를 사용하여 PR에 테스트 결과를 코멘트로 추가합니다.

#### 6.1 GitHub CLI 설치 및 인증
```bash
# GitHub CLI 설치 (macOS)
brew install gh

# GitHub CLI 설치 (Windows)
winget install --id GitHub.cli

# GitHub 인증
gh auth login
```

#### 6.2 PR 코멘트 작성
```bash
# PR 번호로 코멘트 작성
gh pr comment {PR번호} --body "$(cat <<'EOF'
## 🧪 리그레션 테스트 결과

### 테스트 환경
- **테스트 일시**: $(date '+%Y-%m-%d %H:%M')
- **브랜치**: {브랜치명}
- **테스트 실행자**: {이름}

### 테스트 결과 요약

| 테스트 케이스 | 상태 | 비고 |
|--------------|------|------|
| 테스트 1 | ✅ 통과 | - |
| 테스트 2 | ✅ 통과 | - |
| 테스트 3 | ❌ 실패 | 원인 설명 |

**전체 결과**: X/Y 통과 (Z%)

### 상세 내용
- 📄 [PR 분석 문서](regression-tests/pr-results/{PR-이름}/PR-분석.md)
- 📋 [테스트 결과 문서](regression-tests/pr-results/{PR-이름}/테스트-결과.md)
- 📸 스크린샷: `regression-tests/pr-results/{PR-이름}/screenshots/`

### 결론
- [ ] ✅ 승인 가능 (모든 테스트 통과)
- [ ] ⚠️ 수정 필요 (이슈 해결 후 재테스트)
- [ ] ❌ 승인 불가 (심각한 결함 발견)
EOF
)"

# 또는 파일에서 읽어서 코멘트 작성
gh pr comment {PR번호} --body-file regression-tests/pr-results/{PR-이름}/pr-comment.md
```

#### 6.3 자동화 스크립트 활용
PR 코멘트 작성을 자동화하려면 `post-comment.sh` 스크립트를 사용합니다.
(자세한 내용은 "PR 코멘트 자동화" 섹션 참고)

### 7단계: PR 리뷰 및 승인

체크리스트를 확인하고 최종 승인 여부를 결정합니다.

---

## 문서 작성 가이드

### PR-분석.md 템플릿

```markdown
# PR 분석: {PR 제목}

## PR 정보
- **PR 번호**: #{PR번호}
- **작성자**: {작성자}
- **작성일**: YYYY-MM-DD
- **브랜치**: {브랜치명}
- **PR 링크**: {GitHub PR URL}

## 변경사항 요약
{PR에서 변경된 내용을 간략히 설명}

## 변경된 기능
1. **기능 1**: {설명}
   - 파일: `경로/파일명.java`
   - 변경 내용: {상세 설명}

2. **기능 2**: {설명}
   - 파일: `경로/파일명.java`
   - 변경 내용: {상세 설명}

## 영향 받는 API 엔드포인트
| 메서드 | 엔드포인트 | 변경 내용 |
|--------|-----------|----------|
| POST | /api/example | 신규 추가 |
| PUT | /api/example/{id} | 로직 수정 |

## 테스트 범위
- [ ] 테스트 시나리오 1: {설명}
- [ ] 테스트 시나리오 2: {설명}
- [ ] 테스트 시나리오 3: {설명}

## 예상되는 UI 변화
- {변화 1}
- {변화 2}

## 추가 고려사항
- {특이사항이나 주의사항}
```

---

## Playwright 테스트 작성 가이드

### 기본 테스트 구조

```typescript
import { test, expect } from '@playwright/test';

test.describe('기능명 테스트', () => {
  test.beforeEach(async ({ page }) => {
    // 각 테스트 전에 실행할 공통 로직
    await page.goto('http://localhost:3000');
  });

  test('테스트 케이스 1: 정상 동작 확인', async ({ page }) => {
    // Given: 테스트 전제조건

    // When: 테스트 실행 동작

    // Then: 결과 검증
    await expect(page).toHaveTitle(/예상되는 제목/);
  });

  test('테스트 케이스 2: 오류 처리 확인', async ({ page }) => {
    // 테스트 로직
  });
});
```

### Store 프로젝트 테스트 예시

#### 0. API 테스트 예시 (백엔드 직접 테스트)

**장바구니 API 테스트 (Authentication 기반)**:
```typescript
import { test, expect } from '@playwright/test';

let authToken: string;
const baseURL = 'http://localhost:8080';

test.describe('장바구니 API 테스트', () => {
  // 모든 테스트 전에 로그인하여 JWT 토큰 획득
  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${baseURL}/members/sign-in`, {
      data: {
        username: 'testuser',
        password: 'Test1234'
      }
    });

    expect(loginResponse.ok()).toBeTruthy();
    const loginData = await loginResponse.json();
    authToken = loginData.accessToken;
  });

  test('장바구니 조회 - Authentication 기반', async ({ request }) => {
    // When: Authorization 헤더와 함께 장바구니 조회
    const response = await request.get(`${baseURL}/carts`, {
      headers: {
        'Authorization': `Bearer ${authToken}`
      }
    });

    // Then: 성공 응답 확인
    expect(response.ok()).toBeTruthy();
    const cartData = await response.json();
    console.log('Cart data:', cartData);
  });

  test('장바구니 추가 - Authentication 기반', async ({ request }) => {
    const response = await request.post(`${baseURL}/carts`, {
      headers: {
        'Authorization': `Bearer ${authToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        productId: 1,
        quantity: 2
      }
    });

    expect(response.ok()).toBeTruthy();
  });

  test('인증 없이 접근 시 401 오류', async ({ request }) => {
    const response = await request.get(`${baseURL}/carts`);
    expect(response.status()).toBeGreaterThanOrEqual(401);
  });
});
```

#### 1. 회원가입 테스트 예시 (UI 테스트)
```typescript
import { test, expect } from '@playwright/test';

test.describe('회원가입 기능 테스트', () => {
  test('정상 회원가입 플로우', async ({ page }) => {
    // 회원가입 페이지로 이동
    await page.goto('http://localhost:3000/signup');

    // 폼 입력
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Test1234!');
    await page.fill('input[name="email"]', 'test@example.com');

    // 스크린샷 캡처 (폼 입력 후)
    await page.screenshot({ path: 'screenshots/signup-form-filled.png' });

    // 가입 버튼 클릭
    await page.click('button[type="submit"]');

    // 성공 메시지 확인
    await expect(page.locator('.success-message')).toBeVisible();

    // 스크린샷 캡처 (성공 후)
    await page.screenshot({ path: 'screenshots/signup-success.png' });
  });

  test('중복 아이디 오류 처리', async ({ page }) => {
    await page.goto('http://localhost:3000/signup');

    // 이미 존재하는 아이디로 가입 시도
    await page.fill('input[name="username"]', 'existing-user');
    await page.fill('input[name="password"]', 'Test1234!');
    await page.click('button[type="submit"]');

    // 오류 메시지 확인
    await expect(page.locator('.error-message')).toContainText('이미 사용 중인 아이디');

    await page.screenshot({ path: 'screenshots/signup-duplicate-error.png' });
  });
});
```

#### 2. 로그인 및 JWT 인증 테스트 예시
```typescript
import { test, expect } from '@playwright/test';

test.describe('로그인 및 인증 테스트', () => {
  test('로그인 후 JWT 토큰으로 인증된 요청', async ({ page }) => {
    // 로그인
    await page.goto('http://localhost:3000/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Test1234!');

    await page.screenshot({ path: 'screenshots/login-form.png' });

    await page.click('button[type="submit"]');

    // 토큰이 localStorage에 저장되었는지 확인
    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    expect(token).toBeTruthy();

    // 인증이 필요한 페이지로 이동
    await page.goto('http://localhost:3000/my-orders');

    // 주문 목록이 표시되는지 확인
    await expect(page.locator('.order-list')).toBeVisible();

    await page.screenshot({ path: 'screenshots/authenticated-page.png' });
  });
});
```

#### 3. 주문 생성 테스트 예시
```typescript
import { test, expect } from '@playwright/test';

test.describe('주문 생성 테스트', () => {
  test.beforeEach(async ({ page }) => {
    // 로그인
    await page.goto('http://localhost:3000/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Test1234!');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/');
  });

  test('상품 선택 후 주문 생성', async ({ page }) => {
    // 상품 목록 페이지
    await page.goto('http://localhost:3000/products');
    await page.screenshot({ path: 'screenshots/order-products-list.png' });

    // 상품 선택
    await page.click('.product-item:first-child .add-to-cart');

    // 장바구니로 이동
    await page.goto('http://localhost:3000/cart');
    await page.screenshot({ path: 'screenshots/order-cart.png' });

    // 주문하기 클릭
    await page.click('button:has-text("주문하기")');

    // 배송 정보 입력
    await page.fill('input[name="address"]', '서울시 강남구');
    await page.fill('input[name="detailedAddress"]', '101동 101호');
    await page.fill('input[name="zipCode"]', '12345');

    await page.screenshot({ path: 'screenshots/order-shipping-info.png' });

    // 주문 완료
    await page.click('button:has-text("주문 완료")');

    // 주문 완료 메시지 확인
    await expect(page.locator('.order-success')).toBeVisible();
    await page.screenshot({ path: 'screenshots/order-success.png' });

    // 주문 번호 확인
    const orderNumber = await page.locator('.order-number').textContent();
    expect(orderNumber).toMatch(/ORD-\d+/);
  });
});
```

#### 4. 쿠폰 적용 테스트 예시
```typescript
import { test, expect } from '@playwright/test';

test.describe('쿠폰 할인 테스트', () => {
  test('주문 시 쿠폰 적용 및 할인 확인', async ({ page }) => {
    // 로그인 및 장바구니 담기 (생략)

    // 주문 페이지에서 쿠폰 선택
    await page.selectOption('select[name="coupon"]', 'WELCOME10');

    // 할인 전 금액
    const originalPrice = await page.locator('.original-price').textContent();

    // 쿠폰 적용 버튼 클릭
    await page.click('button:has-text("쿠폰 적용")');

    await page.screenshot({ path: 'screenshots/coupon-applied.png' });

    // 할인된 금액 확인
    const discountedPrice = await page.locator('.final-price').textContent();
    expect(discountedPrice).not.toBe(originalPrice);

    // 할인 금액 표시 확인
    await expect(page.locator('.discount-amount')).toBeVisible();
  });
});
```

### 스크린샷 캡처 가이드

**중요**: Playwright 테스트 실행 중에는 자동으로 `test-results/` 폴더에 스크린샷이 저장됩니다. 테스트 완료 후, 문서화를 위해 주요 스크린샷만 선별하여 `../screenshots/` 폴더로 복사 정리합니다.

**1. 자동 스크린샷 (playwright.config.ts 설정 기반)**:
```typescript
// playwright.config.ts에 screenshot: 'on' 설정 시
// 모든 테스트 단계마다 자동으로 스크린샷 캡처
// playwright-tests/test-results/ 디렉토리에 자동 저장

// 테스트 실행 후:
// playwright-tests/test-results/{테스트명}/test-finished-1.png
// playwright-tests/test-results/{테스트명}/test-finished-2.png
// ...
```

**2. 테스트 실행 중 스크린샷 (테스트 코드 내에서)**:
```typescript
import { test, expect } from '@playwright/test';

test.describe('장바구니 테스트', () => {
  test('장바구니 추가 및 조회', async ({ page }) => {
    // 1단계: 로그인
    await page.goto('http://localhost:3000/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'Test1234');

    // 스크린샷은 test-results에 자동 저장됨
    // 또는 명시적으로 특정 단계 캡처 (선택사항)
    await page.screenshot({
      path: 'test-results/manual-login-page.png',
      fullPage: true
    });

    await page.click('button[type="submit"]');

    // 나머지 테스트 진행...
    // screenshot: 'on' 설정으로 모든 단계가 자동 캡처됨
  });
});
```

**3. 테스트 완료 후 스크린샷 정리 (문서화 준비)**:
```bash
# playwright-tests 디렉토리에서 실행 후
cd ..  # PR 루트로 이동

# 주요 스크린샷을 screenshots 폴더로 복사하여 정리
cp playwright-tests/test-results/{테스트-1}/test-finished-1.png screenshots/1-login-page.png
cp playwright-tests/test-results/{테스트-2}/test-finished-1.png screenshots/2-cart-add.png

# 이제 테스트-결과.md에서 screenshots/ 폴더의 이미지를 참조
```

**스크린샷 옵션**:
```typescript
// 전체 페이지 스크린샷 (스크롤 포함)
await page.screenshot({
  path: 'screenshots/full-page.png',
  fullPage: true
});

// 특정 요소만 캡처
await page.locator('.cart-item').screenshot({
  path: 'screenshots/cart-item.png'
});

// 화질 조정 (JPEG)
await page.screenshot({
  path: 'screenshots/page.jpg',
  type: 'jpeg',
  quality: 90
});
```

**스크린샷 명명 규칙**:
- `{순서번호}-{단계설명}.png` 형식 사용
- 예: `1-login-page.png`, `2-cart-add.png`, `3-order-success.png`
- 테스트별로 폴더 분리: `screenshots/cart-test/`, `screenshots/order-test/`

### 테스트 베스트 프랙티스

1. **명확한 테스트 이름**: 무엇을 테스트하는지 명확히
2. **독립적인 테스트**: 각 테스트는 서로 독립적으로 실행 가능해야 함
3. **Given-When-Then 패턴**: 테스트 구조를 명확히
4. **적절한 대기**: `waitForSelector`, `waitForURL` 등을 사용하여 안정적인 테스트 작성
5. **스크린샷 활용**: 주요 단계마다 스크린샷 캡처
6. **의미있는 Assertion**: 단순 존재 확인이 아닌 구체적인 값 검증

---

## 테스트 실행 및 결과 문서화

### 테스트 실행

```bash
# regression-tests/{PR-이름}/playwright-tests 디렉토리에서

# 기본 실행 (브라우저 UI 보이면서, 스크린샷 자동 캡처)
npm test

# 또는
npx playwright test --headed

# HTML 리포트 생성 및 보기
npx playwright test --reporter=html
npx playwright show-report
```

### 스크린샷 정리 및 문서화 준비

테스트 실행 후, 스크린샷을 테스트-결과.md에서 참조할 수 있도록 정리합니다.

```bash
# 1. 현재 위치: regression-tests/{PR-이름}/playwright-tests

# 2. 테스트 실행 후 생성된 스크린샷 확인
ls -la test-results/

# 3. PR 루트에 screenshots 폴더 생성
cd ..
mkdir -p screenshots

# 4. 필요한 스크린샷을 screenshots 폴더로 복사 및 이름 정리
# test-results에서 의미있는 스크린샷만 선별하여 복사
cp playwright-tests/test-results/{테스트명}/test-finished-1.png screenshots/1-login-page.png
cp playwright-tests/test-results/{테스트명}/test-finished-2.png screenshots/2-cart-add.png
cp playwright-tests/test-results/{테스트명}/test-finished-3.png screenshots/3-order-success.png

# 5. 복사된 스크린샷 확인
ls -la screenshots/
```

**스크린샷 정리 가이드**:
- 순서대로 번호를 매겨 정리: `1-`, `2-`, `3-` ...
- 명확한 설명 포함: `1-login-page.png`, `2-cart-add-success.png`
- 테스트-결과.md에서 참조할 주요 스크린샷만 선별
- 원본은 `playwright-tests/test-results/`에 보관, 정리본은 `screenshots/`에 저장

**자동화 스크립트 예시**:
```bash
# organize-screenshots.sh 생성
#!/bin/bash
cd ../screenshots
rm -rf *  # 기존 스크린샷 정리

# 주요 스크린샷만 복사 (테스트 케이스별로)
cp ../playwright-tests/test-results/*로그인*/test-finished-*.png ./1-login-page.png
cp ../playwright-tests/test-results/*장바구니*/test-finished-*.png ./2-cart-add.png
cp ../playwright-tests/test-results/*주문*/test-finished-*.png ./3-order-success.png

echo "스크린샷 정리 완료!"
ls -la
```

### 테스트-결과.md 템플릿

```markdown
# 테스트 결과: {PR 제목}

## 테스트 정보
- **테스트 일시**: YYYY-MM-DD HH:MM
- **테스트 실행자**: {이름}
- **백엔드 버전**: {브랜치명 또는 커밋 해시}
- **프론트엔드 URL**: http://localhost:3000

## 테스트 환경
- **OS**: macOS / Windows / Linux
- **브라우저**: Chromium 120.0, Firefox 121.0, WebKit 17.0
- **Playwright 버전**: 1.40.0

## 테스트 실행 결과 요약

| 테스트 케이스 | 상태 | 소요 시간 | 비고 |
|--------------|------|----------|------|
| 회원가입 - 정상 플로우 | ✅ 통과 | 2.3s | - |
| 회원가입 - 중복 아이디 오류 | ✅ 통과 | 1.8s | - |
| 로그인 - JWT 인증 확인 | ✅ 통과 | 1.5s | - |
| 주문 생성 - 정상 플로우 | ❌ 실패 | 3.2s | 배송비 계산 오류 |

**전체 결과**: 3/4 통과 (75%)

## 상세 테스트 결과

### ✅ 테스트 1: 회원가입 - 정상 플로우

**시나리오**:
1. 회원가입 페이지 접속
2. 사용자 정보 입력 (username, password, email)
3. 가입 버튼 클릭
4. 성공 메시지 확인

**결과**: 통과

**스크린샷**:

![회원가입 폼 입력](screenshots/signup-form-filled.png)
*그림 1: 회원가입 폼 입력 화면*

![회원가입 성공](screenshots/signup-success.png)
*그림 2: 회원가입 성공 메시지*

**검증 항목**:
- [x] 폼 입력 정상 동작
- [x] 백엔드 API 호출 성공 (POST /members/join)
- [x] 성공 메시지 표시
- [x] 환영 쿠폰 자동 발급 확인

---

### ✅ 테스트 2: 회원가입 - 중복 아이디 오류

**시나리오**:
1. 이미 존재하는 아이디로 가입 시도
2. 오류 메시지 확인

**결과**: 통과

**스크린샷**:

![중복 아이디 오류](screenshots/signup-duplicate-error.png)
*그림 3: 중복 아이디 오류 메시지*

**검증 항목**:
- [x] 중복 검증 API 호출 (POST /members/join)
- [x] 400 Bad Request 응답 확인
- [x] 사용자 친화적인 오류 메시지 표시

---

### ❌ 테스트 3: 주문 생성 - 정상 플로우

**시나리오**:
1. 로그인 후 상품 선택
2. 장바구니 추가
3. 배송 정보 입력
4. 주문 완료

**결과**: 실패

**실패 원인**:
- 배송비 계산 로직에서 예외 발생
- 백엔드 로그: `NullPointerException at OrderService.calculateShippingFee`

**스크린샷**:

![주문 오류](screenshots/order-error.png)
*그림 4: 주문 생성 중 오류 화면*

**검증 항목**:
- [x] 상품 선택 및 장바구니 추가
- [x] 배송 정보 입력
- [❌] 주문 금액 계산
- [❌] 주문 완료

**조치사항**:
- [ ] 배송비 계산 로직 수정 필요
- [ ] `ShippingInfo` null 체크 추가
- [ ] 테스트 재실행 예정

---

## 발견된 이슈

### 이슈 1: 배송비 계산 오류
- **심각도**: High
- **상태**: Open
- **설명**: 주문 생성 시 배송비 계산 로직에서 NullPointerException 발생
- **재현 방법**:
  1. 로그인 후 상품 선택
  2. 장바구니에서 주문 진행
  3. 배송 정보 입력 후 주문 완료 클릭
- **예상 원인**: `ShippingInfo` 객체가 null일 경우 처리 미흡
- **수정 필요 파일**: `OrderService.java:calculateShippingFee()`

### 이슈 2: (추가 이슈가 있다면)

---

## 스크린샷 목록

1. `screenshots/signup-form-filled.png` - 회원가입 폼 입력 화면
2. `screenshots/signup-success.png` - 회원가입 성공 메시지
3. `screenshots/signup-duplicate-error.png` - 중복 아이디 오류 메시지
4. `screenshots/login-form.png` - 로그인 폼
5. `screenshots/order-products-list.png` - 상품 목록
6. `screenshots/order-cart.png` - 장바구니
7. `screenshots/order-error.png` - 주문 오류 화면

---

## 테스트 로그

```
Running 4 tests using 1 worker

✓ 회원가입 기능 테스트 > 정상 회원가입 플로우 (2.3s)
✓ 회원가입 기능 테스트 > 중복 아이디 오류 처리 (1.8s)
✓ 로그인 및 인증 테스트 > 로그인 후 JWT 토큰으로 인증된 요청 (1.5s)
✗ 주문 생성 테스트 > 상품 선택 후 주문 생성 (3.2s)

  Error: expect(received).toBeVisible()

  Call log:
    - page.goto(http://localhost:3000/order)
    - page.click(button:has-text("주문 완료"))
    - expect.toBeVisible with timeout 5000ms
    - waiting for selector ".order-success"

  at order.spec.ts:42:5

3 passed, 1 failed (8.8s)
```

---

## 결론 및 권장사항

### 테스트 결과 요약
- 총 4개 테스트 중 3개 통과, 1개 실패
- 주요 기능(회원가입, 로그인)은 정상 동작 확인
- 주문 생성 로직에서 배송비 계산 오류 발견

### 권장사항
1. **즉시 수정 필요**: 배송비 계산 로직의 null 체크 추가
2. **재테스트**: 수정 후 주문 생성 테스트 재실행
3. **추가 테스트 고려**:
   - 다양한 배송지 주소 케이스 테스트
   - 여러 상품 동시 주문 테스트
   - 쿠폰 적용 시나리오 테스트

### PR 승인 여부
- [ ] 승인 가능 (모든 테스트 통과)
- [x] 수정 필요 (이슈 해결 후 재테스트)
- [ ] 승인 불가 (심각한 결함 발견)

### 다음 단계
1. 개발자에게 이슈 전달
2. 수정 사항 코드 리뷰
3. 수정 후 리그레션 테스트 재실행
4. 모든 테스트 통과 시 PR 승인
```

---

## PR 코멘트 자동화

테스트 결과를 GitHub PR에 자동으로 코멘트로 작성하는 방법을 설명합니다.

### PR 코멘트 템플릿 파일 생성

PR 코멘트용 마크다운 파일을 생성하여 재사용합니다.

**pr-comment.md 템플릿**:
```markdown
## 🧪 리그레션 테스트 결과

### 📋 테스트 정보
| 항목 | 내용 |
|------|------|
| **PR** | #{PR번호} {PR 제목} |
| **테스트 일시** | YYYY-MM-DD HH:MM |
| **테스트 실행자** | {이름} |
| **브랜치** | {브랜치명} |

### 📊 테스트 결과 요약

| 테스트 케이스 | 상태 | 소요 시간 | 비고 |
|--------------|------|----------|------|
| {테스트 1} | ✅ 통과 | 2.3s | - |
| {테스트 2} | ✅ 통과 | 1.8s | - |
| {테스트 3} | ❌ 실패 | 3.2s | {원인} |

**전체 결과**: X/Y 통과 (Z%)

### 🔍 상세 내용
- 📄 PR 분석: `regression-tests/pr-results/{PR-이름}/PR-분석.md`
- 📋 테스트 결과: `regression-tests/pr-results/{PR-이름}/테스트-결과.md`
- 📸 스크린샷: `regression-tests/pr-results/{PR-이름}/screenshots/`

### 🐛 발견된 이슈
> 이슈가 없으면 이 섹션을 제거하세요.

1. **이슈 제목**: 설명
   - 심각도: High/Medium/Low
   - 재현 방법: ...

### ✅ 결론

- [x] 승인 가능 (모든 테스트 통과)
- [ ] 수정 필요 (이슈 해결 후 재테스트)
- [ ] 승인 불가 (심각한 결함 발견)

---
*🤖 이 코멘트는 리그레션 테스트 워크플로우에 따라 작성되었습니다.*
```

### GitHub CLI를 이용한 코멘트 작성

#### 기본 사용법
```bash
# 현재 디렉토리의 PR에 코멘트 (브랜치 기준)
gh pr comment --body-file regression-tests/{PR-이름}/pr-comment.md

# PR 번호로 직접 지정
gh pr comment 123 --body-file regression-tests/{PR-이름}/pr-comment.md

# PR URL로 지정
gh pr comment https://github.com/owner/repo/pull/123 --body-file pr-comment.md
```

#### 인라인 코멘트 작성
```bash
# 간단한 테스트 결과 코멘트
gh pr comment 123 --body "## 🧪 테스트 결과: ✅ 모든 테스트 통과 (5/5)"

# 여러 줄 코멘트 (HEREDOC 사용)
gh pr comment 123 --body "$(cat <<'EOF'
## 🧪 리그레션 테스트 결과

✅ **모든 테스트 통과** (5/5)

| 테스트 | 결과 |
|--------|------|
| 로그인 API | ✅ |
| 장바구니 추가 | ✅ |
| 주문 생성 | ✅ |

상세 내용은 `regression-tests/장바구니-리팩토링/테스트-결과.md` 참고
EOF
)"
```

### 자동화 스크립트

PR 코멘트 작성을 자동화하는 스크립트입니다.

**post-pr-comment.sh**:
```bash
#!/bin/bash

# 사용법: ./post-pr-comment.sh <PR번호> <PR폴더명>
# 예시: ./post-pr-comment.sh 123 장바구니-리팩토링

PR_NUMBER=$1
PR_FOLDER=$2

if [ -z "$PR_NUMBER" ] || [ -z "$PR_FOLDER" ]; then
    echo "사용법: $0 <PR번호> <PR폴더명>"
    echo "예시: $0 123 장바구니-리팩토링"
    exit 1
fi

COMMENT_FILE="regression-tests/pr-results/${PR_FOLDER}/pr-comment.md"

if [ ! -f "$COMMENT_FILE" ]; then
    echo "오류: ${COMMENT_FILE} 파일이 존재하지 않습니다."
    exit 1
fi

echo "PR #${PR_NUMBER}에 테스트 결과 코멘트를 작성합니다..."
gh pr comment "$PR_NUMBER" --body-file "$COMMENT_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 코멘트 작성 완료!"
else
    echo "❌ 코멘트 작성 실패"
    exit 1
fi
```

**스크립트 사용법**:
```bash
# 스크립트 실행 권한 부여
chmod +x post-pr-comment.sh

# 실행
./post-pr-comment.sh 123 장바구니-리팩토링
```

### Playwright 테스트 결과 자동 파싱

Playwright 테스트 결과를 자동으로 파싱하여 PR 코멘트를 생성하는 스크립트입니다.

**generate-pr-comment.sh**:
```bash
#!/bin/bash

# 사용법: ./generate-pr-comment.sh <PR폴더명>

PR_FOLDER=$1
PLAYWRIGHT_DIR="regression-tests/playwright-tests"
OUTPUT_FILE="regression-tests/pr-results/${PR_FOLDER}/pr-comment.md"

# Playwright JSON 리포트 생성 (먼저 테스트 실행 필요)
# npx playwright test --reporter=json > test-results.json

# 테스트 결과 파싱 (test-results 디렉토리 기반)
TOTAL_TESTS=$(find "${PLAYWRIGHT_DIR}/test-results" -name "*.png" 2>/dev/null | wc -l)
PASSED_TESTS=$TOTAL_TESTS  # 실패 시 별도 처리 필요

# PR 코멘트 생성
cat > "$OUTPUT_FILE" << EOF
## 🧪 리그레션 테스트 결과

### 📋 테스트 정보
| 항목 | 내용 |
|------|------|
| **테스트 일시** | $(date '+%Y-%m-%d %H:%M') |
| **테스트 폴더** | ${PR_FOLDER} |

### 📊 테스트 결과
- **실행된 테스트**: ${TOTAL_TESTS}개
- **스크린샷**: ${TOTAL_TESTS}개 생성됨

### 🔍 상세 내용
- 📋 테스트 결과: \`regression-tests/pr-results/${PR_FOLDER}/테스트-결과.md\`
- 📸 스크린샷: \`regression-tests/pr-results/${PR_FOLDER}/screenshots/\`

---
*🤖 자동 생성된 코멘트 ($(date '+%Y-%m-%d %H:%M'))*
EOF

echo "✅ PR 코멘트 파일 생성: ${OUTPUT_FILE}"
```

### 기존 코멘트 수정하기

이전에 작성한 코멘트를 수정해야 할 경우:

```bash
# PR의 코멘트 목록 확인
gh pr view 123 --comments

# 특정 코멘트 수정 (코멘트 ID 필요)
gh api repos/{owner}/{repo}/issues/comments/{comment_id} \
  -X PATCH \
  -F body='수정된 내용'

# 또는 새 코멘트로 업데이트 내용 추가
gh pr comment 123 --body "## 🔄 테스트 결과 업데이트

이전 이슈가 수정되어 재테스트를 진행했습니다.

✅ **모든 테스트 통과** (5/5)"
```

---

## PR 승인 전 체크리스트

### 코드 레벨
- [ ] PR 분석 문서 작성 완료
- [ ] 변경된 기능 목록 파악
- [ ] 영향 받는 API 엔드포인트 확인

### 백엔드 테스트
- [ ] 백엔드 빌드 성공
- [ ] 백엔드 서버 정상 실행
- [ ] 단위 테스트 통과 (./gradlew test)
- [ ] API 엔드포인트 정상 응답

### E2E 테스트
- [ ] Playwright 테스트 코드 작성 완료
- [ ] 모든 테스트 케이스 실행
- [ ] 테스트 통과율 100%
- [ ] 스크린샷 캡처 완료

### 문서화
- [ ] PR-분석.md 작성 완료
- [ ] 테스트-결과.md 작성 완료
- [ ] 스크린샷이 문서에 포함됨
- [ ] 발견된 이슈 기록

### PR 코멘트
- [ ] pr-comment.md 작성 완료
- [ ] GitHub PR에 테스트 결과 코멘트 작성
- [ ] 이슈 발견 시 코멘트에 명시

### 최종 확인
- [ ] 기존 기능에 영향 없음 (리그레션 없음)
- [ ] 새로운 기능이 요구사항대로 동작
- [ ] 오류 처리가 적절함
- [ ] 성능 저하 없음
- [ ] 보안 취약점 없음

---

## 트러블슈팅

### 1. Playwright 설치 문제

**증상**: `npx playwright install` 실패

**해결방법**:
```bash
# Node.js 버전 확인 (16 이상 필요)
node --version

# npm 캐시 클리어
npm cache clean --force

# 재설치
npm install -D @playwright/test
npx playwright install
```

### 2. 백엔드 연결 실패

**증상**: 테스트 실행 시 API 호출 실패 (ERR_CONNECTION_REFUSED)

**해결방법**:
```bash
# 백엔드 실행 확인
curl http://localhost:8080/actuator/health

# 백엔드가 실행되지 않았다면
cd store
./gradlew bootRun
```

### 3. 타임아웃 에러

**증상**: `Timeout 30000ms exceeded` 에러

**해결방법**:
```typescript
// playwright.config.ts에서 타임아웃 증가
export default defineConfig({
  timeout: 60000, // 60초로 증가
  expect: {
    timeout: 10000
  }
});

// 또는 특정 테스트에서만
test('느린 테스트', async ({ page }) => {
  test.setTimeout(120000); // 2분
  // 테스트 로직
});
```

### 4. 스크린샷 경로 오류

**증상**: 스크린샷이 저장되지 않음

**해결방법**:
```bash
# screenshots 디렉토리 생성
mkdir -p screenshots

# playwright.config.ts에서 자동 생성되는 경로 확인
# screenshot: 'on' 설정 시 test-results/ 디렉토리에 자동 저장
```

**수동 스크린샷 저장 시**:
```typescript
// 상대 경로 사용
await page.screenshot({ path: 'screenshots/test.png' });

// 절대 경로 사용
import path from 'path';
await page.screenshot({
  path: path.join(__dirname, '../screenshots/test.png')
});
```

### 4-1. 브라우저 UI가 안 보이는 문제

**증상**: 테스트가 실행되지만 브라우저 화면이 보이지 않음

**해결방법**:
```typescript
// playwright.config.ts에서 확인
export default defineConfig({
  use: {
    headless: false,  // 반드시 false로 설정
  },
});
```

**또는 명령어에서 지정**:
```bash
# --headed 옵션 사용
npx playwright test --headed

# package.json에 설정
{
  "scripts": {
    "test": "playwright test --headed"
  }
}
```

### 4-2. 브라우저가 너무 빨리 닫히는 문제

**증상**: 브라우저가 열렸다가 바로 닫혀서 확인하기 어려움

**해결방법**:
```typescript
// 특정 테스트에서 대기 시간 추가
test('테스트', async ({ page }) => {
  // 테스트 로직
  await page.screenshot({ path: 'screenshots/final.png' });

  // 브라우저 유지 (5초)
  await page.waitForTimeout(5000);
});
```

**또는 디버그 모드 사용**:
```bash
# UI 모드로 실행 (단계별 실행 가능)
npx playwright test --ui

# 디버그 모드
npx playwright test --debug
```

### 5. 인증 토큰 만료

**증상**: 인증이 필요한 API 호출 시 401 Unauthorized

**해결방법**:
```typescript
// 각 테스트마다 로그인
test.beforeEach(async ({ page }) => {
  await page.goto('http://localhost:3000/login');
  await page.fill('input[name="username"]', 'testuser');
  await page.fill('input[name="password"]', 'Test1234!');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/'); // 로그인 완료 대기
});

// 또는 storage state 사용
// playwright.config.ts
{
  use: {
    storageState: 'auth.json' // 로그인 상태 재사용
  }
}
```

### 6. QueryDSL 생성 파일 없음

**증상**: 백엔드 빌드 시 Q클래스를 찾을 수 없음

**해결방법**:
```bash
cd store
./gradlew compileQuerydsl
./gradlew build
```

### 7. MySQL 연결 오류

**증상**: `Communications link failure`

**해결방법**:
```bash
# MySQL 실행 확인
mysql -u root -p

# .env 파일 확인
cat .env

# MySQL 재시작 (macOS)
brew services restart mysql
```

### 8. 테스트 간 격리 문제

**증상**: 이전 테스트의 데이터가 다음 테스트에 영향을 줌

**해결방법**:
```typescript
// 각 테스트 후 데이터 정리
test.afterEach(async ({ page }) => {
  // localStorage 클리어
  await page.evaluate(() => localStorage.clear());

  // 쿠키 클리어
  await page.context().clearCookies();
});

// 또는 백엔드에서 테스트 데이터 정리 API 제공
test.afterEach(async ({ request }) => {
  await request.post('http://localhost:8080/api/test/cleanup');
});
```

---

## 참고 자료

### Playwright 공식 문서
- [Playwright 공식 문서](https://playwright.dev/)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [Screenshots](https://playwright.dev/docs/screenshots)
- [API Testing](https://playwright.dev/docs/api-testing)
- [Test Configuration](https://playwright.dev/docs/test-configuration)
- [Debugging Tests](https://playwright.dev/docs/debug)

### Store 프로젝트 관련
- [통합-테스트-스위트-가이드.md](./통합-테스트-스위트-가이드.md) - 통합 테스트 스위트 관리 가이드
- `store/CLAUDE.md` - 프로젝트 아키텍처 및 개발 가이드
- `store/README.md` - 프로젝트 개요

### 관련 도구
- [GitHub Actions for Playwright](https://playwright.dev/docs/ci-intro)
- [Playwright VS Code Extension](https://playwright.dev/docs/getting-started-vscode)
- [Playwright Test Runner](https://playwright.dev/docs/running-tests)

---

## 문서 버전 관리

- **v1.0** (2026-01-07): 초기 문서 작성
- **v1.1** (2026-01-07):
  - 브라우저 UI 표시 (headless: false) 설정 추가
  - 스크린샷 자동 캡처 (screenshot: 'on') 설정 추가
  - API 테스트 예시 추가
  - 스크린샷 명명 규칙 및 저장 가이드 추가
  - 트러블슈팅: 브라우저 UI 관련 문제 해결 방법 추가
- **v1.2** (2026-01-07):
  - 폴더 구조 변경: screenshots 폴더를 PR 루트로 이동
  - 테스트-결과.md와 스크린샷을 함께 관리하도록 구조 개선
  - 스크린샷 정리 프로세스 추가 (test-results → screenshots)
  - 워크플로우 5단계에 스크린샷 정리 과정 추가
  - 스크린샷 자동화 스크립트 예시 추가
- **v1.3** (2026-01-09):
  - PR 코멘트 자동화 섹션 추가 (GitHub CLI 활용)
  - 워크플로우 6단계에 PR 코멘트 작성 과정 추가
  - pr-comment.md 템플릿 추가
  - PR 코멘트 자동화 스크립트 (post-pr-comment.sh, generate-pr-comment.sh) 추가
  - PR 승인 전 체크리스트에 PR 코멘트 항목 추가
- **v1.4** (2026-01-09):
  - 폴더 구조 개편: 통합 테스트 스위트와 PR 결과 분리
    - `playwright-tests/`: 통합 테스트 스위트 (테스트 코드 누적)
    - `pr-results/`: PR별 테스트 결과 문서 및 스크린샷
  - 통합-테스트-스위트-가이드.md 문서 분리 및 상호 참조 추가
  - 워크플로우 단계 경로 업데이트
  - PR 태그 (`@pr-{번호}`) 활용 가이드 추가
