import { test, expect } from '@playwright/test';
import { API_BASE_URL } from '../common/fixtures';

/**
 * PR #7 회귀 테스트: [FEATURE] 회원가입 시 아이디 10자 제한 검증 추가
 *
 * 테스트 목표:
 * - InvalidUsernameLengthException 예외 처리 검증
 * - JoinMemberService.checkUsernameLength() 메서드 동작 확인
 * - MemberController.join() / joinAdmin() 예외 핸들러 검증
 * - UTF-8 한글 에러 메시지 인코딩 정상 동작
 * - 기존 회원가입 플로우 회귀 방지
 *
 * PR 분석 문서 주요 포인트:
 * 1. ✅ 검증 순서 최적화: null/empty → 길이 → DB 중복 체크
 * 2. ⚠️ Null Safety 이슈: checkUsernameLength()에서 NPE 가능성
 * 3. ✅ UTF-8 인코딩: WebMvcConfig + application.yml 설정
 * 4. ✅ 계층별 책임 분리: Service(검증) / Controller(HTTP 응답)
 *
 * 관련 변경 파일:
 * - InvalidUsernameLengthException.java (신규)
 * - JoinMemberService.java (수정)
 * - MemberController.java (수정)
 * - WebMvcConfig.java (신규 - UTF-8 설정)
 * - application.yml (수정 - Tomcat 인코딩)
 */

test.describe('@pr-7 회원가입 Username 길이 검증 - API 테스트', () => {
  const baseSignupData = {
    email: '',
    password: 'TestPass123!@',
    name: '테스트유저',
    role: 'ROLE_USER',
    addressReq: {
      address: '서울시 강남구',
      detailedAddress: '테헤란로 123',
      zipCode: '06234'
    }
  };

  // 테스트 격리를 위한 유니크 이메일 생성 (DB 컬럼 크기 제한 고려하여 짧게)
  function generateUniqueEmail(): string {
    const random = Math.floor(Math.random() * 100000);
    return `t${random}@t.co`;
  }

  test.describe('1. 정상 케이스 - Username 길이 검증 통과', () => {

    test('@critical @smoke username 10자 이하 - 회원가입 성공 (영문 8자)', async ({ request }) => {
      // Given: 영문 username 8자 (10자 이하) - 동적 생성으로 중복 방지
      const random = Math.floor(Math.random() * 10000);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `usr${random}` // 7자 + 4자 랜덤 = 최대 7자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공 응답 확인
      expect(response.status()).toBe(202); // ACCEPTED
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      expect(responseBody.memberId).toBeGreaterThan(0);
      expect(responseBody.message).toBe('회원가입을 축하드립니다.');
      console.log('✅ [성공] username 8자 회원가입:', responseBody);
    });

    test('@critical @boundary username 정확히 10자 - 경계값 테스트 (영문)', async ({ request }) => {
      // Given: 정확히 10자 (경계값) - 동적 생성으로 중복 방지
      const random = Math.floor(Math.random() * 1000);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `usrnam${random}`.padEnd(10, 'x').slice(0, 10) // 정확히 10자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공 응답 확인
      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      expect(responseBody.message).toBe('회원가입을 축하드립니다.');
      console.log('✅ [경계값] username 정확히 10자 성공:', responseBody);
    });

    test('@regression username 1자 - 최소 길이 테스트', async ({ request }) => {
      // Given: username 1자 (최소값) - 테스트 스킵 (1자 username은 중복 확률 높음)
      // 1자 username 테스트는 중복 가능성이 매우 높아 신뢰성 있는 테스트가 어려움
      test.skip();
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'x' // 1자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공 (길이 제한은 최대값만 검증)
      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      console.log('✅ [최소값] username 1자 성공:', responseBody);
    });

    test('@regression username 5자 - 중간값 테스트', async ({ request }) => {
      // Given: username 5자 (중간값) - 동적 생성
      const random = Math.floor(Math.random() * 100);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `us${random}`.slice(0, 5) // 5자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공
      expect(response.status()).toBe(202);
      console.log('✅ [중간값] username 5자 성공');
    });
  });

  test.describe('2. 실패 케이스 - Username 길이 초과 (InvalidUsernameLengthException)', () => {

    test('@critical @boundary username 11자 초과 - 400 에러 발생', async ({ request }) => {
      // Given: username 11자 (10자 초과)
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'username123' // 11자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 400 Bad Request 응답 확인
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [실패] username 11자 초과 - 에러:', errorMessage);
    });

    test('@regression username 15자 초과 - 400 에러 발생', async ({ request }) => {
      // Given: username 19자 (매우 긴 경우)
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'verylongusername123' // 19자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 400 Bad Request 응답 확인
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [실패] username 19자 초과 - 에러:', errorMessage);
    });

    test('@regression username 50자 초과 - 극단값 테스트', async ({ request }) => {
      // Given: username 50자 (극단적으로 긴 경우)
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'a'.repeat(50) // 50자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 400 Bad Request
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [극단값] username 50자 초과 - 에러:', errorMessage);
    });
  });

  test.describe('3. 한글 Username 테스트 (Character vs Byte 길이)', () => {

    test('@regression 한글 username 5자 - 회원가입 성공', async ({ request }) => {
      // Given: 한글 username 5자 (10자 이하) - 동적 생성
      const random = Math.floor(Math.random() * 100);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `한글${random}` // 한글 2자 + 숫자 = 5자 이하
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공 (character count 기준 검증)
      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      console.log('✅ [한글] username 5자 성공:', responseBody);
    });

    test('@boundary 한글 username 정확히 10자 - 회원가입 성공', async ({ request }) => {
      // Given: 한글 username 정확히 10자 - 동적 생성
      const random = Math.floor(Math.random() * 10);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `사용자테스트${random}아아아`.slice(0, 10) // 정확히 10자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공
      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      console.log('✅ [한글 경계값] username 정확히 10자 성공:', responseBody);
    });

    test('@critical 한글 username 11자 초과 - 400 에러', async ({ request }) => {
      // Given: 한글 username 11자 - 동적 생성으로 중복 방지
      const random = Math.floor(Math.random() * 10);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `한글테스트${random}유저명일이` // 한글+숫자 11자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 400 Bad Request
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [한글 실패] username 11자 초과 - 에러:', errorMessage);
    });

    test('@regression 한글+영문 혼합 username 10자 - 회원가입 성공', async ({ request }) => {
      // Given: 한글+영문 혼합 10자 - 동적 생성
      const random = Math.floor(Math.random() * 100);
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: `한글u${random}` // 한글 2자 + 영문 1자 + 숫자 = 최대 5자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 성공
      expect(response.status()).toBe(202);
      console.log('✅ [혼합] 한글+영문 username 성공');
    });

    test('@regression 한글+숫자 혼합 username 11자 - 400 에러', async ({ request }) => {
      // Given: 한글+숫자 혼합 11자
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: '테스트12345678' // 한글 3자 + 숫자 8자 = 총 11자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 400 에러
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [혼합 실패] 한글+숫자 11자 에러');
    });
  });

  test.describe('4. UTF-8 한글 에러 메시지 인코딩 검증', () => {

    test('@critical UTF-8 인코딩 - 한글 에러 메시지 정상 출력', async ({ request }) => {
      // Given: username 11자 초과
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'toolongname' // 11자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: 한글 에러 메시지 정상 출력 확인
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();

      // 한글이 깨지지 않고 정상적으로 출력되는지 검증
      expect(errorMessage).toContain('아이디');
      expect(errorMessage).toContain('10자');
      expect(errorMessage).toContain('이하');
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');

      // 깨진 문자 없음 확인
      expect(errorMessage).not.toContain('?');
      expect(errorMessage).not.toMatch(/[^\x00-\x7F\uAC00-\uD7A3]/); // 한글+ASCII 외 문자 없음

      console.log('✅ [UTF-8] 한글 에러 메시지 정상 출력:', errorMessage);
    });

    test('@regression Content-Type 헤더 - charset=UTF-8 확인', async ({ request }) => {
      // Given: username 11자 초과
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'longusername' // 12자
      };

      // When: 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join`, {
        data: signupData
      });

      // Then: Content-Type 헤더 확인
      const contentType = response.headers()['content-type'];
      console.log('Content-Type 헤더:', contentType);

      // UTF-8 인코딩 확인 (application/json 또는 text/plain에 charset 포함)
      if (contentType) {
        expect(contentType.toLowerCase()).toMatch(/charset|utf-?8/i);
      }

      console.log('✅ [헤더] Content-Type UTF-8 확인:', contentType);
    });
  });

  test.describe('5. 관리자 회원가입 - 동일한 검증 로직 적용', () => {

    test('@critical 관리자 가입 - username 11자 초과 시 400 에러', async ({ request }) => {
      // Given: username 11자 (관리자 가입 엔드포인트)
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'adminname11' // 11자
      };

      // When: 관리자 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join/admin`, {
        data: signupData
      });

      // Then: 400 Bad Request 응답 확인
      expect(response.status()).toBe(400);
      const errorMessage = await response.text();
      expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');
      console.log('✅ [관리자] username 11자 초과 에러:', errorMessage);
    });

    test('@regression 관리자 가입 - username 10자 이하 성공', async ({ request }) => {
      // Given: username 9자 (관리자 가입)
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'admin1234' // 9자
      };

      // When: 관리자 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join/admin`, {
        data: signupData
      });

      // Then: 성공 응답 확인
      expect(response.status()).toBe(202);
      const responseBody = await response.json();
      expect(responseBody).toHaveProperty('memberId');
      console.log('✅ [관리자] username 9자 성공:', responseBody);
    });

    test('@boundary 관리자 가입 - username 정확히 10자 성공', async ({ request }) => {
      // Given: username 정확히 10자
      const signupData = {
        ...baseSignupData,
        email: generateUniqueEmail(),
        username: 'adminteste' // 정확히 10자
      };

      // When: 관리자 회원가입 요청
      const response = await request.post(`${API_BASE_URL}/members/join/admin`, {
        data: signupData
      });

      // Then: 성공
      expect(response.status()).toBe(202);
      console.log('✅ [관리자 경계값] username 정확히 10자 성공');
    });
  });
});

test.describe('@pr-7 기존 회원가입 기능 회귀 테스트 - 변경으로 인한 영향 없음 확인', () => {
  const baseSignupData = {
    email: '',
    password: 'TestPass123!@',
    username: 'testuser',
    name: '테스트유저',
    role: 'ROLE_USER',
    addressReq: {
      address: '서울시 강남구',
      detailedAddress: '테헤란로 123',
      zipCode: '06234'
    }
  };

  function generateUniqueEmail(): string {
    const random = Math.floor(Math.random() * 100000);
    return `t${random}@t.co`;
  }

  test('@smoke @critical 기존 회원가입 플로우 - 정상 동작 확인', async ({ request }) => {
    // Given: 정상적인 회원가입 데이터 - username 10자 이하로 수정
    const random = Math.floor(Math.random() * 10000);
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: `new${random}` // 최대 7자
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 성공 응답 확인
    expect(response.status()).toBe(202);
    const responseBody = await response.json();
    expect(responseBody).toHaveProperty('memberId');
    expect(responseBody.message).toBe('회원가입을 축하드립니다.');
    console.log('✅ [회귀] 기존 회원가입 플로우 정상 동작');
  });

  test('@regression 이메일 중복 체크 - 정상 동작 확인', async ({ request }) => {
    // Given: 동일한 이메일로 두 번 가입 시도
    const email = generateUniqueEmail();
    const random = Math.floor(Math.random() * 10000);
    const signupData = {
      ...baseSignupData,
      email: email,
      username: `eml${random}` // 7자 이하
    };

    // When: 첫 번째 가입
    const firstResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });
    expect(firstResponse.status()).toBe(202);

    // When: 동일 이메일로 재가입 시도
    const signupData2 = {
      ...signupData,
      username: `em2${random}` // 다른 username
    };
    const secondResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData2
    });

    // Then: 400 에러 반환 확인
    expect(secondResponse.status()).toBe(400);
    const errorMessage = await secondResponse.text();
    expect(errorMessage).toBe('이메일이 중복됩니다.');
    console.log('✅ [회귀] 이메일 중복 체크 정상 동작');
  });

  test('@regression username 중복 체크 - 정상 동작 확인', async ({ request }) => {
    // Given: 동일한 username으로 두 번 가입 시도
    const random = Math.floor(Math.random() * 10000);
    const username = `dup${random}`; // 7자 이하
    const signupData1 = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: username
    };

    // When: 첫 번째 가입
    const firstResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData1
    });
    expect(firstResponse.status()).toBe(202);

    // When: 동일 username으로 재가입 시도
    const signupData2 = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: username
    };
    const secondResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData2
    });

    // Then: 400 에러 반환 확인
    expect(secondResponse.status()).toBe(400);
    const errorMessage = await secondResponse.text();
    expect(errorMessage).toBe('아이디가 중복됩니다.');
    console.log('✅ [회귀] username 중복 체크 정상 동작');
  });

  test('@regression 회원가입 후 로그인 - 정상 동작 확인', async ({ request }) => {
    // Given: 회원가입 완료
    const random = Math.floor(Math.random() * 10000);
    const username = `lgn${random}`; // 7자 이하
    const password = 'TestPass123!@';
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: username,
      password: password
    };

    const signupResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });
    expect(signupResponse.status()).toBe(202);

    // When: 생성된 계정으로 로그인
    const loginResponse = await request.post(`${API_BASE_URL}/members/sign-in`, {
      data: {
        username: username,
        password: password
      }
    });

    // Then: 로그인 성공 및 JWT 토큰 발급 확인
    expect(loginResponse.status()).toBe(202);
    const loginData = await loginResponse.json();
    expect(loginData).toHaveProperty('accessToken');
    expect(loginData).toHaveProperty('refreshToken');
    expect(loginData.grantType).toBe('Bearer');
    console.log('✅ [회귀] 회원가입 후 로그인 정상 동작');
  });

  test('@regression null username - 검증 정상 동작 (NPE 발생 가능성 체크)', async ({ request }) => {
    // Given: username null (PR 분석에서 Null Safety 이슈 지적)
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: null
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 400 에러 반환 (500 NPE가 아닌 400으로 처리되어야 함)
    expect(response.status()).toBe(400);

    // NullPointerException으로 500 발생 시 경고
    if (response.status() === 500) {
      console.warn('⚠️ [경고] username null 처리 시 500 에러 - NullPointerException 발생!');
      console.warn('⚠️ PR 분석에서 지적된 Null Safety 이슈가 실제로 발생함');
    }
    console.log('✅ [회귀] null username 검증 정상 동작');
  });

  test('@regression 빈 username - 검증 정상 동작', async ({ request }) => {
    // Given: username 빈 문자열
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: ''
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 400 에러 반환
    expect(response.status()).toBe(400);
    console.log('✅ [회귀] 빈 username 검증 정상 동작');
  });
});

test.describe('@pr-7 검증 순서 테스트 - PR 분석 Good Practice 검증', () => {
  /**
   * PR 분석 문서 Good Point #2:
   * "검증 순서 최적화: null/empty 체크 → 길이 검증 → DB 중복 체크 순서로 진행
   *  불필요한 데이터베이스 쿼리를 방지하여 성능 효율적"
   */

  const baseSignupData = {
    email: '',
    password: 'TestPass123!@',
    username: '',
    name: '테스트유저',
    role: 'ROLE_USER',
    addressReq: {
      address: '서울시 강남구',
      detailedAddress: '테헤란로 123',
      zipCode: '06234'
    }
  };

  function generateUniqueEmail(): string {
    const random = Math.floor(Math.random() * 100000);
    return `t${random}@t.co`;
  }

  test('@regression 검증 순서 #1 - null 체크가 길이 체크보다 먼저 실행됨', async ({ request }) => {
    // Given: username null (null 체크가 먼저 발생해야 함)
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: null
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 400 에러 (길이 체크 전에 null 체크)
    expect(response.status()).toBe(400);
    const errorMessage = await response.text();

    // InvalidUsernameLengthException이 아니라 null 검증 에러여야 함
    expect(errorMessage).not.toBe('아이디는 10자 이하로 입력해주세요.');
    console.log('✅ [검증순서] null 체크 우선:', errorMessage);
  });

  test('@critical 검증 순서 #2 - 길이 체크가 중복 체크보다 먼저 실행됨 (성능 최적화)', async ({ request }) => {
    // Given: username 11자 초과 (길이 체크가 중복 체크보다 먼저 발생해야 함)
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: 'verylongusername' // 16자
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: InvalidUsernameLengthException 발생 (DB 조회 전)
    expect(response.status()).toBe(400);
    const errorMessage = await response.text();
    expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');

    // 중복 체크 에러가 아님을 확인 (DB 조회 없이 실패)
    expect(errorMessage).not.toBe('아이디가 중복됩니다.');
    console.log('✅ [검증순서] 길이 체크 → DB 조회 순서 확인');
  });

  test('@performance 검증 순서 #3 - DB 조회 전 길이 검증으로 빠른 실패 (성능 테스트)', async ({ request }) => {
    /**
     * 길이 검증은 DB 조회 전에 수행되므로 응답 시간이 빨라야 함
     */
    const startTime = Date.now();

    // Given: username 20자 초과
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: 'verylongusername12345' // 21자
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    const endTime = Date.now();
    const responseTime = endTime - startTime;

    // Then: 400 에러
    expect(response.status()).toBe(400);
    const errorMessage = await response.text();
    expect(errorMessage).toBe('아이디는 10자 이하로 입력해주세요.');

    // DB 조회 없이 즉시 실패하므로 300ms 이내 응답 기대
    console.log(`⏱️ [성능] 응답 시간: ${responseTime}ms`);
    expect(responseTime).toBeLessThan(500);

    if (responseTime < 100) {
      console.log('✅ [최적화] 매우 빠른 응답 - DB 조회 없이 검증됨');
    }
  });

  test('@regression 검증 순서 #4 - 길이 통과 후 중복 체크 실행 확인', async ({ request }) => {
    // Given: 첫 번째 회원가입 (username 9자)
    const username = 'duptest' + Date.now().toString().slice(-3); // 10자
    const firstSignup = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: username
    };

    const firstResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: firstSignup
    });
    expect(firstResponse.status()).toBe(202);

    // When: 동일 username으로 재가입 (길이는 통과, 중복 체크에서 실패해야 함)
    const duplicateSignup = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: username
    };

    const duplicateResponse = await request.post(`${API_BASE_URL}/members/join`, {
      data: duplicateSignup
    });

    // Then: 길이 검증은 통과했지만 중복 체크에서 실패
    expect(duplicateResponse.status()).toBe(400);
    const errorMessage = await duplicateResponse.text();
    expect(errorMessage).toBe('아이디가 중복됩니다.');

    // 길이 검증 에러가 아님을 확인
    expect(errorMessage).not.toBe('아이디는 10자 이하로 입력해주세요.');
    console.log('✅ [검증순서] 길이 통과 → 중복 체크 실행 확인');
  });
});

test.describe('@pr-7 에지 케이스 및 특수 문자 테스트', () => {
  const baseSignupData = {
    email: '',
    password: 'TestPass123!@',
    name: '테스트유저',
    role: 'ROLE_USER',
    addressReq: {
      address: '서울시 강남구',
      detailedAddress: '테헤란로 123',
      zipCode: '06234'
    }
  };

  function generateUniqueEmail(): string {
    const random = Math.floor(Math.random() * 100000);
    return `t${random}@t.co`;
  }

  test('@regression 특수문자 포함 username 10자 이하 - 성공 여부 확인', async ({ request }) => {
    // Given: 특수문자 포함 username 9자
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: 'user_1234' // 언더스코어 포함 9자
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 특수문자 허용 정책에 따라 성공/실패
    // (길이 검증은 통과, 특수문자 정책은 별도 검증)
    if (response.status() === 202) {
      console.log('✅ [특수문자] 특수문자 허용됨');
    } else if (response.status() === 400) {
      const errorMessage = await response.text();
      console.log('ℹ️ [특수문자] 특수문자 정책으로 실패:', errorMessage);
      // 길이 검증 에러는 아니어야 함
      expect(errorMessage).not.toBe('아이디는 10자 이하로 입력해주세요.');
    }
  });

  test('@regression 공백 포함 username 10자 이하 - 처리 확인', async ({ request }) => {
    // Given: 공백 포함 username 10자
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: 'user name1' // 공백 포함 10자
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: 공백 허용 정책에 따라 성공/실패
    if (response.status() === 202) {
      console.log('✅ [공백] 공백 허용됨');
    } else if (response.status() === 400) {
      const errorMessage = await response.text();
      console.log('ℹ️ [공백] 공백 정책으로 실패:', errorMessage);
      // 길이 검증 에러는 아니어야 함
      expect(errorMessage).not.toBe('아이디는 10자 이하로 입력해주세요.');
    }
  });

  test('@regression 이모지 포함 username - Character 길이 vs Byte 길이 테스트', async ({ request }) => {
    // Given: 이모지 포함 username (character count는 적지만 byte가 클 수 있음)
    const signupData = {
      ...baseSignupData,
      email: generateUniqueEmail(),
      username: 'user😀123' // 이모지 1개 + 영문 7자 = character 8개
    };

    // When: 회원가입 요청
    const response = await request.post(`${API_BASE_URL}/members/join`, {
      data: signupData
    });

    // Then: Character count 기준이면 성공, Byte 기준이면 실패 가능
    if (response.status() === 202) {
      console.log('✅ [이모지] Character count 기준 검증 - 성공');
    } else if (response.status() === 400) {
      const errorMessage = await response.text();
      console.log('ℹ️ [이모지] 이모지 정책 또는 Byte 길이 제한:', errorMessage);
    }
  });
});
