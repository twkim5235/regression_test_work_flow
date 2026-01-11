# PR 분석 보고서

## 📋 PR 개요
| 항목 | 내용 |
|------|------|
| PR 제목 | Develop refactoring cart |
| PR 번호 | #1 |
| 작성자 | twkim5235 |
| 대상 브랜치 | main ← develop-refactoring-cart |
| 변경 파일 수 | 22개 (Java 소스: 6개) |
| 추가/삭제 라인 | +2,305 / -22 |
| 상태 | Merged (2026-01-07) |
| PR 링크 | https://github.com/twkim5235/regression_test_work_flow/pull/1 |

## 🎯 변경 요약

이 PR은 **장바구니 및 주문 관련 API의 보안 강화**를 목표로 하는 리팩토링 작업입니다. 기존에 클라이언트가 `memberId`를 파라미터나 요청 바디에 포함하여 전달하던 방식을 제거하고, Spring Security의 `Authentication` 객체를 통해 현재 로그인한 사용자 정보를 서버에서 자동으로 추출하도록 개선했습니다.

**핵심 보안 개선 사항:**
- IDOR (Insecure Direct Object Reference) 취약점 제거
- 권한 상승 공격 방지 (타 사용자의 memberId 임의 조작 차단)
- 인증 강제 적용으로 인가 로직 강화

## 📁 변경된 파일 분석

### 1. `src/main/java/com/example/ddd_start/order/presentation/CartController.java`
- **변경 유형**: 수정 (핵심 변경)
- **변경 내용**:
  - `GET /carts`: `@RequestParam Long memberId` 제거 → `Authentication authentication` 추가
  - `POST /carts`: 요청 바디의 `memberId` 제거, `Authentication` 파라미터 추가
  - `DELETE /carts-all`: `@RequestParam Long memberId` 제거 → `Authentication authentication` 추가
  - 모든 메서드에서 `authentication.getName()`으로 username 추출하여 서비스 레이어로 전달
- **영향도**: 높음 (Breaking Change - 클라이언트 코드 수정 필수)

### 2. `src/main/java/com/example/ddd_start/order/application/service/CartService.java`
- **변경 유형**: 수정 및 메서드 추가
- **변경 내용**:
  - `saveByUsername(String username, Long productId, Integer quantity)` 메서드 추가
  - `printAllCartsByUsername(String username)` 메서드 추가
  - `deleteAllByUsername(String username)` 메서드 추가
  - `CartDto`에 `productId` 필드 추가 (기존 DTO 확장)
  - username으로 Member 조회 후 기존 memberId 기반 메서드 호출하는 위임 패턴
- **영향도**: 중간 (기존 메서드 유지, 새 메서드 추가)

### 3. `src/main/java/com/example/ddd_start/order/presentation/OrderController.java`
- **변경 유형**: 수정
- **변경 내용**:
  - `GET /orders/my-order`: `@RequestParam Long memberId` 제거 → `Authentication authentication` 추가
  - `OrderService.findMyOrdersByUsername()` 호출로 변경
- **영향도**: 높음 (Breaking Change)

### 4. `src/main/java/com/example/ddd_start/order/application/service/OrderService.java`
- **변경 유형**: 수정 및 메서드 추가
- **변경 내용**:
  - `findMyOrdersByUsername(String username)` 메서드 추가
  - Member 존재 여부 검증 로직 추가 (`NoMemberFoundException`)
- **영향도**: 중간

### 5. `src/main/java/com/example/ddd_start/order/application/model/CartDto.java`
- **변경 유형**: 수정
- **변경 내용**:
  - `productId` 필드 추가
  - 프론트엔드에서 장바구니 상품 관리 시 productId 직접 활용 가능
- **영향도**: 낮음 (필드 추가, 하위 호환성 유지 가능)

### 6. `src/main/java/com/example/ddd_start/auth/SecurityConfig.java`
- **변경 유형**: 수정
- **변경 내용**:
  - 장바구니/주문 관련 엔드포인트의 인증 요구사항 강화
  - `/carts/**`, `/orders/my-order` 엔드포인트에 `.authenticated()` 명시적 적용
- **영향도**: 높음 (보안 정책 변경)

### 7. `src/main/resources/application.yml`
- **변경 유형**: 추가
- **변경 내용**: 설정 추가 (구체적 내용 미확인)
- **영향도**: 낮음

### 8. `build.gradle`
- **변경 유형**: 수정
- **변경 내용**: 의존성 추가 (1줄 추가)
- **영향도**: 낮음

## ✅ 잘된 점 (Good)

1. **보안 강화 - IDOR 취약점 제거**
   - 사용자가 타인의 memberId를 임의로 조작하여 다른 사용자의 장바구니/주문을 조회하는 공격 원천 차단
   - Spring Security Authentication을 활용한 서버 사이드 검증

2. **명확한 인증/인가 분리**
   - Controller 레벨에서 Authentication 객체 주입
   - Service 레이어는 username을 통한 비즈니스 로직 처리
   - 계층별 책임 분리 명확

3. **기존 코드 재활용**
   - 기존 memberId 기반 메서드 유지하면서 username 기반 메서드 추가
   - Facade 패턴으로 코드 중복 최소화

4. **점진적 마이그레이션 가능**
   - 기존 내부 메서드(`printAllCarts(Long memberId)`)는 유지
   - 새로운 public API 메서드만 추가하여 하위 호환성 고려

5. **명시적인 예외 처리**
   - `NoMemberFoundException`을 통한 회원 정보 없을 때 명확한 에러 응답

## ⚠️ 개선 필요 사항 (Improvements)

1. **성능 고려 - 중복 DB 조회**
   - 모든 요청마다 `memberRepository.findMemberByUsername()` 호출
   - 제안: Spring Security의 `@AuthenticationPrincipal`과 커스텀 UserDetails로 Member 정보 캐싱
   ```java
   // 개선 예시
   public List<CartDto> printAllCarts(@AuthenticationPrincipal CustomUserDetails userDetails) {
       Long memberId = userDetails.getMemberId(); // DB 조회 없이 바로 사용
       return cartService.printAllCarts(memberId);
   }
   ```

2. **에러 처리 일관성**
   - 현재 `IllegalArgumentException("회원 정보를 찾을 수 없습니다.")` 사용
   - 제안: 커스텀 예외(`MemberNotFoundException`) 생성하여 일관된 에러 응답 제공

3. **API 응답 DTO 개선**
   - `AddCartResponse`는 존재하지만, 다른 API는 단순 문자열 응답
   - 제안: 모든 API에 대해 일관된 응답 DTO 구조 적용
   ```java
   record ApiResponse<T>(T data, String message, int code) {}
   ```

4. **테스트 코드 누락**
   - 새로 추가된 메서드(`saveByUsername`, `printAllCartsByUsername` 등)에 대한 단위 테스트 미존재
   - 제안: CartServiceTest, OrderServiceTest에 테스트 케이스 추가

5. **문서화 부족**
   - API 스펙 변경에 대한 Swagger/OpenAPI 문서 업데이트 필요
   - 클라이언트 개발자를 위한 마이그레이션 가이드 필요

## 🚨 주의 사항 (Concerns)

1. **Breaking Change - 프론트엔드 영향**
   - **Critical**: 모든 장바구니/주문 API 호출 코드 수정 필수
   - 변경 전: `GET /carts?memberId=123`
   - 변경 후: `GET /carts` (JWT 토큰을 `Authorization: Bearer {token}` 헤더에 포함)
   - 요청 바디에서 `memberId` 필드 제거 필요

2. **인증 없는 요청 처리**
   - 인증 없이 API 호출 시 `401 Unauthorized` 반환
   - 프론트엔드에서 토큰 만료 시 자동 로그인 페이지 리다이렉션 로직 필요

3. **CORS 설정 확인 필요**
   - `Authorization` 헤더를 포함한 크로스 오리진 요청 허용 여부 확인
   - `SecurityConfig`에서 CORS 설정 검증 필요

4. **세션 vs JWT 일관성**
   - Spring Security가 Stateless JWT 기반인지 확인
   - `SecurityConfig`에서 `.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)` 설정 확인 필요

5. **회원 삭제 시 cascade 영향**
   - username 기반 조회 시 삭제된 회원 처리 로직 확인 필요

## 🔍 상세 코드 리뷰

### 이슈 1: 중복 Member 조회로 인한 성능 저하

**위치**: `CartService.java:51`, `CartService.java:72`, `CartService.java:107`

**심각도**: Major

**설명**:
매 요청마다 `memberRepository.findMemberByUsername(username)`을 호출하여 동일한 Member 엔티티를 조회합니다. 하나의 요청에서 여러 서비스 메서드를 호출할 경우 중복 조회가 발생할 수 있습니다.

**제안**:
Spring Security의 UserDetails에 Member 정보를 포함시켜 DB 조회 최소화

```java
// CustomUserDetails.java
public class CustomUserDetails implements UserDetails {
    private final Long memberId;
    private final String username;
    // ... 기타 필드

    public Long getMemberId() {
        return memberId;
    }
}

// CartController.java
@GetMapping("/carts")
public ResponseEntity<List<CartDto>> printAllCarts(
    @AuthenticationPrincipal CustomUserDetails userDetails
) {
    List<CartDto> cartDtos = cartService.printAllCarts(userDetails.getMemberId());
    return ResponseEntity.ok().body(cartDtos);
}
```

### 이슈 2: AddCartRequest에서 memberId 제거 미확인

**위치**: `CartController.java:36`

**심각도**: Critical

**설명**:
코드에서 `req.memberId()` 호출이 제거되었으나, `AddCartRequest` DTO 정의에서 실제로 `memberId` 필드가 제거되었는지 diff에서 확인 불가. 만약 필드가 남아있다면 클라이언트가 여전히 memberId를 전송할 수 있습니다.

**제안**:
`AddCartRequest` record/class에서 `memberId` 필드 완전 제거 확인

```java
// AddCartRequest.java (확인 필요)
public record AddCartRequest(
    // Long memberId,  // 이 필드가 제거되었는지 확인 필요
    Long productId,
    Integer quantity
) {}
```

### 이슈 3: 트랜잭션 경계 최적화

**위치**: `CartService.java`

**심각도**: Minor

**설명**:
새로 추가된 `*ByUsername` 메서드들이 각각 `@Transactional`을 가지고 있으며, 내부에서 또 다른 `@Transactional` 메서드를 호출합니다. 이로 인해 불필요한 트랜잭션 중첩이 발생할 수 있습니다.

**제안**:
트랜잭션 전파 레벨 명시 또는 구조 개선

```java
@Transactional(readOnly = true)
public List<CartDto> printAllCartsByUsername(String username) {
    Member member = memberRepository.findMemberByUsername(username)
        .orElseThrow(() -> new MemberNotFoundException("회원 정보를 찾을 수 없습니다."));
    // printAllCarts는 readOnly 트랜잭션이므로 중첩 안전
    return printAllCarts(member.getId());
}
```

## 📊 종합 평가

| 평가 항목 | 점수 | 코멘트 |
|-----------|------|--------|
| 코드 품질 | ⭐⭐⭐⭐☆ | 깔끔한 리팩토링이나 중복 조회 이슈 존재 |
| 설계/아키텍처 | ⭐⭐⭐⭐⭐ | 계층 분리 명확, 보안 원칙 준수 |
| 보안 | ⭐⭐⭐⭐⭐ | IDOR 취약점 제거, 인증 강제 적용 탁월 |
| 테스트 커버리지 | ⭐⭐☆☆☆ | 새 메서드에 대한 단위 테스트 부재 |
| 문서화 | ⭐⭐⭐☆☆ | PR 설명은 명확하나 API 문서 업데이트 필요 |
| 하위 호환성 | ⭐⭐☆☆☆ | Breaking Change, 프론트엔드 전면 수정 필요 |

**종합 점수**: ⭐⭐⭐⭐☆ (4.0/5.0)

## 🏁 결론

**머지 권장 여부**: ✅ **조건부 승인 권장**

**최종 의견**:

이 PR은 **보안 측면에서 매우 중요하고 필수적인 개선**을 제공합니다. IDOR 취약점을 제거하고 인증 기반 API로 전환한 것은 프로덕션 환경에서 반드시 적용되어야 할 변경입니다.

다만 다음 사항들을 고려해야 합니다:

**승인 전 필수 조치:**
1. 프론트엔드 팀과 Breaking Change 공유 및 마이그레이션 계획 수립
2. API 문서(Swagger/Postman) 업데이트
3. `AddCartRequest`에서 `memberId` 필드 완전 제거 확인

**후속 작업 권장:**
1. 단위 테스트 추가 (CartServiceTest, OrderServiceTest)
2. 성능 최적화: CustomUserDetails에 memberId 포함하여 중복 조회 제거
3. 커스텀 예외 클래스 도입 (`MemberNotFoundException`)
4. 통합 테스트 및 E2E 테스트 수행 (Playwright 활용)

**리그레션 테스트 필요 영역:**
- 장바구니 조회/추가/삭제 (인증 필수 확인)
- 주문 조회 (인증 필수 확인)
- 인증 없는 요청 시 401 응답 확인
- JWT 토큰 만료 시 처리
- CORS 설정 검증

**다음 단계:**
1. Playwright를 사용한 E2E 리그레션 테스트 작성 및 실행
2. 테스트 결과를 바탕으로 최종 승인 결정
3. 프론트엔드 마이그레이션 가이드 문서 작성
