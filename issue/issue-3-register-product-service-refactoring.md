# GitHub Issue #3 분석 결과

## 이슈 요약
- **이슈 번호**: #3
- **제목**: 상품 등록 서비스 리팩토링
- **유형**: refactor
- **라벨**: 없음
- **상태**: OPEN
- **담당자**: 없음
- **분석 일시**: 2026-01-13

## 생성된 브랜치
- **브랜치명**: `refactor/3-register-product-service`
- **기반 브랜치**: main

## 관련 코드 분석

### 영향받는 파일들

| 파일 경로 | 역할 |
|-----------|------|
| `src/main/java/com/example/ddd_start/product/application/service/RegisterProductService.java` | 리팩토링 대상 주요 파일 (3개 메서드) |
| `src/main/java/com/example/ddd_start/product/domain/Product.java` | Product 엔티티, 생성자 및 updateProduct 메서드 |
| `src/main/java/com/example/ddd_start/product/presentation/ProductController.java` | 서비스 호출부 (lines 40, 141, 161) |
| `src/main/java/com/example/ddd_start/store/domain/Store.java` | Store 애그리거트, createProduct() 팩토리 메서드 |
| `src/main/java/com/example/ddd_start/product/application/service/model/NewProductRequest.java` | 상품 등록 요청 DTO |
| `src/main/java/com/example/ddd_start/product/application/service/model/UpdateProductRequest.java` | 상품 수정 요청 DTO |

### 주요 리팩토링 포인트

#### 1. 중복 코드 및 일관성 문제

**현재 상태:**
- `registerNewProduct()`: Store 애그리거트를 통해 상품 생성 (DDD 원칙 준수)
- `registerProductNoStore()`: Product를 직접 생성 (Store 없이)
- `updateProduct()`: Product 엔티티의 `updateProduct()` 메서드 사용, 하지만 `store` 파라미터를 항상 `null`로 전달

**문제점:**
- Store를 통한 생성과 직접 생성이 공존하여 일관성 부족
- `updateProduct()` 메서드가 store를 null로 설정하는 것은 비즈니스 로직상 위험
- ProductController에서는 `registerProductNoStore()`만 사용 중 (line 141)
- CategoryRepository를 주입받지만 사용하지 않음

#### 2. Money 값 객체 사용 불일치
- `registerNewProduct()`: ProductInfo에서 Integer price를 받아 Store가 Money로 변환
- `registerProductNoStore()`, `updateProduct()`: 직접 `new Money(req.getPrice())` 호출
- DDD 원칙상 Money 변환은 도메인 계층에서 처리해야 함

#### 3. 비즈니스 로직 검증 부재
- `registerProductNoStore()`는 Store의 상태 검증 우회 (StoreBlockedException 체크 없음)
- Category 유효성 검증 없음 (CategoryRepository 미사용)
- Price 유효성 검증 없음 (음수, 0원 등)

#### 4. 트랜잭션 관리
- `updateProduct()`에서 `productRepository.save()` 호출은 불필요
- JPA dirty checking으로 자동 업데이트되므로 명시적 save는 중복

### 권장 리팩토링 방향

#### Phase 1: 비즈니스 요구사항 명확화
1. Store 없는 상품 등록이 실제로 필요한지 확인
2. 상품 업데이트 시 Store 변경이 가능해야 하는지 결정
3. Category 유효성 검증 필요 여부 확인

#### Phase 2: 도메인 모델 개선
1. Product 엔티티에 비즈니스 검증 로직 추가
   - Price 유효성 검증
   - CategoryId 필수 여부 검증
2. ProductInfo 값 객체를 불변 객체로 개선
3. Product의 `updateProduct()` 메서드 시그니처 개선
   - Store를 nullable로 받지 말고 명확한 의도 표현

#### Phase 3: 서비스 계층 리팩토링
1. 메서드 명확화
   - `registerNewProduct()` vs `registerProductNoStore()` 중 하나로 통일
   - 또는 Store 유무에 따른 명확한 구분 및 문서화
2. CategoryRepository 사용 또는 제거
   - 사용하지 않으면 의존성 제거
   - 사용한다면 카테고리 존재 여부 검증 추가
3. 불필요한 save() 호출 제거
4. 공통 검증 로직 추출

#### Phase 4: 테스트 추가
- 현재 테스트 파일 없음
- RegisterProductServiceTest 작성 필요
- 각 시나리오별 테스트 케이스:
  - Store를 통한 상품 등록 성공
  - Store가 CLOSED 상태일 때 등록 실패
  - 유효하지 않은 Category로 등록 시도
  - 상품 업데이트 성공
  - 존재하지 않는 상품 업데이트 시도

### 의존성 및 영향도

**직접 영향:**
- ProductController (POST /products, PUT /products)
- Store 애그리거트의 createProduct() 메서드
- Product 엔티티의 생성자 및 updateProduct() 메서드

**간접 영향:**
- CategoryRepository (사용 여부 결정 필요)
- StoreRepository (Store 검증 로직 추가 시)
- 프론트엔드 상품 등록/수정 API 호출부

### 권장 구현 순서

1. 현재 API 사용 현황 파악
2. 테스트 케이스 작성 (기존 동작 보호)
3. 도메인 모델 개선
4. 서비스 메서드 통합 또는 명확화
5. CategoryRepository 활용 또는 제거
6. 불필요한 코드 제거
7. 통합 테스트 실행
8. 문서화

### 테스트 고려사항

**단위 테스트 (Unit Test):**
- RegisterProductService의 각 메서드별 테스트
- Mock을 사용한 Repository 격리
- 비즈니스 로직 검증

**통합 테스트 (Integration Test):**
- @SpringBootTest를 사용한 전체 플로우 테스트
- 실제 DB 연동 테스트
- ProductController를 통한 E2E 시나리오 테스트

**테스트 데이터:**
- 정상 케이스: 유효한 상품 정보
- 예외 케이스: null 값, 음수 가격, 존재하지 않는 카테고리
- 경계 케이스: 최소/최대 가격, 긴 설명 텍스트
