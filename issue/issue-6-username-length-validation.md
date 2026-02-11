# 이슈 분석 보고서

## 이슈 요약
- **이슈 번호**: #6
- **제목**: 회원 가입 시, 아이디 10자리 초과 방지 로직 추가
- **유형**: Feature (기능 개선 / 검증 로직 추가)
- **라벨**: 없음
- **상태**: OPEN
- **분석 일시**: 2026-01-19

## 생성된 브랜치
- **브랜치명**: `feature/6-username-length-validation`
- **기반 브랜치**: `main`
- **브랜치 생성 완료**: ✅

## 이슈 상세 내용
회원 가입 시, 아이디(username) 필드가 10자리를 초과하지 않도록 검증 로직을 추가해야 합니다.

## 관련 코드 분석

### 현재 상태

#### 1. Domain Layer - Member 엔티티
**파일**: `/src/main/java/com/example/ddd_start/member/domain/Member.java`

```java
@Column(columnDefinition = "varchar(15)")
private String username;
```

**분석**:
- 데이터베이스 컬럼 정의는 `varchar(15)`로 15자까지 허용
- 이슈 요구사항은 10자 제한이므로, DB 제약보다 애플리케이션 레벨에서 더 엄격한 검증 필요
- 현재 엔티티 레벨에서는 username 길이 검증 로직 없음

#### 2. Application Layer - JoinMemberService
**파일**: `/src/main/java/com/example/ddd_start/member/applicaiton/JoinMemberService.java`

```java
@Transactional
public joinResponse joinMember(joinCommand req)
    throws DuplicateEmailException, DuplicateUsernameException {
  // 값의 형식 검사
  checkEmpty(req.getEmail(), "email");
  checkEmpty(req.getPassword(), "password");
  checkEmpty(req.getUsername(), "username");
  checkEmpty(req.getRole(), "role");

  // 로직 검사
  checkDuplicatedEmail(req.getEmail());
  checkDuplicatedUsername(req.getUsername());

  // ... Member 생성 로직
}

private void checkEmpty(String value, String propertyName) {
  if (value == null || value.isEmpty()) {
    throw new NullPointerException(propertyName);
  }
}
```

**분석**:
- 현재 검증: null/empty 체크, 중복 체크만 수행
- **username 길이 검증 로직 없음** - 이슈 해결을 위해 추가 필요
- 검증 패턴: private 메서드로 검증 로직 분리하는 구조 사용 중

#### 3. Presentation Layer - Request DTO
**파일**: `/src/main/java/com/example/ddd_start/member/presentation/model/JoinMemberRequest.java`

```java
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JoinMemberRequest {
  String email;
  String password;
  String username;
  String name;
  AddressCommand addressReq;
}
```

**분석**:
- Bean Validation 어노테이션 미사용
- 필드 레벨 검증 없음
- Controller에서도 별도 검증 없이 Service로 위임

#### 4. Application Command
**파일**: `/src/main/java/com/example/ddd_start/member/applicaiton/model/joinCommand.java`

```java
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class joinCommand {
  String email;
  String password;
  String username;
  String name;
  AddressCommand addressReq;
  String role;
}
```

**분석**:
- Command 객체에도 검증 로직 없음

### 영향받는 파일들

| 순번 | 파일 경로 | 변경 필요성 | 상세 |
|------|----------|-------------|------|
| 1 | `/src/main/java/com/example/ddd_start/member/applicaiton/JoinMemberService.java` | **필수** | username 길이 검증 메서드 추가 |
| 2 | `/src/main/java/com/example/ddd_start/common/domain/exception/` | **필수** | 새로운 예외 클래스 추가 (UsernameValidationException 등) |
| 3 | `/src/main/java/com/example/ddd_start/member/presentation/MemberController.java` | **선택** | 예외 핸들링 추가 (catch 블록) |
| 4 | `/src/test/java/.../member/application/` | **권장** | JoinMemberService 테스트 케이스 추가 |

## 주요 변경 포인트

### 1. 예외 클래스 생성 (필수)
**위치**: `/src/main/java/com/example/ddd_start/common/domain/exception/InvalidUsernameLengthException.java`

```java
package com.example.ddd_start.common.domain.exception;

public class InvalidUsernameLengthException extends RuntimeException {
    public InvalidUsernameLengthException() {
        super("Username must be 10 characters or less");
    }

    public InvalidUsernameLengthException(String message) {
        super(message);
    }
}
```

**근거**:
- 기존 예외들(`DuplicateEmailException`, `DuplicateUsernameException`)과 동일한 패턴
- `common.domain.exception` 패키지에 도메인 검증 예외 집중 관리

### 2. JoinMemberService 검증 로직 추가 (필수)
**위치**: `/src/main/java/com/example/ddd_start/member/applicaiton/JoinMemberService.java`

**추가할 메서드**:
```java
private void checkUsernameLength(String username) throws InvalidUsernameLengthException {
    if (username != null && username.length() > 10) {
        throw new InvalidUsernameLengthException();
    }
}
```

**호출 위치** (line 29-37 사이):
```java
@Transactional
public joinResponse joinMember(joinCommand req)
    throws DuplicateEmailException, DuplicateUsernameException, InvalidUsernameLengthException {
  // 값의 형식 검사
  checkEmpty(req.getEmail(), "email");
  checkEmpty(req.getPassword(), "password");
  checkEmpty(req.getUsername(), "username");
  checkEmpty(req.getRole(), "role");

  checkUsernameLength(req.getUsername());  // ← 여기 추가

  // 로직 검사
  checkDuplicatedEmail(req.getEmail());
  checkDuplicatedUsername(req.getUsername());
  // ...
}
```

**검증 순서 논리**:
1. null/empty 체크 (`checkEmpty`) - 기본 형식 검증
2. **길이 검증** (`checkUsernameLength`) - 값 형식 검증
3. 중복 체크 (`checkDuplicatedUsername`) - 비즈니스 로직 검증 (DB 조회 필요)

### 3. MemberController 예외 핸들링 (필수)
**위치**: `/src/main/java/com/example/ddd_start/member/presentation/MemberController.java`

**수정 위치**: `join()` 메서드 (line 47-70)

```java
@PostMapping("/members/join")
public ResponseEntity join(@RequestBody JoinMemberRequest req, Errors errors) {
  // ...
  try {
    joinResponse joinResponse = joinMemberService.joinMember(
        new joinCommand(email, password, username, name, addressReq, "USER"));

    return new ResponseEntity<MemberResponse>(
        new MemberResponse(
            joinResponse.getMemberId(), joinResponse.getName(), "회원가입을 축하드립니다."),
        HttpStatus.ACCEPTED);
  } catch (DuplicateEmailException e) {
    errors.rejectValue(e.getMessage(), "duplicate");
    return new ResponseEntity("이메일이 중복됩니다.", HttpStatus.BAD_REQUEST);
  } catch (DuplicateUsernameException e) {
    errors.rejectValue(e.getMessage(), "duplicate");
    return new ResponseEntity("아이디가 중복됩니다.", HttpStatus.BAD_REQUEST);
  } catch (InvalidUsernameLengthException e) {  // ← 여기 추가
    return new ResponseEntity("아이디는 10자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST);
  }
}
```

**동일하게 수정 필요한 메서드**: `joinAdmin()` (line 72-95)

## 의존성 및 영향도

### 영향받는 컴포넌트

#### 직접 영향
- `JoinMemberService`: 검증 로직 추가
- `MemberController`: `/members/join`, `/members/join/admin` 엔드포인트의 예외 핸들링

#### 간접 영향
- **없음**: 해당 변경사항은 회원가입 프로세스에만 국한되며, 다른 도메인(Order, Product, Coupon 등)에는 영향 없음
- `UpdateMemberService`: username 변경 기능이 있다면 동일한 검증 로직 적용 검토 필요

### 데이터베이스 영향
- **스키마 변경 없음**: DB는 이미 `varchar(15)`로 여유 있게 설정되어 있음
- 기존 데이터 마이그레이션 불필요

### 테스트 영향
- 기존 테스트: 현재 회원가입 관련 테스트 파일 없음 (검색 결과 확인)
- 새로운 테스트 작성 권장

## 권장 구현 순서

### Step 1: 예외 클래스 생성
1. `InvalidUsernameLengthException.java` 파일 생성
2. 기존 예외 클래스(`DuplicateEmailException` 등) 참고하여 작성
3. RuntimeException 상속으로 일관성 유지

### Step 2: Service Layer 검증 로직 추가
1. `JoinMemberService`에 `checkUsernameLength()` 메서드 추가
2. `joinMember()` 메서드의 검증 단계에 호출 추가
3. 메서드 시그니처에 throws 절 추가

### Step 3: Controller Layer 예외 핸들링
1. `MemberController.join()` 메서드에 catch 블록 추가
2. `MemberController.joinAdmin()` 메서드에 동일 적용
3. 한글 에러 메시지 작성

### Step 4: 테스트 작성 (권장)
1. `JoinMemberServiceTest.java` 생성
2. 테스트 케이스:
   - ✅ 정상 케이스: username 10자 이하
   - ✅ 경계값 테스트: 정확히 10자
   - ❌ 실패 케이스: 11자 이상
   - ❌ 실패 케이스: null (기존 검증)
   - ❌ 실패 케이스: empty (기존 검증)

### Step 5: 통합 테스트
1. Postman 또는 curl로 API 테스트
2. 에러 응답 메시지 확인
3. DB 저장 여부 확인

## 테스트 고려사항

### 단위 테스트 (Unit Test)

#### JoinMemberServiceTest
```java
@SpringBootTest
class JoinMemberServiceTest {

    @Autowired
    private JoinMemberService joinMemberService;

    @Test
    @DisplayName("username이 10자 초과 시 예외 발생")
    void testUsernameTooLong() {
        // given
        joinCommand command = new joinCommand(
            "test@example.com",
            "password123",
            "verylongusername123",  // 19자
            "테스트유저",
            new AddressCommand("주소", "상세주소", "12345"),
            "USER"
        );

        // when & then
        assertThrows(InvalidUsernameLengthException.class,
            () -> joinMemberService.joinMember(command));
    }

    @Test
    @DisplayName("username이 정확히 10자일 때 성공")
    void testUsernameExactly10Chars() {
        // given
        joinCommand command = new joinCommand(
            "test@example.com",
            "password123",
            "username10",  // 정확히 10자
            "테스트유저",
            new AddressCommand("주소", "상세주소", "12345"),
            "USER"
        );

        // when & then
        assertDoesNotThrow(() -> joinMemberService.joinMember(command));
    }

    @Test
    @DisplayName("username이 10자 미만일 때 성공")
    void testUsernameLessThan10Chars() {
        // given
        joinCommand command = new joinCommand(
            "test@example.com",
            "password123",
            "user",  // 4자
            "테스트유저",
            new AddressCommand("주소", "상세주소", "12345"),
            "USER"
        );

        // when & then
        assertDoesNotThrow(() -> joinMemberService.joinMember(command));
    }
}
```

### 통합 테스트 (Integration Test)

#### API 테스트 시나리오
```bash
# 1. 실패 케이스 - 11자 username
curl -X POST http://localhost:8080/members/join \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "verylongusername",
    "name": "테스트",
    "addressReq": {
      "address": "서울시",
      "detailedAddress": "강남구",
      "zipCode": "12345"
    }
  }'

# 예상 응답: 400 Bad Request
# "아이디는 10자 이하로 입력해주세요."

# 2. 성공 케이스 - 10자 username
curl -X POST http://localhost:8080/members/join \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "username10",
    "name": "테스트",
    "addressReq": {
      "address": "서울시",
      "detailedAddress": "강남구",
      "zipCode": "12345"
    }
  }'

# 예상 응답: 202 Accepted
# { "memberId": 1, "name": "username10", "message": "회원가입을 축하드립니다." }
```

### 엣지 케이스 검증
- **한글 username**: "한글이름열자이상" (UTF-8 처리 확인)
- **특수문자 username**: "user@#$%^&" (특수문자 허용 여부)
- **공백 포함**: "user name1" (공백 처리)
- **null vs empty**: null과 "" 구분 처리

## 추가 개선 제안

### 1. Bean Validation 적용 (선택사항)
현재 프로젝트는 Bean Validation을 사용하지 않고 있으나, 장기적으로는 적용 고려:

```java
// JoinMemberRequest.java
public class JoinMemberRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotBlank(message = "아이디는 필수입니다")
    @Size(max = 10, message = "아이디는 10자 이하로 입력해주세요")
    private String username;

    // ...
}
```

### 2. UpdateMemberService 동일 검증 적용
현재 `UpdateMemberService`의 `changeUsername()` 메서드도 동일한 검증 필요:

```java
// Member.java의 changeUsername 메서드
public void changeUsername(String username) {
    if (username != null && username.length() > 10) {
        throw new InvalidUsernameLengthException();
    }
    this.username = username;
}
```

### 3. 데이터베이스 제약 조건 일치
장기적으로 DB 컬럼 정의도 10자로 변경 고려:
```sql
ALTER TABLE member MODIFY COLUMN username VARCHAR(10);
```

## 구현 체크리스트

- [ ] `InvalidUsernameLengthException.java` 생성
- [ ] `JoinMemberService.checkUsernameLength()` 메서드 추가
- [ ] `JoinMemberService.joinMember()` 검증 로직 통합
- [ ] `MemberController.join()` 예외 핸들링 추가
- [ ] `MemberController.joinAdmin()` 예외 핸들링 추가
- [ ] 단위 테스트 작성 (선택)
- [ ] API 통합 테스트 (필수)
- [ ] 에러 메시지 한글 확인
- [ ] 기존 회원가입 기능 정상 동작 확인

## 참고사항

### DDD 관점에서의 검증 위치
- **현재 접근**: Application Layer (Service)에서 검증
- **대안**: Domain Layer (Member 엔티티)에서 검증
  - Member 생성자에서 검증 수행 가능
  - 불변성(Invariant) 보장 측면에서 유리
  - 하지만 현재 프로젝트는 Service 레벨 검증 패턴 사용 중

### 기존 프로젝트 패턴 준수
이슈 해결 방식은 기존 코드베이스의 다음 패턴을 따릅니다:
- ✅ Service Layer에서 private 메서드로 검증
- ✅ Custom Exception 클래스 사용
- ✅ Controller에서 try-catch로 예외 핸들링
- ✅ HTTP 400 Bad Request 응답 사용

---

**작성자**: Claude Code Issue Analyst
**분석 완료**: 2026-01-19
**브랜치**: feature/6-username-length-validation
