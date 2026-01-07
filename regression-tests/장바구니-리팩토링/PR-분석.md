# PR 분석: 장바구니 리팩토링

## PR 정보
- **PR 번호**: #1
- **작성자**: twkim5235
- **작성일**: 2026-01-07
- **브랜치**: develop-refactoring-cart → main
- **PR 링크**: https://github.com/twkim5235/regression_test_work_flow/pull/1

## 변경사항 요약
장바구니 및 주문 관련 API를 리팩토링하여, 기존에 클라이언트가 memberId를 파라미터로 전달하던 방식을 제거하고, Spring Security의 Authentication 객체를 통해 현재 로그인한 사용자 정보를 자동으로 추출하도록 개선했습니다. 이를 통해 보안을 강화하고, 다른 사용자의 장바구니나 주문에 접근하는 것을 원천적으로 차단했습니다.

## 변경된 기능

### 1. 장바구니 조회
- **파일**: `src/main/java/com/example/ddd_start/order/presentation/CartController.java`
- **변경 내용**:
  - **Before**: `GET /carts?memberId={memberId}` - 클라이언트가 memberId를 파라미터로 전달
  - **After**: `GET /carts` - Authentication 객체에서 username 추출하여 장바구니 조회
  - **보안 개선**: 타 사용자의 memberId를 임의로 조회하는 것 방지

### 2. 장바구니 추가
- **파일**: `src/main/java/com/example/ddd_start/order/presentation/CartController.java`
- **변경 내용**:
  - **Before**: `POST /carts` 요청 바디에 `memberId` 포함
  - **After**: `POST /carts` 요청 바디에서 `memberId` 제거, Authentication 사용
  - **로직 변경**: `CartService.saveByUsername()` 새로운 메서드 추가

### 3. 장바구니 전체 삭제
- **파일**: `src/main/java/com/example/ddd_start/order/presentation/CartController.java`
- **변경 내용**:
  - **Before**: `DELETE /carts-all?memberId={memberId}`
  - **After**: `DELETE /carts-all` - Authentication 사용
  - **로직 변경**: `CartService.deleteAllByUsername()` 새로운 메서드 추가

### 4. 내 주문 조회
- **파일**: `src/main/java/com/example/ddd_start/order/presentation/OrderController.java`
- **변경 내용**:
  - **Before**: `GET /orders/my-order?memberId={memberId}`
  - **After**: `GET /orders/my-order` - Authentication 사용
  - **예외 처리**: `NoMemberFoundException` 추가하여 회원 정보 없을 때 400 Bad Request 반환

### 5. CartDto에 productId 추가
- **파일**: `src/main/java/com/example/ddd_start/order/application/service/CartService.java`
- **변경 내용**:
  - CartDto 생성 시 `productId` 필드 추가
  - 프론트엔드에서 장바구니 상품 관리 시 productId 활용 가능

## 영향 받는 API 엔드포인트

| 메서드 | 엔드포인트 | 변경 내용 | 인증 필요 |
|--------|-----------|----------|----------|
| GET | /carts | memberId 파라미터 제거, Authentication 사용 | ✅ |
| POST | /carts | 요청 바디에서 memberId 제거, Authentication 사용 | ✅ |
| DELETE | /carts-all | memberId 파라미터 제거, Authentication 사용 | ✅ |
| GET | /orders/my-order | memberId 파라미터 제거, Authentication 사용 | ✅ |

## 테스트 범위

### API 레벨 테스트
- [ ] 인증된 사용자가 자신의 장바구니 조회 (GET /carts)
- [ ] 인증된 사용자가 장바구니에 상품 추가 (POST /carts)
- [ ] 인증된 사용자가 자신의 장바구니 전체 삭제 (DELETE /carts-all)
- [ ] 인증된 사용자가 자신의 주문 조회 (GET /orders/my-order)
- [ ] 인증 없이 장바구니/주문 API 호출 시 401 Unauthorized 반환
- [ ] CartDto에 productId 포함 여부 확인

### E2E 테스트 (프론트엔드 연동)
- [ ] 로그인 후 장바구니 페이지 접속 및 조회
- [ ] 상품 상세 페이지에서 장바구니 추가 버튼 클릭
- [ ] 장바구니 페이지에서 전체 삭제 버튼 클릭
- [ ] 마이페이지에서 내 주문 목록 조회
- [ ] 로그아웃 상태에서 장바구니 접근 시 로그인 페이지 리다이렉트

## 예상되는 UI 변화

프론트엔드 관점에서 다음과 같은 변화가 예상됩니다:
1. **API 요청 변경**:
   - 기존에 `memberId`를 파라미터나 요청 바디에 포함하던 코드 제거
   - JWT 토큰을 `Authorization: Bearer {token}` 헤더에 포함하여 요청

2. **응답 데이터 변경**:
   - CartDto에 `productId` 필드 추가로 인해 프론트엔드에서 상품 정보 활용 용이

3. **에러 핸들링**:
   - 401 Unauthorized: 로그인 페이지로 리다이렉트
   - 400 Bad Request (회원 정보 없음): 적절한 에러 메시지 표시

## 추가 고려사항

### 보안
- ✅ **권한 상승 공격 방지**: 타 사용자의 memberId를 임의로 전달하여 정보 조회하는 공격 차단
- ✅ **인증 강제**: 모든 장바구니/주문 API에 인증 필수 적용
- ⚠️ **CORS 설정**: 프론트엔드 도메인에서 인증 헤더를 포함한 요청 가능하도록 CORS 설정 확인 필요

### 프론트엔드 호환성
- ⚠️ **Breaking Change**: 기존 프론트엔드 코드에서 memberId를 전달하던 부분 수정 필요
- ✅ **JWT 토큰 관리**: 로그인 후 localStorage/sessionStorage에 토큰 저장 및 API 요청 시 헤더 포함

### 백엔드 로직
- ✅ **새로운 Service 메서드 추가**: `saveByUsername`, `printAllCartsByUsername`, `deleteAllByUsername`
- ✅ **MemberRepository 사용**: username으로 회원 조회 로직 추가
- ⚠️ **성능**: username으로 회원 조회 시 매번 DB 쿼리 발생 - 캐싱 고려 가능

### 테스트
- ⚠️ **프론트엔드 테스트 업데이트 필요**: 기존 E2E 테스트에서 memberId 전달 부분 수정
- ✅ **백엔드 단위 테스트**: CartService, OrderService의 새로운 메서드 테스트 추가 권장
